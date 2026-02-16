package com.example.cartservice.service.Impl;

import com.example.cartservice.dto.AddToCartRequest;
import com.example.cartservice.dto.CartResponse;
import com.example.cartservice.dto.CheckoutResponse;
import com.example.cartservice.dto.UpdateCartItemRequest;
import com.example.cartservice.model.RedisCart;
import com.example.cartservice.service.CartService;
import com.example.cartservice.service.store.CartStore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CartServiceImpl implements CartService {

    private static final Logger log = LoggerFactory.getLogger(CartServiceImpl.class);

    private final CartStore cartStore;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${order.service.url}")
    private String orderServiceUrl;

    public CartServiceImpl(CartStore cartStore) {
        this.cartStore = cartStore;
    }

    private String cartKey(Long userId, Integer tableId) {
        return String.format("cart:%d:%s", userId, tableId != null ? tableId.toString() : "default");
    }

    @Override
    public CartResponse openCart(Long userId, Integer tableId) {
        String key = cartKey(userId, tableId);
        RedisCart cart = cartStore.load(key);
        if (cart == null) {
            cart = RedisCart.builder()
                    .orderId("ORD-" + UUID.randomUUID())
                    .userId(userId)
                    .tableId(tableId)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            cart.recalc();
            cartStore.save(key, cart);
        } else {
            if (cart.getItems() == null)
                cart.setItems(new java.util.ArrayList<>());
        }
        return toCartResponse(cart);
    }

    @Override
    public CartResponse getCart(Long userId, Integer tableId) {
        String key = cartKey(userId, tableId);
        RedisCart cart = cartStore.load(key);
        if (cart == null)
            return openCart(userId, tableId);
        return toCartResponse(cart);
    }

    @Override
    public CartResponse getOrderByOrderId(String orderId) {
        throw new UnsupportedOperationException("getOrderByOrderId not supported in store-backed cart");
    }

    @Override
    public CartResponse addToCart(Long userId, Integer tableId, AddToCartRequest request) {
        String key = cartKey(userId, tableId);
        RedisCart cart = cartStore.load(key);
        if (cart == null) {
            cart = RedisCart.builder()
                    .orderId("ORD-" + UUID.randomUUID())
                    .userId(userId)
                    .tableId(tableId)
                    .createdAt(LocalDateTime.now())
                    .build();
        } else {
            if (cart.getItems() == null)
                cart.setItems(new java.util.ArrayList<>());
        }

        RedisCart.CartItem existing = cart.getItems().stream()
                .filter(i -> i.getMenuItemId().equals(request.getMenuItemId()))
                .findFirst()
                .orElse(null);

        if (existing != null) {
            existing.setQuantity(existing.getQuantity() + request.getQuantity());
            if (request.getNote() != null)
                existing.setNote(request.getNote());
        } else {
            RedisCart.CartItem item = RedisCart.CartItem.builder()
                    .menuItemId(request.getMenuItemId())
                    .itemName(request.getItemName())
                    .price(request.getPrice())
                    .quantity(request.getQuantity())
                    .note(request.getNote())
                    .addedAt(LocalDateTime.now())
                    .build();
            cart.getItems().add(item);
        }

        cart.recalc();
        cartStore.save(key, cart);
        return toCartResponse(cart);
    }

    @Override
    public CartResponse updateCartItem(Long userId, Integer tableId, Long itemId, UpdateCartItemRequest request) {
        String key = cartKey(userId, tableId);
        RedisCart cart = cartStore.load(key);
        if (cart == null)
            throw new RuntimeException("Cart not found");

        RedisCart.CartItem item = cart.getItems().stream()
                .filter(i -> i.getMenuItemId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Item not found"));

        if (request.getQuantity() != null)
            item.setQuantity(request.getQuantity());
        if (request.getNote() != null)
            item.setNote(request.getNote());

        cart.recalc();
        cartStore.save(key, cart);
        return toCartResponse(cart);
    }

    @Override
    public CartResponse removeFromCart(Long userId, Integer tableId, Long itemId) {
        String key = cartKey(userId, tableId);
        RedisCart cart = cartStore.load(key);
        if (cart == null)
            throw new RuntimeException("Cart not found");

        cart.getItems().removeIf(i -> i.getMenuItemId().equals(itemId));
        cart.recalc();
        cartStore.save(key, cart);
        return toCartResponse(cart);
    }

    @Override
    public CartResponse clearCart(Long userId, Integer tableId) {
        String key = cartKey(userId, tableId);
        RedisCart cart = RedisCart.builder()
                .orderId("ORD-" + UUID.randomUUID())
                .userId(userId)
                .tableId(tableId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        cartStore.save(key, cart);
        return toCartResponse(cart);
    }

    @Override
    @Transactional
    public CheckoutResponse checkout(Long userId, Integer tableId, String authHeader) {
        String key = cartKey(userId, tableId);
        RedisCart cart = cartStore.load(key);
        if (cart == null || cart.getItems() == null || cart.getItems().isEmpty())
            throw new RuntimeException("Cart is empty");

        // Build items array for order service: [{itemId, itemName, quantity,
        // unitPrice}, ...]
        var itemsArray = cart.getItems().stream().map(i -> {
            var m = new java.util.HashMap<String, Object>();
            m.put("itemId", i.getMenuItemId());
            m.put("itemName", i.getItemName());
            m.put("quantity", i.getQuantity());
            m.put("unitPrice", i.getPrice());
            return m;
        }).collect(Collectors.toList());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        // Forward the Authorization header if present
        if (authHeader != null && !authHeader.isBlank()) {
            headers.set("Authorization", authHeader);
        }
        // send user id and table id in headers as requested
        headers.set("X-User-Id", String.valueOf(userId));
        headers.set("X-Table-Id", tableId != null ? tableId.toString() : "default");

        var payload = new java.util.HashMap<String, Object>();
        payload.put("items", itemsArray);

        HttpEntity<Object> entity = new HttpEntity<>(payload, headers);

        // Normalize orderServiceUrl
        String originalUrl = orderServiceUrl != null ? orderServiceUrl : "";
        String targetUrl = originalUrl.trim();
        int httpIndex = targetUrl.toLowerCase().indexOf("http");
        if (httpIndex > 0) {
            targetUrl = targetUrl.substring(httpIndex).trim();
        }

        log.debug("Original order.service.url='{}' -> normalized='{}'", originalUrl, targetUrl);

        if (!(targetUrl.startsWith("http://") || targetUrl.startsWith("https://"))) {
            throw new RuntimeException("Invalid order service URL: '" + originalUrl
                    + "'. It must contain an absolute URL starting with http:// or https://");
        }

        log.debug("Sending order to order-service at URL='{}' userId='{}' table='{}' payloadItemsCount={}", targetUrl,
                userId, tableId, itemsArray.size());

        ResponseEntity<String> resp = restTemplate.postForEntity(targetUrl, entity, String.class);

        // if success, delete cart from store
        if (resp.getStatusCode().is2xxSuccessful()) {
            cartStore.delete(key);
            return CheckoutResponse.builder()
                    .orderId(cart.getOrderId())
                    .userId(cart.getUserId())
                    .status("SENT")
                    .items(cart.getItems().stream().map(i -> CheckoutResponse.OrderItemDetail.builder()
                            .itemId(i.getMenuItemId())
                            .itemName(i.getItemName())
                            .quantity(i.getQuantity())
                            .unitPrice(i.getPrice())
                            .subtotal(i.getSubtotal())
                            .build()).collect(Collectors.toList()))
                    .totalAmount(cart.getTotalAmount())
                    .confirmedAt(LocalDateTime.now())
                    .message("Order sent to order service")
                    .build();
        }

        throw new RuntimeException("Failed to send order to order service: " + resp.getStatusCode());
    }

    private CartResponse toCartResponse(RedisCart cart) {
        return CartResponse.builder()
                .orderId(cart.getOrderId())
                .status("PENDING")
                .userId(cart.getUserId())
                .items(cart.getItems().stream().map(i -> CartResponse.CartItemResponse.builder()
                        .menuItemId(i.getMenuItemId())
                        .itemName(i.getItemName())
                        .price(i.getPrice())
                        .quantity(i.getQuantity())
                        .note(i.getNote())
                        .subtotal(i.getSubtotal())
                        .addedAt(i.getAddedAt())
                        .build()).collect(Collectors.toList()))
                .totalAmount(cart.getTotalAmount())
                .totalItems(cart.getItems().stream().mapToInt(RedisCart.CartItem::getQuantity).sum())
                .createdAt(cart.getCreatedAt())
                .updatedAt(cart.getUpdatedAt())
                .build();
    }
}
