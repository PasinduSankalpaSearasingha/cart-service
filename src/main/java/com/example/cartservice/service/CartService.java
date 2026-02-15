package com.example.cartservice.service;

import com.example.cartservice.dto.AddToCartRequest;
import com.example.cartservice.dto.CartResponse;
import com.example.cartservice.dto.CheckoutResponse;
import com.example.cartservice.dto.UpdateCartItemRequest;

public interface CartService {
    CartResponse openCart(Long userId, String tableName);
    CartResponse getCart(Long userId, String tableName);
    CartResponse getOrderByOrderId(String orderId);
    CartResponse addToCart(Long userId, String tableName, AddToCartRequest request);
    CartResponse updateCartItem(Long userId, String tableName, Long itemId, UpdateCartItemRequest request);
    CartResponse removeFromCart(Long userId, String tableName, Long itemId);
    CartResponse clearCart(Long userId, String tableName);
    CheckoutResponse checkout(Long userId, String tableName);
}