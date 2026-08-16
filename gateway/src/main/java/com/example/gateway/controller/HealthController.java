package com.example.gateway.controller;

/**
 * Health check được xử lý qua route gateway.
 * Không dùng @RestController để tránh xung đột với gateway RouterFunction.
 */
public class HealthController {
    private HealthController() {}
}
