package com.ducnhu.order.kafka;

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
    public void onPlaced(OrderPlacedEvent event) {
        Order order = new Order();
        order.setOrderNumber(event.orderNumber());
        order.setOrderTime(event.createdAt());
        order.setCustomerId(event.customerId());
        order.setCustomerEmail(event.customerEmail());

        // address
        order.setFirstName("");
        order.setLastName("");
        order.setPhoneNumber("");
        order.setAddressLine1(event.addressLine());
        order.setAddressLine2("");
        order.setCity("");
        order.setState("");
        order.setPostalCode("");
        order.setCountry("");

        order.setPaymentMethod(PaymentMethod.COD);
        order.setStatus(OrderStatus.NEW);

        order.setProductCost(0f); // không tính riêng cost ở event
        order.setSubtotal(event.productTotal());
        order.setShippingCost(event.shippingCost());
        order.setTax(0f);
        order.setTotal(event.paymentTotal());
        order.setDeliverDays(0);
        order.setDeliverDate(null);


        for (OrderPlacedItem it : event.items()) {
            OrderDetail detail = new OrderDetail();
            detail.setOrder(order);
            detail.setProductId(it.productId());
            detail.setProductName(it.name());
            detail.setProductAlias(it.alias());
            detail.setProductImage(it.image());
            detail.setUnitPrice(it.unitPrice());
            detail.setQuantity(it.quantity());
            detail.setSubtotal(it.subtotal());
            detail.setShippingCost(it.shippingCost());
            detail.setProductCost(0f);
            order.getOrderDetails().add(detail);
        }

        OrderTrack track = new OrderTrack();
        track.setOrder(order);
        track.setStatus(OrderStatus.NEW);
        track.setUpdatedTime(new Date());
        track.setNotes("Order placed");
        order.getOrderTracks().add(track);

        orderRepository.save(order);
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

    @KafkaListener(topics = Topics.ORDER_HAS_PURCHASED_REQ, groupId="order-service")
    public void onHasPurchased(OrderHasPurchasedRequest req){
        boolean purchased = orderRepository.existsPurchased(req.customerId(), req.productId(),
                List.of(OrderStatus.PAID, OrderStatus.DELIVERED));
        kafkaTemplate.send(Topics.ORDER_HAS_PURCHASED_RESP,
                new OrderHasPurchasedResponse(req.correlationId(), req.customerId(), req.productId(), purchased));
    }

}
