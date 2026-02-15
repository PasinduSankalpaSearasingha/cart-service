package com.example.cartservice.dto;

import java.math.BigDecimal;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartInfoDTO {
    private Long userId;
    private List<CartItemInfo> items;
    private BigDecimal totalAmount;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CartItemInfo {
        private Long itemId;
        private String itemName;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal totalPrice;
    }
}
