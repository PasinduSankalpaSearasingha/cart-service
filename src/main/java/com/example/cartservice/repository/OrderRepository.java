package com.example.cartservice.repository;

import com.example.cartservice.entity.Order;
import com.example.cartservice.entity.OrderStatus;

import java.util.List;
import java.util.Optional;

public interface OrderRepository {
    Order save(Order order);
    Order findByOrderId(String orderId);
    Optional<Order> findByUserIdAndStatus(Long userId, OrderStatus status);
    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);
    boolean existsByUserIdAndStatus(Long userId, OrderStatus status);
}