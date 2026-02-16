package com.example.cartservice.service;

import com.example.cartservice.dto.AddToCartRequest;
import com.example.cartservice.dto.CartResponse;
import com.example.cartservice.dto.CheckoutResponse;
import com.example.cartservice.dto.UpdateCartItemRequest;

public interface CartService {
    CartResponse openCart(Long userId, Integer tableId);

    CartResponse getCart(Long userId, Integer tableId);

    CartResponse getOrderByOrderId(String orderId);

    CartResponse addToCart(Long userId, Integer tableId, AddToCartRequest request);

    CartResponse updateCartItem(Long userId, Integer tableId, Long itemId, UpdateCartItemRequest request);

    CartResponse removeFromCart(Long userId, Integer tableId, Long itemId);

    CartResponse clearCart(Long userId, Integer tableId);

    CheckoutResponse checkout(Long userId, Integer tableId, String authHeader);
}