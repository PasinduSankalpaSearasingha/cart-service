package com.example.cartservice.controller;

import com.example.cartservice.dto.*;
import com.example.cartservice.service.CartService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/cart")
public class CartController {
    
    @Autowired
    private CartService cartService;

    private Long resolveUserId(Authentication authentication, String userIdHeader) {
        if (authentication != null && authentication.isAuthenticated() && authentication.getPrincipal() != null) {
            Object p = authentication.getPrincipal();
            if (p instanceof Long) return (Long) p;
            if (p instanceof String) {
                try {
                    return Long.parseLong((String) p);
                } catch (NumberFormatException ignored) {
                }
            }
        }
        if (userIdHeader != null && !userIdHeader.isBlank()) {
            try {
                return Long.parseLong(userIdHeader);
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("X-User-Id header must be a number");
            }
        }
        throw new IllegalStateException("No authenticated user and no X-User-Id header provided");
    }

    @PostMapping("/open")
    public ResponseEntity<CartResponse> openCart(Authentication authentication,
                                                @RequestHeader(value = "X-Table-Name", required = false) String tableName,
                                                @RequestHeader(value = "X-User-Id", required = false) String userIdHeader) {
        Long userId = resolveUserId(authentication, userIdHeader);

        CartResponse response = cartService.openCart(userId, tableName);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    
    @GetMapping("/items")
    public ResponseEntity<List<CartResponse.CartItemResponse>> getCartItems(Authentication authentication,
                                                     @RequestHeader(value = "X-Table-Name", required = false) String tableName,
                                                     @RequestHeader(value = "X-User-Id", required = false) String userIdHeader) {
        Long userId = resolveUserId(authentication, userIdHeader);
        CartResponse cart = cartService.getCart(userId, tableName);
        return ResponseEntity.ok(cart.getItems());
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<CartResponse> getOrderByOrderId(@PathVariable String orderId) {
        return ResponseEntity.ok(cartService.getOrderByOrderId(orderId));
    }
    
    @PostMapping({"/items", "/item"})
    public ResponseEntity<CartResponse> addToCart(
            Authentication authentication,
            @RequestHeader(value = "X-Table-Name", required = false) String tableName,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @Valid @RequestBody AddToCartRequest request) {
        
        Long userId = resolveUserId(authentication, userIdHeader);

        CartResponse response = cartService.addToCart(userId, tableName, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @PutMapping("/items/{itemId}")
    public ResponseEntity<CartResponse> updateCartItem(
            Authentication authentication,
            @PathVariable Long itemId,
            @Valid @RequestBody UpdateCartItemRequest request,
            @RequestHeader(value = "X-Table-Name", required = false) String tableName,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader) {

        Long userId = resolveUserId(authentication, userIdHeader);

        return ResponseEntity.ok(cartService.updateCartItem(userId, tableName, itemId, request));
    }
    
    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<CartResponse> removeFromCart(
            Authentication authentication,
            @PathVariable Long itemId,
            @RequestHeader(value = "X-Table-Name", required = false) String tableName,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader) {

        Long userId = resolveUserId(authentication, userIdHeader);

        return ResponseEntity.ok(cartService.removeFromCart(userId, tableName, itemId));
    }
    
    @DeleteMapping
    public ResponseEntity<CartResponse> clearCart(Authentication authentication,
                                                  @RequestHeader(value = "X-Table-Name", required = false) String tableName,
                                                  @RequestHeader(value = "X-User-Id", required = false) String userIdHeader) {
        Long userId = resolveUserId(authentication, userIdHeader);

        return ResponseEntity.ok(cartService.clearCart(userId, tableName));
    }
    
    @PostMapping("/checkout")
    public ResponseEntity<CheckoutResponse> checkout(Authentication authentication,
                                                     @RequestHeader(value = "X-Table-Name", required = false) String tableName,
                                                     @RequestHeader(value = "X-User-Id", required = false) String userIdHeader) {
        Long userId = resolveUserId(authentication, userIdHeader);

        return ResponseEntity.ok(cartService.checkout(userId, tableName));
    }
    @GetMapping
    public ResponseEntity<ItemsResponse> getItemsForOrder(
            Authentication authentication,
            @RequestHeader(value = "X-Table-Name", required = false) String tableName,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader)  {

        Long userId = resolveUserId(authentication, userIdHeader);
        CartResponse cart = cartService.getCart(userId, tableName);

        List<ItemForOrder> items = cart.getItems().stream()
                .map(i -> ItemForOrder.builder()
                        .itemId(i.getMenuItemId())
                        .itemName(i.getItemName())
                        .quantity(i.getQuantity())
                        .unitPrice(i.getPrice())
                        .build())
                .collect(Collectors.toList());

        return ResponseEntity.ok(new ItemsResponse(items));
    }
}