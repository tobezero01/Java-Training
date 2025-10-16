package com.ducnhu.checkout.service;

import com.ducnhu.checkout.dto.CheckoutItemDTO;
import com.ducnhu.checkout.dto.CheckoutSummaryDTO;
import com.ducnhu.checkout.dto.PlaceOrderRequest;
import com.ducnhu.checkout.dto.PlaceOrderResponse;
import com.ducnhu.common.events.carts.CartClearCommand;
import com.ducnhu.common.events.carts.CartGetRequest;
import com.ducnhu.common.events.carts.CartGetResponse;
import com.ducnhu.common.events.carts.CartLine;
import com.ducnhu.common.events.catalog.ProductSnapshot;
import com.ducnhu.common.events.catalog.ProductSnapshotRequest;
import com.ducnhu.common.events.catalog.ProductSnapshotResponse;
import com.ducnhu.common.events.customer.AddressDTO;
import com.ducnhu.common.events.customer.AddressQueryRequest;
import com.ducnhu.common.events.customer.AddressQueryResponse;
import com.ducnhu.common.events.orders.OrderPlacedEvent;
import com.ducnhu.common.events.orders.OrderPlacedItem;
import com.ducnhu.common.events.shipping.ShippingRateRequest;
import com.ducnhu.common.events.shipping.ShippingRateResponse;
import com.ducnhu.common.exception.CartItemNotFoundException;
import com.ducnhu.common.kafka.RequestReplyClient;
import com.ducnhu.common.kafka.Topics;
import com.ducnhu.common.mail.CommonMailService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.text.DecimalFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static jdk.internal.icu.impl.Utility.escape;

@Service
@RequiredArgsConstructor
public class CheckoutServiceImpl implements CheckoutService {
    private final RequestReplyClient requestReplyClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final CommonMailService mailService;
    private static final int DIM_DIVISOR = 139;

    @Override
    public CheckoutSummaryDTO summarize(Integer customerId, Integer addressId) {
        CartGetResponse cartGetResponse = requestReplyClient.request(
                Topics.CART_GET_REQ, Topics.CART_GET_RESP, CartGetResponse.class,
                corr -> new CartGetRequest(corr, Topics.CART_GET_RESP, customerId),
                Duration.ofSeconds(3)
        );
        if (cartGetResponse.items() == null || cartGetResponse.items().isEmpty()) {
            throw new CartItemNotFoundException("Cart is empty");
        }

        // address
        AddressQueryResponse addressQueryResponse = requestReplyClient.request(
                Topics.CUST_ADDR_REQ, Topics.CART_GET_RESP, AddressQueryResponse.class,
                corr -> new AddressQueryRequest(corr, Topics.CUST_ADDR_RESP, customerId, addressId),
                Duration.ofSeconds(3)
        );

        AddressDTO address = addressQueryResponse.address();
        if (address == null) throw new RuntimeException("Address not found");

        // 3) shipping rate
        ShippingRateResponse rate = requestReplyClient.request(
                Topics.SHIP_RATE_REQ, Topics.SHIP_RATE_RESP, ShippingRateResponse.class,
                corr -> new ShippingRateRequest(corr, Topics.SHIP_RATE_RESP, address.countryId(),
                        firstNonBlank(address.state(), address.city())),
                Duration.ofSeconds(3)
        );

        // 4) product snapshot (đảm bảo tính toán chuẩn xác)
        List<Integer> ids = cartGetResponse.items().stream().map(CartLine::productId).toList();
        ProductSnapshotResponse prods = requestReplyClient.request(
                Topics.CATALOG_PROD_SNAPSHOT_REQ, Topics.CATALOG_PROD_SNAPSHOT_RESP, ProductSnapshotResponse.class,
                corr -> new ProductSnapshotRequest(corr, Topics.CATALOG_PROD_SNAPSHOT_RESP, ids),
                Duration.ofSeconds(3)
        );
        Map<Integer, ProductSnapshot> map = prods.products().stream().collect(Collectors.toMap(ProductSnapshot::id, x -> x));

        // 5) compute totals
        float productTotal = 0f;
        float shippingCost = 0f;
        List<CheckoutItemDTO> items = new ArrayList<>();
        for (CartLine l : cartGetResponse.items()) {
            ProductSnapshot p = map.get(l.productId());
            float unit = unitPrice(p.price(), p.discountPrice());
            float subtotal = unit * l.quantity();
            productTotal += subtotal;

            float length = nz(p.length()), width = nz(p.width()), height = nz(p.height()), weight = nz(p.weight());
            float dimWeight = (length * width * height) / DIM_DIVISOR;
            float finalWeight = Math.max(dimWeight, weight);
            float itemShip = finalWeight * l.quantity() * (rate.rate() == null ? 0f : rate.rate());
            shippingCost += itemShip;

            items.add(new CheckoutItemDTO(l.productId(), p.name(), p.alias(), p.mainImagePath(), unit, l.quantity(), subtotal));
        }
        float payment = productTotal + (Boolean.TRUE.equals(rate.codSupported()) ? shippingCost : 0f);

        return new CheckoutSummaryDTO(
                items, productTotal,
                (Boolean.TRUE.equals(rate.codSupported()) ? shippingCost : 0f),
                payment,
                rate.days() != null && rate.days() > 0, // shippingSupported
                address.id(), addressLine(address)
        );
    }

