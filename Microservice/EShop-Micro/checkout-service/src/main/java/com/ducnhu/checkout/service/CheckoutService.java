package com.ducnhu.checkout.service;

import com.ducnhu.checkout.dto.CheckoutSummaryDTO;
import com.ducnhu.checkout.dto.PlaceOrderRequest;
import com.ducnhu.checkout.dto.PlaceOrderResponse;

public interface CheckoutService {
    CheckoutSummaryDTO summarize(Integer customerId, Integer addressId);

    PlaceOrderResponse placeOrderCod(Integer customerId, String customerEmail, PlaceOrderRequest req);

    void compensateCancel(String orderNumber, Integer customerId, String reason);
}
