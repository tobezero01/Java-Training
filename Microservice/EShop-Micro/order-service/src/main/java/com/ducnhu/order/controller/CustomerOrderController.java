package com.ducnhu.order.controller;

import com.ducnhu.common.dto.PageResponse;
import com.ducnhu.order.dto.OrderDetailDTO;
import com.ducnhu.order.dto.OrderSummaryDTO;
import com.ducnhu.order.service.CustomerOrderQueryService;
import com.ducnhu.order.client.AuthClient;
import com.ducnhu.order.client.MeResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class CustomerOrderController {

    private final CustomerOrderQueryService service;
    private final AuthClient authClient;

    private Integer meId() {
        MeResponse me = authClient.me();
        if (me == null || me.id() == null)
            throw new ResponseStatusException(UNAUTHORIZED, "Missing/invalid token");
        return me.id();
    }

    @GetMapping
    public PageResponse<OrderSummaryDTO> list(
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        return service.listForCustomer(meId(), page, size);
    }

    @GetMapping("/{orderNumber}")
    public OrderDetailDTO detail(@PathVariable("orderNumber") String orderNumber) {
        OrderDetailDTO dto = service.getDetail(orderNumber, meId());
        if (dto == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.NOT_FOUND, "Order not found");
        }
        return dto;
    }
}
