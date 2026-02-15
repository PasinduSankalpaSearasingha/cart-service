package com.example.cartservice.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemForOrder {
    private Long itemId;
    private String itemName;
    private Integer quantity;
    private BigDecimal unitPrice;
}
