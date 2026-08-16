package com.example.authservice.security;

import com.example.authservice.model.User;
import com.nimbusds.jose.jwk.RSAKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
public class JwtTokenProvider {

    private final JwtEncoder jwtEncoder;
    private final RSAKey rsaKey;
    private final String issuer;
    private final long jwtExpirationSeconds;

    public JwtTokenProvider(
            JwtEncoder jwtEncoder,
            RSAKey rsaKey,
            @Value("${jwt.issuer:http://localhost:8081}") String issuer,
            @Value("${jwt.expiration-seconds:3600}") long jwtExpirationSeconds) {
        this.jwtEncoder = jwtEncoder;
        this.rsaKey = rsaKey;
        this.issuer = issuer;
        this.jwtExpirationSeconds = jwtExpirationSeconds;
    }

    public String generateToken(User user) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plusSeconds(jwtExpirationSeconds);
        List<String> roles = user.getRoles().stream()
                .map(role -> role.getName())
                .toList();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .subject(user.getUsername())
                .claim("user_id", user.getId())
                .claim("email", user.getEmail())
                .claim("roles", roles)
                .claim("first_name", user.getFirstName())
                .claim("last_name", user.getLastName())
                .claim("provider", user.getProvider())
                .build();

        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256)
                .keyId(rsaKey.getKeyID())
                .build();

        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    public long getJwtExpirationSeconds() {
        return jwtExpirationSeconds;
    }

    public String getIssuer() {
        return issuer;
    }
}


