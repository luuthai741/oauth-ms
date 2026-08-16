package com.example.authservice.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class JwksProvider {

    private final RSAKey rsaKey;

    public JwksProvider(RSAKey rsaKey) {
        this.rsaKey = rsaKey;
    }

    public Map<String, Object> getJwkSet() {
        return new JWKSet(rsaKey.toPublicJWK()).toJSONObject();
    }
}


