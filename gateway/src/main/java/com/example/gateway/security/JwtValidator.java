package com.example.gateway.security;

/**
 * JWT validation đã được chuyển hoàn toàn sang downstream services (order-service).
 * Gateway chỉ forward request, không validate token.
 */
public class JwtValidator {
    private JwtValidator() {}
}
