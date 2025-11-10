package com.ducnhu.cart;

import com.ducnhu.cart.dto.AddReq;
import com.ducnhu.cart.dto.AddResult;
import com.ducnhu.cart.dto.CartActionResp;
import com.ducnhu.cart.dto.CartGetResponse;
import com.ducnhu.cart.dto.CartLineView;
import com.ducnhu.cart.dto.MeResponse;
import com.ducnhu.cart.dto.UpdateReq;
import com.ducnhu.cart.entity.CartItem;
import com.ducnhu.cart.service.ShoppingCartService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final ShoppingCartService cart;
    private final AuthClient authClient;

    private Integer meId(HttpServletRequest req) {
        String authz = req.getHeader("Authorization");
        if (authz == null || authz.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing Authorization");
        }
        MeResponse me = authClient.me(authz);
        if (me == null || me.id() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid JWT");
        }
        return me.id();
    }

    /**
     * GET /api/cart/items — trả toàn bộ giỏ của người dùng hiện tại (customerId lấy từ /me).
     */
    @GetMapping("/items")
    public CartGetResponse getAll(HttpServletRequest request) {
        Integer customerId = meId(request);

        List<CartItem> items = cart.list(customerId);

        List<CartLineView> views = items.stream()
                .map(it -> new CartLineView(
                        it.getProductId(),
                        it.getName(),
                        it.getAlias(),
                        it.getImage(),
                        it.getQuantity(),
                        it.getPrice(),
                        it.getDiscountPrice(),
                        it.getSubtotal()
                ))
                .toList();

        int itemCount = views.size();
        int totalQuantity = items.stream().mapToInt(CartItem::getQuantity).sum();
        float totalAmount = (float) items.stream().mapToDouble(CartItem::getSubtotal).sum();

        return new CartGetResponse(views, itemCount, totalQuantity, totalAmount);
    }

    /**
     * POST /api/cart/items — thêm 1 sản phẩm vào giỏ.
     */
    @PostMapping("/items")
    public ResponseEntity<CartActionResp> add(@RequestBody AddReq req, HttpServletRequest request) {
        AddResult r = cart.addProduct(meId(request), req.productId(), req.quantity());
        return ResponseEntity.ok(new CartActionResp(req.productId(), r.quantity(), r.subtotal(), "Add to cart"));
    }

    /**
     * PATCH /api/cart/items/{productId} — cập nhật số lượng.
     */
    @PatchMapping("/items/{productId}")
    public ResponseEntity<CartActionResp> update(@PathVariable("productId") Integer productId,
                                                 @RequestBody UpdateReq req,
                                                 HttpServletRequest request) {
        float subtotal = cart.updateQuantity(meId(request), productId, req.quantity());
        return ResponseEntity.ok(new CartActionResp(productId, req.quantity(), subtotal, "Quantity updated"));
    }

    /**
     * DELETE /api/cart/items/{productId} — xóa 1 dòng khỏi giỏ.
     */
    @DeleteMapping("/items/{productId}")
    public ResponseEntity<CartActionResp> remove(@PathVariable("productId") Integer productId, HttpServletRequest request) {
        cart.remove(meId(request), productId);
        return ResponseEntity.ok(new CartActionResp(productId, 0, 0f, "Item removed"));
    }

    /**
     * DELETE /api/cart — xóa toàn bộ giỏ.
     */
    @DeleteMapping
    public ResponseEntity<CartActionResp> clear(HttpServletRequest request) {
        cart.clear(meId(request));
        return ResponseEntity.ok(new CartActionResp(null, 0, 0f, "Cart cleared"));
    }
}
