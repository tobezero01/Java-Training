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
    public void onPlaced(Object payload) {
        if (payload instanceof OrderPlacedEventV2 e) {
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

            // Payment flag
            order.setPaymentMethod("PAYPAL".equalsIgnoreCase(e.paymentMethod()) ? PaymentMethod.PAYPAL : PaymentMethod.COD);
            order.setStatus(OrderStatus.NEW);

            // Totals (nếu dùng BigDecimal ở entity thì map thẳng; hiện entity là float → convert)
            order.setProductCost(0f);
            order.setSubtotal(e.productTotal());
            order.setShippingCost(e.shippingCost());
            order.setTax(0f);
            order.setTotal(e.paymentTotal());
            order.setDeliverDays(0);
            order.setDeliverDate(null);

            // Items giữ nguyên như V1 (OrderPlacedItem không đổi)
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

            OrderTrack track = new OrderTrack();
            track.setOrder(order);
            track.setStatus(OrderStatus.NEW);
            track.setUpdatedTime(new Date());
            track.setNotes("Order placed (V2 snapshot)");
            order.getOrderTracks().add(track);

            orderRepository.save(order);
            return;
        }

        if (payload instanceof OrderPlacedEvent e) {
            // === Fallback V1: logic hiện tại của bạn ===
            Order order = new Order();
            order.setOrderNumber(e.orderNumber());
            order.setOrderTime(e.createdAt());
            order.setCustomerId(e.customerId());
            order.setCustomerEmail(e.customerEmail());

            // address (thiếu, như code cũ)
            order.setFirstName("");
            order.setLastName("");
            order.setPhoneNumber("");
            order.setAddressLine1(e.addressLine());
            order.setAddressLine2("");
            order.setCity("");
            order.setState("");
            order.setPostalCode("");
            order.setCountry("");

            order.setPaymentMethod(PaymentMethod.COD);
            order.setStatus(OrderStatus.NEW);

            order.setProductCost(0f);
            order.setSubtotal(e.productTotal());
            order.setShippingCost(e.shippingCost());
            order.setTax(0f);
            order.setTotal(e.paymentTotal());
            order.setDeliverDays(0);
            order.setDeliverDate(null);

            for (OrderPlacedItem it : e.items()) { /* như cũ */ }

            OrderTrack track = new OrderTrack();
            track.setOrder(order);
            track.setStatus(OrderStatus.NEW);
            track.setUpdatedTime(new Date());
            track.setNotes("Order placed (V1)");
            order.getOrderTracks().add(track);

            orderRepository.save(order);
        }
    }

    @KafkaListener(topics = Topics.ORDER_PAID_EVENTS, groupId = "order-service")
    public void onPaid(OrderPaidEvent event) {
        Order order = orderRepository.findByOrderNumberAndCustomerId(event.orderNumber(), event.customerId())
                .orElseGet(() -> {
                    Order order1 = new Order();
                    order1.setOrderNumber(event.orderNumber());
                    order1.setCustomerId(event.customerId());
                    order1.setOrderTime(new Date());
                    order1.setCustomerEmail(event.customerEmail());
                    order1.setPaymentMethod(PaymentMethod.PAYPAL);
                    order1.setStatus(OrderStatus.PAID);
                    return order1;
                });
        order.setStatus(OrderStatus.PAID);

        order.setPaymentMethod(PaymentMethod.PAYPAL);
        order.setPaymentTransactionId(event.transactionId());
        order.setPaidAmount(event.paidAmount());
        order.setPaidCurrency(event.currency());
        order.setPaidTime(event.paidTime());

        OrderTrack t = new OrderTrack();
        t.setOrder(order);
        t.setStatus(OrderStatus.PAID);
        t.setUpdatedTime(new Date());
        t.setNotes("PayPal captured");
        order.getOrderTracks().add(t);

        orderRepository.save(order);
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

    @KafkaListener(topics = Topics.ORDER_HAS_PURCHASED_REQ, groupId="order-service")
    public void onHasPurchased(OrderHasPurchasedRequest req){
        boolean purchased = orderRepository.existsPurchased(req.customerId(), req.productId(),
                List.of(OrderStatus.PAID, OrderStatus.DELIVERED));
        kafkaTemplate.send(Topics.ORDER_HAS_PURCHASED_RESP,
                new OrderHasPurchasedResponse(req.correlationId(), req.customerId(), req.productId(), purchased));
    }

}
