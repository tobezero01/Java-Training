package com.ducnhu.order.kafka;

import com.ducnhu.common.events.customer.AddressSnapshot;
import com.ducnhu.common.events.orders.*;
import com.ducnhu.common.kafka.Topics;
import com.ducnhu.order.entity.*;
import com.ducnhu.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

@Component
@RequiredArgsConstructor
public class OrderHandlers {
    private final OrderRepository orderRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @KafkaListener(topics = Topics.ORDER_EVENTS, groupId = "order-service")
    public void onPlaced(OrderPlacedEventV2 e) {
        Order order = new Order();
        order.setOrderNumber(e.orderNumber());
        order.setOrderTime(e.createdAt());

        // Customer snapshot
        order.setCustomerId(e.customer().id());
        order.setCustomerEmail(e.customer().email());

        // Address snapshot
        AddressSnapshot s = e.shippingAddress();
        order.setFirstName(s.firstName());
        order.setLastName(s.lastName());
        order.setPhoneNumber(s.phoneNumber());
        order.setAddressLine1(s.line1());
        order.setAddressLine2(s.line2());
        order.setCity(s.city());
        order.setState(s.state());
        order.setPostalCode(s.postalCode());
        order.setCountry(s.country());

        // Payment/Status
        order.setPaymentMethod("PAYPAL".equalsIgnoreCase(e.paymentMethod()) ? PaymentMethod.PAYPAL : PaymentMethod.COD);
        order.setStatus(OrderStatus.NEW);

        // Totals
        order.setProductCost(0f);
        order.setSubtotal(e.productTotal());
        order.setShippingCost(e.shippingCost());
        order.setTax(0f);
        order.setTotal(e.paymentTotal());
        order.setDeliverDays(0);
        order.setDeliverDate(null);

        // Items
        for (OrderPlacedItem it : e.items()) {
            OrderDetail d = new OrderDetail();
            d.setOrder(order);
            d.setProductId(it.productId());
            d.setProductName(it.name());
            d.setProductAlias(it.alias());
            d.setProductImage(it.image());
            d.setUnitPrice(it.unitPrice());
            d.setQuantity(it.quantity());
            d.setSubtotal(it.subtotal());
            d.setShippingCost(it.shippingCost());
            d.setProductCost(0f);
            order.getOrderDetails().add(d);
        }

        // Track
        OrderTrack track = new OrderTrack();
        track.setOrder(order);
        track.setStatus(OrderStatus.NEW);
        track.setUpdatedTime(new java.util.Date());
        track.setNotes("Order placed (V2 snapshot)");
        order.getOrderTracks().add(track);

        orderRepository.save(order);
    }

    @KafkaListener(topics = Topics.ORDER_PAID_EVENTS, groupId = "order-service")
    public void onPaid(OrderPaidEventV2  ev) {
        Order order = orderRepository.findByOrderNumberAndCustomerId(ev.orderNumber(), ev.customerId())
                .orElseGet(() -> {
                    Order o = new Order();
                    o.setOrderNumber(ev.orderNumber());
                    o.setCustomerId(ev.customerId());
                    o.setOrderTime(new Date());
                    o.setCustomerEmail(ev.customerEmail());
                    o.setPaymentMethod(PaymentMethod.PAYPAL);
                    o.setStatus(OrderStatus.NEW);

                    AddressSnapshot s = ev.shippingAddress();
                    String line1 = (s != null && s.line1()!=null && !s.line1().isBlank()) ? s.line1() : "N/A";
                    String country = (s != null && s.country()!=null && !s.country().isBlank()) ? s.country() : "Unknown";

                    o.setFirstName(s != null ? nz(s.firstName()) : "");
                    o.setLastName(s != null ? nz(s.lastName()) : "");
                    o.setPhoneNumber(s != null ? nz(s.phoneNumber()) : "");
                    o.setAddressLine1(line1);
                    o.setAddressLine2(s != null ? nz(s.line2()) : "");
                    o.setCity(s != null ? nz(s.city()) : "");
                    o.setState(s != null ? nz(s.state()) : "");
                    o.setPostalCode(s != null ? nz(s.postalCode()) : "");
                    o.setCountry(country);

                    o.setSubtotal(0f);
                    o.setShippingCost(0f);
                    o.setTax(0f);
                    o.setTotal(ev.paidAmount());
                    return o;
                });
        if (ev.deliverDays() != null) order.setDeliverDays(ev.deliverDays());
        if (ev.deliverDate() != null) order.setDeliverDate(ev.deliverDate());
        order.setStatus(OrderStatus.PAID);
        order.setPaymentMethod(PaymentMethod.PAYPAL);
        order.setPaymentTransactionId(ev.transactionId());
        order.setPaidAmount(ev.paidAmount());
        order.setPaidCurrency(ev.currency());
        order.setPaidTime(ev.paidTime());

        OrderTrack t = new OrderTrack();
        t.setOrder(order);
        t.setStatus(OrderStatus.PAID);
        t.setUpdatedTime(new Date());
        t.setNotes("PayPal captured (V2)");
        order.getOrderTracks().add(t);

        orderRepository.save(order);
        return;
    }

    @KafkaListener(topics = Topics.ORDER_CANCELLED_EVENTS, groupId = "order-service")
    public void onCancelled(OrderCancelledEvent ev) {
        orderRepository.findByOrderNumberAndCustomerId(ev.orderNumber(), ev.customerId()).ifPresent(order -> {
            order.setStatus(OrderStatus.CANCELLED);
            OrderTrack t = new OrderTrack();
            t.setOrder(order);
            t.setStatus(OrderStatus.CANCELLED);
            t.setUpdatedTime(new Date());
            t.setNotes("Cancelled: " + ev.reason());
            order.getOrderTracks().add(t);
            orderRepository.save(order);
        });
    }

    @KafkaListener(topics = Topics.ORDER_HAS_PURCHASED_REQ, groupId = "order-service")
    public void onHasPurchased(OrderHasPurchasedRequest req) {
        boolean purchased = orderRepository.existsPurchased(req.customerId(), req.productId(),
                List.of(OrderStatus.PAID, OrderStatus.DELIVERED));
        kafkaTemplate.send(Topics.ORDER_HAS_PURCHASED_RESP,
                new OrderHasPurchasedResponse(req.correlationId(), req.customerId(), req.productId(), purchased));
    }
    private static String nz(String s){ return s == null ? "" : s; }

}
