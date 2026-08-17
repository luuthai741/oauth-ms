package com.example.orderservice.security;

import com.example.orderservice.exception.OrderAccessDeniedException;
import com.example.orderservice.exception.OrderNotFoundException;
import com.example.orderservice.model.Order;
import com.example.orderservice.service.OrderService;
import lombok.AllArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("orderSecurity")
@AllArgsConstructor
public class OrderSecurity {
    private final OrderService orderService;

    /**
     * Kiểm tra quyền truy cập order.
     * - ADMIN: luôn cho phép.
     * - Order không tồn tại: throw OrderNotFoundException → 404.
     * - Order không thuộc user hiện tại: throw OrderAccessDeniedException → 403.
     */
    public boolean canAccessOrder(Authentication authentication, String orderId) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new OrderAccessDeniedException(orderId);
        }

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        if (principal.hasRole("ADMIN")) {
            return true;
        }

        Order order = orderService.getOrderById(orderId);
        if (order == null || !order.getUserId().equals(principal.getUserId())) {
            throw new OrderNotFoundException(orderId);
        }

        return true;
    }
}
