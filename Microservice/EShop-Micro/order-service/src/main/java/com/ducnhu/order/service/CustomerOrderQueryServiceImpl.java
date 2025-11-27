package com.ducnhu.order.service;

import com.ducnhu.common.dto.PageResponse;
import com.ducnhu.order.dto.OrderDetailDTO;
import com.ducnhu.order.dto.OrderItemDTO;
import com.ducnhu.order.dto.OrderSummaryDTO;
import com.ducnhu.order.entity.Order;
import com.ducnhu.order.entity.OrderDetail;
import com.ducnhu.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerOrderQueryServiceImpl implements CustomerOrderQueryService {

    private final OrderRepository orderRepository;

    @Override
    public PageResponse<OrderSummaryDTO> listForCustomer(Integer customerId, int page, int size) {
        PageRequest pr = PageRequest.of(Math.max(page - 1, 0), size);
        Page<Order> p = orderRepository.findByCustomerIdOrderByOrderTimeDesc(customerId, pr);

        List<OrderSummaryDTO> items = p.getContent().stream()
                .map(o -> {
                    List<OrderDetail> details = o.getOrderDetails();
                    float productTotal = (float) details.stream()
                            .mapToDouble(OrderDetail::getSubtotal)
                            .sum();
                    float shippingTotal = (float) details.stream()
                            .mapToDouble(OrderDetail::getShippingCost)
                            .sum();
                    return new OrderSummaryDTO(
                            o.getOrderNumber(),
                            o.getOrderTime(),
                            o.getStatus().toString(),
                            o.getPaymentMethod().toString(),
                            details.size(),
                            productTotal,
                            shippingTotal,
                            o.getTotal()
                    );
                })
                .toList();


        return PageResponse.of(items, p.getNumber() + 1, p.getSize(), p.getTotalElements(), p.getTotalPages());
    }

    @Override
    public OrderDetailDTO getDetail(String orderNumber, Integer customerId) {
        Order o = orderRepository.findByOrderNumberAndCustomerId(orderNumber, customerId)
                .orElse(null);
        if (o == null) return null;
        List<OrderDetail> details = o.getOrderDetails();

        List<OrderItemDTO> items = o.getOrderDetails().stream()
                .map(this::mapItem)
                .toList();

        String addr = java.util.stream.Stream.of(
                        (o.getFirstName() + " " + o.getLastName()).trim(),
                        o.getAddressLine1(),
                        o.getAddressLine2(),
                        o.getCity(),
                        o.getState(),
                        o.getPostalCode(),
                        o.getCountry()
                )
                .filter(s -> s != null && !s.isBlank())
                .collect(Collectors.joining(", "));

        float productTotal = (float) details.stream()
                .mapToDouble(OrderDetail::getSubtotal)
                .sum();

        float shippingTotal = (float) details.stream()
                .mapToDouble(OrderDetail::getShippingCost)
                .sum();

        return new OrderDetailDTO(
                o.getOrderNumber(),
                o.getOrderTime(),
                o.getStatus().toString(),
                o.getPaymentMethod().toString(),
                productTotal,
                shippingTotal,
                o.getTotal(),
                addr,
                items
        );
    }

    private OrderItemDTO mapItem(OrderDetail d) {
        return new OrderItemDTO(
                d.getProductId(),
                d.getProductName(),
                d.getProductAlias(),
                d.getProductImage(),
                d.getUnitPrice(),
                d.getQuantity(),
                d.getSubtotal()
        );
    }
}
