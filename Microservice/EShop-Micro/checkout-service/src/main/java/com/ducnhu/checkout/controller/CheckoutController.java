package com.ducnhu.checkout.controller;

import com.ducnhu.checkout.dto.CheckoutSummaryDTO;
import com.ducnhu.checkout.dto.MeResponse;
import com.ducnhu.checkout.dto.PlaceOrderRequest;
import com.ducnhu.checkout.dto.PlaceOrderResponse;
import com.ducnhu.checkout.service.AuthClient;
import com.ducnhu.checkout.service.CheckoutService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/checkout")
@RequiredArgsConstructor
public class CheckoutController {
    private final AuthClient auth;
    private final CheckoutService svc;

    @GetMapping("/summary")
    public CheckoutSummaryDTO summary(@RequestParam(required = false) Integer addressId) {
        MeResponse me = auth.me();
        return svc.summarize(me.id(), addressId);
    }

    @PostMapping("/place-order")
    public ResponseEntity<PlaceOrderResponse> cod(@RequestBody PlaceOrderRequest req) {
        MeResponse me = auth.me();
        // buộc COD trong orchestrator (giống monolith) :contentReference[oaicite:8]{index=8}
        PlaceOrderRequest cod = new PlaceOrderRequest(req.addressId(), "COD", req.note());
        return ResponseEntity.ok(svc.placeOrderCod(me.id(), me.email(), cod));
    }

    @PostMapping("/cancel-order")
    public ResponseEntity<?> cancel(@RequestParam String orderNumber,
                                    @RequestParam(required = false, defaultValue = "User requested cancellation") String reason) {
        MeResponse me = auth.me(); // xác thực & lấy customerId
        svc.compensateCancel(orderNumber, me.id(), reason);
        return ResponseEntity.accepted().body(Map.of(
                "cancelled", true,
                "orderNumber", orderNumber,
                "reason", reason
        ));
    }
}