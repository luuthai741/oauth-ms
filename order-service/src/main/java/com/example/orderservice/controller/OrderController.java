package com.example.orderservice.controller;

import com.example.orderservice.model.CreateOrderRequest;
import com.example.orderservice.model.Order;
import com.example.orderservice.model.UpdateOrderRequest;
import com.example.orderservice.security.UserPrincipal;
import com.example.orderservice.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<?> createOrder(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateOrderRequest request) {
        ResponseEntity<?> validationError = validateCreateRequest(request);
        if (validationError != null) {
            return validationError;
        }
        Order order = orderService.createOrder(principal.getUserId(), principal.getUsername(), request.getDescription(), request.getAmount());
        return ResponseEntity.ok(order);
    }

    @GetMapping("/{orderId}")
    @PreAuthorize("@orderSecurity.canAccessOrder(authentication, #orderId)")
    public ResponseEntity<?> getOrder(
            @PathVariable String orderId,
            @AuthenticationPrincipal UserPrincipal principal) {
        Order order = orderService.getOrderById(orderId);
        if (order == null) {
            return new ResponseEntity<>("Order not found", HttpStatus.NOT_FOUND);
        }

        if (!principal.hasRole("ADMIN") && !order.getUserId().equals(principal.getUserId())) {
            return new ResponseEntity<>("Forbidden", HttpStatus.FORBIDDEN);
        }

        return ResponseEntity.ok(order);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<List<Order>> getOrders(@AuthenticationPrincipal UserPrincipal principal) {
        List<Order> orders;
        if (principal.hasRole("ADMIN")) {
            orders = orderService.getAllOrders();
        } else {
            orders = orderService.getUserOrders(principal.getUserId());
        }
        return ResponseEntity.ok(orders);
    }

    @PutMapping("/{orderId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateOrder(
            @PathVariable String orderId,
            @Valid @RequestBody UpdateOrderRequest request) {
        ResponseEntity<?> validationError = validateUpdateRequest(request);
        if (validationError != null) {
            return validationError;
        }
        Order existingOrder = orderService.getOrderById(orderId);
        if (existingOrder == null) {
            return new ResponseEntity<>("Order not found", HttpStatus.NOT_FOUND);
        }

        Order orderUpdate = Order.builder()
                .id(orderId)
                .userId(existingOrder.getUserId())
                .username(existingOrder.getUsername())
                .description(request.getDescription())
                .amount(request.getAmount())
                .status(request.getStatus())
                .createdAt(existingOrder.getCreatedAt())
                .updatedAt(existingOrder.getUpdatedAt())
                .build();

        Order updated = orderService.updateOrder(orderId, orderUpdate);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{orderId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteOrder(@PathVariable String orderId) {
        Order order = orderService.getOrderById(orderId);
        if (order == null) {
            return new ResponseEntity<>("Order not found", HttpStatus.NOT_FOUND);
        }

        orderService.deleteOrder(orderId);
        return ResponseEntity.ok("Order deleted successfully");
    }

    private ResponseEntity<?> validateCreateRequest(CreateOrderRequest request) {
        if (request == null || isBlank(request.getDescription()) || request.getAmount() == null || request.getAmount() <= 0) {
            return new ResponseEntity<>("Description is required and amount must be greater than 0", HttpStatus.BAD_REQUEST);
        }
        return null;
    }

    private ResponseEntity<?> validateUpdateRequest(UpdateOrderRequest request) {
        if (request == null
                || isBlank(request.getDescription())
                || request.getAmount() == null
                || request.getAmount() <= 0
                || isBlank(request.getStatus())) {
            return new ResponseEntity<>("Description, status and positive amount are required", HttpStatus.BAD_REQUEST);
        }
        return null;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}



