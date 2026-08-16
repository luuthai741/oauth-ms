package com.example.authservice.controller;

import com.example.authservice.config.JwksProvider;
import com.example.authservice.security.JwtTokenProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/.well-known")
public class JwksController {

    private final JwksProvider jwksProvider;
    private final JwtTokenProvider jwtTokenProvider;

    public JwksController(JwksProvider jwksProvider, JwtTokenProvider jwtTokenProvider) {
        this.jwksProvider = jwksProvider;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @GetMapping("/jwks.json")
    public ResponseEntity<Map<String, Object>> getJwks() {
        return ResponseEntity.ok(jwksProvider.getJwkSet());
    }

    @GetMapping("/openid-configuration")
    public ResponseEntity<Map<String, Object>> openIdConfiguration() {
        return ResponseEntity.ok(Map.of(
                "issuer", jwtTokenProvider.getIssuer(),
                "jwks_uri", jwtTokenProvider.getIssuer() + "/.well-known/jwks.json",
                "token_endpoint", jwtTokenProvider.getIssuer() + "/auth/login",
                "registration_endpoint", jwtTokenProvider.getIssuer() + "/auth/register"
        ));
    }
}


