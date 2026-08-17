package com.example.orderservice.security;

import java.util.Collections;
import java.util.List;

/**
 * Encapsulates the authenticated user's identity extracted from a validated JWT.
 */
public class UserPrincipal {

    private final String userId;
    private final String username;
    private final List<String> roles;

    public UserPrincipal(String userId, String username, List<String> roles) {
        this.userId = userId;
        this.username = username;
        this.roles = roles != null ? List.copyOf(roles) : Collections.emptyList();
    }

    public String getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public List<String> getRoles() {
        return roles;
    }

    public boolean hasRole(String role) {
        return roles.contains(role);
    }

    @Override
    public String toString() {
        return "UserPrincipal{userId='" + userId + "', username='" + username + "', roles=" + roles + "}";
    }
}

