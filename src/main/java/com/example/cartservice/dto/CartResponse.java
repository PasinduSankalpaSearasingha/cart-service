package com.example.cartservice.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartResponse {
    
    private String orderId;
    private String status;
    private Long userId;
    private List<CartItemResponse> items;
    private BigDecimal totalAmount;
    private int totalItems;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CartItemResponse {
        private Long itemId;
        private Long menuItemId;
        private String itemName;
        private BigDecimal price;
        private Integer quantity;
        private String note;
        private BigDecimal subtotal;
        private LocalDateTime addedAt;
    }
}