package com.example.orderservice.service;

import com.example.orderservice.model.Order;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private final Map<String, Order> orders = new ConcurrentHashMap<>();

    public Order createOrder(String userId, String username, String description, Double amount) {
        Order order = Order.builder()
            .id(UUID.randomUUID().toString())
            .userId(userId)
            .username(username)
            .description(description)
            .amount(amount)
            .status("PENDING")
            .createdAt(System.currentTimeMillis())
            .updatedAt(System.currentTimeMillis())
            .build();

        orders.put(order.getId(), order);
        return order;
    }

    public Order getOrderById(String orderId) {
        return orders.get(orderId);
    }

    public List<Order> getUserOrders(String userId) {
        return orders.values().stream()
            .filter(order -> Objects.equals(order.getUserId(), userId))
            .collect(Collectors.toList());
    }

    public List<Order> getAllOrders() {
        return new ArrayList<>(orders.values());
    }

    public Order updateOrder(String orderId, Order order) {
        if (!orders.containsKey(orderId)) {
            throw new IllegalArgumentException("Order not found");
        }
        order.setId(orderId);
        order.setUpdatedAt(System.currentTimeMillis());
        orders.put(orderId, order);
        return order;
    }

    public void deleteOrder(String orderId) {
        orders.remove(orderId);
    }
}

