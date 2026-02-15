package com.example.cartservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RedisCart {
    private String orderId;
    private Long userId;
    private String tableName; // from gateway
    @Builder.Default
    private List<CartItem> items = new ArrayList<>();
    private BigDecimal totalAmount = BigDecimal.ZERO;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CartItem {
        private Long menuItemId;
        private String itemName;
        private BigDecimal price;
        private Integer quantity;
        private String note;
        private LocalDateTime addedAt;

        public BigDecimal getSubtotal() {
            return price.multiply(BigDecimal.valueOf(quantity));
        }
    }

    public void recalc() {
        if (items == null || items.isEmpty()) {
            totalAmount = BigDecimal.ZERO;
        } else {
            totalAmount = items.stream()
                    .map(CartItem::getSubtotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        updatedAt = LocalDateTime.now();
        if (createdAt == null) createdAt = updatedAt;
        if (items == null) items = new ArrayList<>();
    }
}
