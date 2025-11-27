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
                .map(o -> new OrderSummaryDTO(
                        o.getOrderNumber(),
                        o.getOrderTime(),
                        o.getStatus().toString(),              // tuỳ enum / string trong entity
                        o.getPaymentMethod().toString(),
                        o.getOrderDetails().size(),
                        o.getProductCost(),
                        o.getShippingCost(),
                        o.getTotal()
                ))
                .toList();

        return PageResponse.of(items, p.getNumber() + 1, p.getSize(), p.getTotalElements(), p.getTotalPages());
    }

    @Override
    public OrderDetailDTO getDetail(String orderNumber, Integer customerId) {
        Order o = orderRepository.findByOrderNumberAndCustomerId(orderNumber, customerId)
                .orElse(null);
        if (o == null) return null;

        List<OrderItemDTO> items = o.getOrderDetails().stream()
                .map(this::mapItem)
                .toList();

        String addr = o.getAddressLine1(); // nếu entity tách nhiều trường, ở đây anh ghép lại chuỗi.

        return new OrderDetailDTO(
                o.getOrderNumber(),
                o.getOrderTime(),
                o.getStatus().toString(),
                o.getPaymentMethod().toString(),
                o.getProductCost(),
                o.getShippingCost(),
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
