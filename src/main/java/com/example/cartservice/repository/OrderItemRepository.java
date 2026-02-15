package com.example.cartservice.repository;

import com.example.cartservice.entity.OrderItem;

import java.util.List;

public interface OrderItemRepository {
    OrderItem save(OrderItem item);
    void delete(OrderItem item);
    List<OrderItem> findByOrderId(String orderId);
}