    @Override
    public PlaceOrderResponse placeOrderCod(Integer customerId, String customerEmail, PlaceOrderRequest request) {
        CheckoutSummaryDTO sum = summarize(customerId, request.addressId());

        // tạo mã đơn tạm (local)
        String orderNumber = "OD" + LocalDateTime.now().format(DateTimeFormatter.BASIC_ISO_DATE)
                + "-" + String.format("%06d", new Random().nextInt(1_000_000)); // giữ format cũ :contentReference[oaicite:7]{index=7}

        // dựng event
        List<OrderPlacedItem> items = new ArrayList<>();
// cần lại cart + prod để ghi shippingCost từng item:
        // (tối ưu: reuse summarize data - ở đây tính lại tương tự)
        // lấy cart
        CartGetResponse cart = requestReplyClient.request(
                Topics.CART_GET_REQ, Topics.CART_GET_RESP, CartGetResponse.class,
                corr -> new CartGetRequest(corr, Topics.CART_GET_RESP, customerId), Duration.ofSeconds(3)
        );
        // địa chỉ
        AddressQueryResponse addr = requestReplyClient.request(
                Topics.CUST_ADDR_REQ, Topics.CUST_ADDR_RESP, AddressQueryResponse.class,
                corr -> new AddressQueryRequest(corr, Topics.CUST_ADDR_RESP, customerId, request.addressId()), Duration.ofSeconds(3)
        );
        AddressDTO a = addr.address();
        // rate
        ShippingRateResponse rate = requestReplyClient.request(
                Topics.SHIP_RATE_REQ, Topics.SHIP_RATE_RESP, ShippingRateResponse.class,
                corr -> new ShippingRateRequest(corr, Topics.SHIP_RATE_RESP, a.countryId(), firstNonBlank(a.state(), a.city())),
                Duration.ofSeconds(3)
        );
        // snapshot
        List<Integer> ids = cart.items().stream().map(CartLine::productId).toList();
        ProductSnapshotResponse prods = requestReplyClient.request(
                Topics.CATALOG_PROD_SNAPSHOT_REQ, Topics.CATALOG_PROD_SNAPSHOT_RESP, ProductSnapshotResponse.class,
                corr -> new ProductSnapshotRequest(corr, Topics.CATALOG_PROD_SNAPSHOT_RESP, ids), Duration.ofSeconds(3)
        );
        Map<Integer, ProductSnapshot> map = prods.products().stream().collect(java.util.stream.Collectors.toMap(ProductSnapshot::id, x -> x));

        float productTotal = 0f, shippingCost = 0f;
        for (CartLine l : cart.items()) {
            ProductSnapshot p = map.get(l.productId());
            float unit = unitPrice(p.price(), p.discountPrice());
            float subtotal = unit * l.quantity();
            productTotal += subtotal;
            float length = nz(p.length()), width = nz(p.width()), height = nz(p.height()), weight = nz(p.weight());
            float dimWeight = (length * width * height) / DIM_DIVISOR;
            float finalWeight = Math.max(dimWeight, weight);
            float itemShip = finalWeight * l.quantity() * (rate.rate() == null ? 0f : rate.rate());
            shippingCost += itemShip;

            items.add(new OrderPlacedItem(l.productId(), p.name(), p.alias(), p.mainImagePath(),
                    unit, l.quantity(), subtotal, itemShip));
        }
        Float payment = productTotal + (Boolean.TRUE.equals(rate.codSupported()) ? shippingCost : 0f);
        OrderPlacedEvent evt = new OrderPlacedEvent(
                UUID.randomUUID().toString(), orderNumber,
                customerId, customerEmail,
                a.id(), addressLine(a),
                productTotal, (Boolean.TRUE.equals(rate.codSupported()) ? shippingCost : 0f), payment,
                "COD", new Date(), items
        );
        kafkaTemplate.send(Topics.ORDER_EVENTS, evt);
        kafkaTemplate.send(Topics.CART_CLEAR_CMD, new CartClearCommand(customerId));

        try {
            String html = buildEmailHtml(evt);
            mailService.sendHtml("${mail.from}", "${mail.fromName}", customerEmail, "[EShop] Xác nhận đơn " + orderNumber, html);
        } catch (Exception e) { /* log và bỏ qua */ }

        return new PlaceOrderResponse(true, orderNumber, productTotal, (Boolean.TRUE.equals(rate.codSupported()) ? shippingCost : 0f), payment);
    }

