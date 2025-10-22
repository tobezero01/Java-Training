package com.ducnhu.cart.service;

import com.ducnhu.cart.dto.AddResult;
import com.ducnhu.cart.entity.CartItem;

import java.util.List;

public interface ShoppingCartService {
    AddResult addProduct(Integer customerId, Integer productId, Integer quantity);
    float   updateQuantity(Integer customerId, Integer productId, Integer quantity);
    void    remove(Integer customerId, Integer productId);
    void    clear(Integer customerId);
    List<CartItem> list(Integer customerId);
}
