package com.example.gateway.filter;

/**
 * JWT validation không thực hiện ở gateway layer.
 * Downstream services (order-service) tự validate JWT qua JWKS từ auth-service.
 */
public class JwtValidationFilter {
    private JwtValidationFilter() {}
}