    private float unitPrice(Float price, Float discount) {
        return (discount != null && discount > 0) ? discount : (price == null ? 0f : price);
    }

    private float nz(Float v) {
        return v == null ? 0f : v;
    }

    private String firstNonBlank(String a, String b) {
        return (a != null && !a.isBlank()) ? a : (b == null ? "" : b);
    }

    private String addressLine(AddressDTO a) {
        return String.join(", ",
                nullToEmpty(a.addressLine1()),
                nullToEmpty(a.addressLine2()),
                nullToEmpty(a.city()),
                nullToEmpty(a.state()),
                nullToEmpty(a.postalCode()),
                nullToEmpty(a.countryName())
        ).replaceAll(",\\s*,", ", ").replaceAll("^,\\s*|,\\s*$", "");
    }

    private String nullToEmpty(String s) {
        return (s == null) ? "" : s.trim();
    }

    private String buildEmailHtml(OrderPlacedEvent e) {
        StringBuilder sb = new StringBuilder();
        DecimalFormat df = new DecimalFormat("#,##0.00");
        sb.append("<h3>Đơn hàng ").append(e.orderNumber()).append("</h3>");
        sb.append("<p>Địa chỉ giao: ").append(e.addressLine()).append("</p>");
        sb.append("<table border='1' cellpadding='6' cellspacing='0'>");
        sb.append("<tr><th>Sản phẩm</th><th>SL</th><th>Đơn giá</th><th>Tạm tính</th></tr>");
        for (OrderPlacedItem it : e.items()) {
            sb.append("<tr>")
                    .append("<td>").append(escape(it.name())).append("</td>")
                    .append("<td align='right'>").append(it.quantity()).append("</td>")
                    .append("<td align='right'>").append(df.format(it.unitPrice())).append("</td>")
                    .append("<td align='right'>").append(df.format(it.subtotal())).append("</td>")
                    .append("</tr>");
        }
        sb.append("</table>");
        sb.append("<p>Tổng tiền hàng: ").append(df.format(e.productTotal())).append("</p>");
        sb.append("<p>Phí vận chuyển: ").append(df.format(e.shippingCost())).append("</p>");
        sb.append("<p><b>THANH TOÁN: ").append(df.format(e.paymentTotal())).append("</b></p>");
        return sb.toString();
    }
}
