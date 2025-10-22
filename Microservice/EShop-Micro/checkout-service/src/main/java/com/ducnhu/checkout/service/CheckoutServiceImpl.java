package com.ducnhu.checkout.service;

import com.ducnhu.checkout.dto.*;
import com.ducnhu.checkout.saga.OrderSagaState;
import com.ducnhu.checkout.saga.OrderSagaStore;
import com.ducnhu.common.events.carts.CartClearCommand;
import com.ducnhu.common.events.carts.CartGetRequest;
import com.ducnhu.common.events.carts.CartGetResponse;
import com.ducnhu.common.events.carts.CartLine;
import com.ducnhu.common.events.catalog.ProductSnapshot;
import com.ducnhu.common.events.catalog.ProductSnapshotRequest;
import com.ducnhu.common.events.catalog.ProductSnapshotResponse;
import com.ducnhu.common.events.customer.*;
import com.ducnhu.common.events.orders.OrderCancelledEvent;
import com.ducnhu.common.events.orders.OrderPlacedEvent;
import com.ducnhu.common.events.orders.OrderPlacedEventV2;
import com.ducnhu.common.events.orders.OrderPlacedItem;
import com.ducnhu.common.events.settings.EmailSettingsRequest;
import com.ducnhu.common.events.settings.EmailSettingsResponse;
import com.ducnhu.common.events.shipping.ShippingRateRequest;
import com.ducnhu.common.events.shipping.ShippingRateResponse;
import com.ducnhu.common.exception.CartItemNotFoundException;
import com.ducnhu.common.kafka.RequestReplyClient;
import com.ducnhu.common.kafka.Topics;
import com.ducnhu.common.mail.CommonMailService;
import com.ducnhu.common.mail.MailUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CheckoutServiceImpl implements CheckoutService {
    private final RequestReplyClient requestReplyClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final CommonMailService mailService;
    private static final int DIM_DIVISOR = 139;
    private final OrderSagaStore store;
    private final AuthClient auth;

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
                Topics.CUST_ADDR_REQ, Topics.CUST_ADDR_RESP, AddressQueryResponse.class,
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
        //CheckoutSummaryDTO sum = summarize(customerId, request.addressId());
        MeResponse me = auth.me();
        // tạo mã đơn tạm (local)
        String orderNumber = "OD" + LocalDateTime.now().format(DateTimeFormatter.BASIC_ISO_DATE)
                + "-" + String.format("%06d", (int)(Instant.now().toEpochMilli() % 1_000_000));

        // dựng event
        List<OrderPlacedItem> items = new ArrayList<>();
        // cần lại cart + prod để ghi shippingCost từng item:
        // (tối ưu: reuse summarize data - ở đây tính lại tương tự)
        // lấy cart
        CartGetResponse cart = requestReplyClient.request(
                Topics.CART_GET_REQ, Topics.CART_GET_RESP, CartGetResponse.class,
                corr -> new CartGetRequest(corr, Topics.CART_GET_RESP, customerId), Duration.ofSeconds(3)
        );
        if (cart.items() == null || cart.items().isEmpty()) {
            throw new RuntimeException("Cart is empty");
        }
        // địa chỉ
        AddressQueryResponse addr = requestReplyClient.request(
                Topics.CUST_ADDR_REQ, Topics.CUST_ADDR_RESP, AddressQueryResponse.class,
                corr -> new AddressQueryRequest(corr, Topics.CUST_ADDR_RESP, customerId, request.addressId()), Duration.ofSeconds(3)
        );
        AddressDTO a = addr.address();
        if (a == null) throw new RuntimeException("Address not found");

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
        Map<Integer, ProductSnapshot> map = prods.products().stream().collect(Collectors.toMap(ProductSnapshot::id, x -> x));

        float productTotalF = 0f, shippingCostF = 0f;

        for (CartLine line : cart.items()) {
            ProductSnapshot p = map.get(line.productId());

            float unit = unitPrice(p.price(), p.discountPrice());
            float subtotal = unit * line.quantity();

            float length = nz(p.length()), width = nz(p.width()), height = nz(p.height()), weight = nz(p.weight());
            float dimWeight = (length * width * height) / DIM_DIVISOR;
            float finalWeight = Math.max(dimWeight, weight);
            float itemShip = finalWeight * line.quantity() * (rate.rate() == null ? 0f : rate.rate());

            productTotalF += subtotal;
            shippingCostF += itemShip;

            // lưu chi tiết (có thể làm tròn hiển thị cho đẹp)
            items.add(new OrderPlacedItem(
                    line.productId(), p.name(), p.alias(), p.mainImagePath(),
                    round2(unit), line.quantity(), round2(subtotal), round2(itemShip)
            ));
        }
        // chốt phí ship theo codSupported
        float shippingApplyF = Boolean.TRUE.equals(rate.codSupported()) ? shippingCostF : 0f;
        float paymentF = productTotalF + shippingApplyF;

        float productTotal = round2(productTotalF);
        float shippingCost = round2(shippingApplyF);
        float payment      = round2(paymentF);

        // saga
        OrderSagaState state = new OrderSagaState();
        state.setOrderNumber(orderNumber);
        state.setCustomerId(customerId);
        state.setStatus(OrderSagaState.Status.NEW);
        state.setUpdatedAt(new Date());
        store.save(state);

//        OrderPlacedEvent evt = new OrderPlacedEvent(
//                UUID.randomUUID().toString(), orderNumber,
//                customerId, customerEmail,
//                a.id(), addressLine(a),
//                productTotal, (Boolean.TRUE.equals(rate.codSupported()) ? shippingCost : 0f), payment,
//                "COD", new Date(), items
//        );

        CustomerSnapshot customerSnapshot = new CustomerSnapshot(
                customerId,                           // id
                customerEmail,                        // email
                me != null ? me.firstName() : null,   // nếu bạn có MeResponse; nếu chưa, để null
                me != null ? me.lastName() : null,
                me != null ? me.phoneNumber() : null
        );

        AddressSnapshot adr = new AddressSnapshot(
                a.firstName(), a.lastName(), a.phoneNumber(),
                a.addressLine1(), a.addressLine2(),
                a.city(), a.state(), a.postalCode(),
                a.countryName()
        );


        OrderPlacedEventV2 evt2 = new OrderPlacedEventV2(
                UUID.randomUUID().toString(),
                orderNumber,
                new Date(),
                customerSnapshot,
                adr,
                items,                    // reuse danh sách OrderPlacedItem đang build
                productTotal,
                shippingCost,
                payment,
                "COD"
        );

        kafkaTemplate.executeInTransaction(kt -> {
            kt.send(Topics.ORDER_EVENTS, orderNumber, evt2);
            kt.send(Topics.CART_CLEAR_CMD, String.valueOf(customerId), new CartClearCommand(customerId));
            return true;
        });

        state.setStatus(OrderSagaState.Status.PUBLISHED);
        state.setUpdatedAt(new Date());
        store.save(state);

        try {
            EmailSettingsResponse mailCfg = requestReplyClient.request(
                    Topics.SETTINGS_EMAIL_REQ, Topics.SETTINGS_EMAIL_RESP, EmailSettingsResponse.class,
                    corr -> new EmailSettingsRequest(corr, Topics.SETTINGS_EMAIL_RESP), Duration.ofSeconds(3)
            );
            JavaMailSender sender = MailUtil.buildSender(
                    mailCfg.host(), mailCfg.port(), mailCfg.username(), mailCfg.password(),
                    mailCfg.smtpAuth(), mailCfg.smtpSecured()
            );
            String subject = mailCfg.orderConfirmSubject().replace("[[orderId]]", orderNumber);
            String body = mailCfg.orderConfirmContent()
                    .replace("[[name]]", customerEmail)
                    .replace("[[orderId]]", orderNumber)
                    .replace("[[time]]", LocalDateTime.now().toString());
            mailService.sendHtml(sender, mailCfg.mailFrom(), mailCfg.senderName(), customerEmail, subject, body);
        } catch (Exception e) {

        }

        state.setStatus(OrderSagaState.Status.COMPLETED);
        state.setUpdatedAt(new Date());
        store.save(state);

        return new PlaceOrderResponse(true, orderNumber, productTotal, (Boolean.TRUE.equals(rate.codSupported()) ? shippingCost : 0f), payment);
    }

    @Override
    public void compensateCancel(String orderNumber, Integer customerId, String reason) {
        kafkaTemplate.executeInTransaction(kt -> {
            kt.send(Topics.ORDER_CANCELLED_EVENTS, orderNumber,
                    new OrderCancelledEvent(UUID.randomUUID().toString(), orderNumber, customerId, reason, new Date()));
            return true;
        });
        OrderSagaState s = store.get(orderNumber);
        if (s != null) {
            s.setStatus(OrderSagaState.Status.CANCELLED);
            s.setNote(reason);
            s.setUpdatedAt(new Date());
            store.save(s);
        }
    }

    private float unitPrice(Float price, Float discount) {
        return (discount != null && discount > 0) ? discount : (price == null ? 0f : price);
    }

    private float nz(Float v) {
        return v == null ? 0f : v;
    }

    private float round2(float v) {
        return Math.round(v * 100f) / 100f;
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

}
