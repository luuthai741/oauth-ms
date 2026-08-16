package com.example.orderservice.security;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class JwtValidator {

    private final JwtDecoder jwtDecoder;

    public JwtValidator(JwtDecoder jwtDecoder) {
        this.jwtDecoder = jwtDecoder;
    }

    public String extractUsername(String token) {
        return decode(token).getSubject();
    }

    public boolean validateToken(String token) {
        try {
            decode(token);
            return true;
        } catch (JwtException exception) {
            return false;
        }
    }

    public Set<String> extractRoles(String token) {
        List<String> roles = decode(token).getClaimAsStringList("roles");
        return roles == null ? new HashSet<>() : new HashSet<>(roles);
    }

    public Map<String, Object> getAllClaims(String token) {
        return new HashMap<>(decode(token).getClaims());
    }

    private Jwt decode(String token) {
        return jwtDecoder.decode(token);
    }
}



