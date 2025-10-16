package com.ducnhu.cart;

import com.ducnhu.cart.dto.AddReq;
import com.ducnhu.cart.dto.CartActionResp;
import com.ducnhu.cart.dto.UpdateReq;
import com.ducnhu.cart.service.ShoppingCartService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final ShoppingCartService cart;
    private final AuthClient authClient;

    private Integer me() {
        return authClient.me().id();
    }

    @PostMapping("/items")
    public ResponseEntity<?> add(@RequestBody AddReq req) {
        Integer q = cart.addProduct(me(), req.productId(), req.quantity());
        float subtotal = cart.updateQuantity(me(), req.productId(), q);
        return ResponseEntity.ok(new CartActionResp(req.productId(), q, subtotal, "Add to cart"));
    }

    @PatchMapping("/items/{productId}")
    public ResponseEntity<?> update(@PathVariable Integer productId, @RequestBody UpdateReq req) {
        float subtotal = cart.updateQuantity(me(), productId, req.quantity());
        return ResponseEntity.ok(new CartActionResp(productId, req.quantity(), subtotal, "Quantity updated"));
    }

    @DeleteMapping("/items/{productId}")
    public ResponseEntity<?> remove(@PathVariable Integer productId) {
        cart.remove(me(), productId);
        return ResponseEntity.ok(new CartActionResp(productId, 0, 0f, "Item removed"));
    }

    @DeleteMapping
    public ResponseEntity<?> clear() {
        cart.clear(me());
        return ResponseEntity.ok(new CartActionResp(null, 0, 0f, "Cart cleared"));
    }

}