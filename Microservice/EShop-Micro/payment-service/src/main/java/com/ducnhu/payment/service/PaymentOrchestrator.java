package com.ducnhu.payment.service;

import com.ducnhu.common.events.carts.CartGetRequest;
import com.ducnhu.common.events.carts.CartGetResponse;
import com.ducnhu.common.events.carts.CartLine;
import com.ducnhu.common.events.catalog.ProductSnapshot;
import com.ducnhu.common.events.catalog.ProductSnapshotRequest;
import com.ducnhu.common.events.catalog.ProductSnapshotResponse;
import com.ducnhu.common.events.customer.AddressDTO;
import com.ducnhu.common.events.customer.AddressQueryRequest;
import com.ducnhu.common.events.customer.AddressQueryResponse;
import com.ducnhu.common.events.shipping.ShippingRateRequest;
import com.ducnhu.common.events.shipping.ShippingRateResponse;
import com.ducnhu.common.kafka.RequestReplyClient;
import com.ducnhu.common.kafka.Topics;
import com.ducnhu.payment.dto.Summary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PaymentOrchestrator {
    private static final int DIM_DIVISOR = 139;

    private final RequestReplyClient rr;

    public Summary summarize(Integer customerId, Integer addressId) {
        CartGetResponse cart = rr.request(
                Topics.CART_GET_REQ, Topics.CART_GET_RESP, CartGetResponse.class,
                corr -> new CartGetRequest(corr, Topics.CART_GET_RESP, customerId),
                Duration.ofSeconds(3)
        );
        if (cart.items() == null || cart.items().isEmpty()) throw new IllegalStateException("Cart is empty");

        AddressQueryResponse addr = rr.request(
                Topics.CUST_ADDR_REQ, Topics.CUST_ADDR_RESP, AddressQueryResponse.class,
                corr -> new AddressQueryRequest(corr, Topics.CUST_ADDR_RESP, customerId, addressId),
                Duration.ofSeconds(3)
        );
        AddressDTO a = addr.address(); if (a == null) throw new IllegalStateException("Address not found");

        ShippingRateResponse rate = rr.request(
                Topics.SHIP_RATE_REQ, Topics.SHIP_RATE_RESP, ShippingRateResponse.class,
                corr -> new ShippingRateRequest(corr, Topics.SHIP_RATE_RESP, a.countryId(),
                        firstNonBlank(a.state(), a.city())),
                Duration.ofSeconds(3)
        );

        List<Integer> ids = cart.items().stream().map(CartLine::productId).toList();
        ProductSnapshotResponse prods = rr.request(
                Topics.CATALOG_PROD_SNAPSHOT_REQ, Topics.CATALOG_PROD_SNAPSHOT_RESP, ProductSnapshotResponse.class,
                corr -> new ProductSnapshotRequest(corr, Topics.CATALOG_PROD_SNAPSHOT_RESP, ids),
                Duration.ofSeconds(3)
        );

        Map<Integer, ProductSnapshot> map = new HashMap<>();
        for (ProductSnapshot p : prods.products()) map.put(p.id(), p);

        float productTotal = 0f, shipping = 0f;
        for (CartLine line : cart.items()) {
            ProductSnapshot p = map.get(line.productId());
            float unit = unitPrice(p.price(), p.discountPrice());
            productTotal += unit * line.quantity();

            float length = nz(p.length()), width = nz(p.width()), height = nz(p.height()), weight = nz(p.weight());
            float dimWeight = (length * width * height) / DIM_DIVISOR;
            float finalWeight = Math.max(dimWeight, weight);
            Float rateVal = rate.rate() == null ? 0f : rate.rate();
            shipping += finalWeight * line.quantity() * rateVal;
        }

        boolean codSupported = Boolean.TRUE.equals(rate.codSupported());
        float paymentTotal = productTotal + (codSupported ? shipping : 0f);

        return new Summary(productTotal, shipping, paymentTotal, codSupported);
    }

    private static float unitPrice(Float price, Float discount) { return (discount != null && discount > 0) ? discount : (price == null ? 0f : price); }
    private static float nz(Float f) { return f == null ? 0f : f; }
    private static String firstNonBlank(String a, String b) { return (a != null && !a.isBlank()) ? a : (b == null ? "" : b); }
}