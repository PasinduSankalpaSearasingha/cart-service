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
public class CheckoutResponse {
    
    private String orderId;
    private Long userId;
    private String status;
    private List<OrderItemDetail> items;
    private BigDecimal totalAmount;
    private LocalDateTime confirmedAt;
    private String message;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OrderItemDetail {
        private Long itemId;       // was menuItemId
        private String itemName;
        private Integer quantity;
        private BigDecimal unitPrice; // was price
        private BigDecimal subtotal;
    }
}