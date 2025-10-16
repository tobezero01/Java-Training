package com.ducnhu.cart.dto;

 public record CartActionResp(Integer productId, Integer quantity, Float subtotal, String message) {
}