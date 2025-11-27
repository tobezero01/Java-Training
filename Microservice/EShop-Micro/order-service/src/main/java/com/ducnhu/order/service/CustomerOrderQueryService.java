package com.ducnhu.order.service;


import com.ducnhu.common.dto.PageResponse;
import com.ducnhu.order.dto.OrderDetailDTO;
import com.ducnhu.order.dto.OrderSummaryDTO;

public interface CustomerOrderQueryService {
    PageResponse<OrderSummaryDTO> listForCustomer(Integer customerId, int page, int size);
    OrderDetailDTO getDetail(String orderNumber, Integer customerId);
}
