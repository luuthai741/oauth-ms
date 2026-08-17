package com.example.authservice.service;

import com.example.authservice.model.ExternalUserProfile;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.JWKSourceBuilder;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class ExternalIdentityService {

    private final RestClient restClient;
    private final String externalBaseUrl;
    private final String googleClientId;
    private final String googleClientSecret;

    public ExternalIdentityService(
            RestClient restClient,
            @Value("${auth.external.base-url:}") String externalBaseUrl,
            @Value("${auth.external.google.client-id:}") String googleClientId,
            @Value("${auth.external.google.client-secret:}") String googleClientSecret) {
        this.restClient = restClient;
        this.externalBaseUrl = externalBaseUrl;
        this.googleClientId = googleClientId;
        this.googleClientSecret = googleClientSecret;
    }

    public Optional<String> buildAuthorizationUrl(String provider, String state, String codeChallenge, String nonce) {
        return registration(provider).map(registration -> {
            UriComponentsBuilder builder = UriComponentsBuilder
                    .fromUriString(registration.authorizationUri())
                    .queryParam("client_id", registration.clientId())
                    .queryParam("redirect_uri", callbackUri(registration.provider()))
                    .queryParam("response_type", "code")
                    .queryParam("scope", registration.scope())
                    .queryParam("state", state);

            if (nonce != null && !nonce.isBlank()) {
                builder.queryParam("nonce", nonce);
            }

            if (registration.pkceEnabled()) {
                builder.queryParam("code_challenge", codeChallenge)
                        .queryParam("code_challenge_method", "S256");
            }

            return builder.encode().build().toUriString();
        });
    }

    public String generateState() {
        return UUID.randomUUID().toString();
    }

    public String generateNonce() {
        return UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
    }

    public String generateCodeVerifier() {
        String seed = UUID.randomUUID() + UUID.randomUUID().toString().replace("-", "");
        return seed.replace("-", "");
    }

    public String generateCodeChallenge(String codeVerifier) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(codeVerifier.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hashed);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to generate PKCE code challenge", exception);
        }
    }

    public Optional<ExternalUserProfile> exchangeCodeForProfile(String provider, String code, String codeVerifier, String expectedNonce) {
        Optional<Registration> registration = registration(provider);
        if (registration.isEmpty()) {
            return Optional.empty();
        }

        try {
            Map<String, Object> tokenPayload = exchangeCodeForTokenPayload(registration.get(), code, codeVerifier);
            if (tokenPayload == null) {
                return Optional.empty();
            }

            // Ưu tiên: nếu có id_token → verify bằng JWKS của provider
            String idToken = stringValue(tokenPayload.get("id_token"));
            if (idToken != null && !idToken.isBlank()) {
                Optional<ExternalUserProfile> profileFromIdToken =
                        extractProfileFromIdToken(registration.get(), idToken, expectedNonce);
                if (profileFromIdToken.isPresent()) {
                    return profileFromIdToken;
                }
            }

            // Fallback: dùng access_token gọi userinfo endpoint
            String accessToken = stringValue(tokenPayload.get("access_token"));
            if (accessToken == null || accessToken.isBlank()) {
                return Optional.empty();
            }
            return verify(registration.get().provider(), accessToken);
        } catch (RestClientException exception) {
            return Optional.empty();
        }
    }

    private Optional<ExternalUserProfile> extractProfileFromIdToken(Registration registration, String idToken, String expectedNonce) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(idToken);
            JWSAlgorithm tokenAlgorithm = signedJWT.getHeader().getAlgorithm();
            if (tokenAlgorithm == null || !registration.supportedJwsAlgorithms().contains(tokenAlgorithm)) {
                return Optional.empty();
            }

            DefaultJWTProcessor<SecurityContext> processor = new DefaultJWTProcessor<>();
            var jwkSource = JWKSourceBuilder
                    .create(new URL(registration.jwksUri()))
                    .build();
            processor.setJWSKeySelector(
                    new JWSVerificationKeySelector<>(tokenAlgorithm, jwkSource));

            JWTClaimsSet claims = processor.process(signedJWT, null);

            // Validate issuer
            if (!registration.issuer().equals(claims.getIssuer())) {
                return Optional.empty();
            }

            // Validate audience
            List<String> audience = claims.getAudience();
            if (audience == null || !audience.contains(registration.clientId())) {
                return Optional.empty();
            }

            // Validate expiry
            if (claims.getExpirationTime() == null ||
                    claims.getExpirationTime().toInstant().isBefore(Instant.now())) {
                return Optional.empty();
            }

            // Validate nonce when provided in authorization request
            if (expectedNonce != null && !expectedNonce.isBlank()) {
                String nonceClaim = stringValue(claims.getClaim("nonce"));
                if (nonceClaim == null || !expectedNonce.equals(nonceClaim)) {
                    return Optional.empty();
                }
            }

            return Optional.of(ExternalUserProfile.builder()
                    .provider(registration.provider())
                    .providerId(claims.getSubject())
                    .email(stringValue(claims.getStringClaim("email")))
                    .firstName(stringValue(claims.getStringClaim("given_name")))
                    .lastName(stringValue(claims.getStringClaim("family_name")))
                    .build());
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public Optional<ExternalUserProfile> verify(String provider, String accessToken) {
        return switch (provider.toLowerCase(Locale.ROOT)) {
            case "google" -> verifyGoogle(accessToken);
            default -> Optional.empty();
        };
    }

    @SuppressWarnings("unchecked")
    private Optional<ExternalUserProfile> verifyGoogle(String accessToken) {
        try {
            Map<String, Object> payload = restClient.get()
                    .uri("https://openidconnect.googleapis.com/v1/userinfo")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(Map.class);
            if (payload == null) {
                return Optional.empty();
            }
            return Optional.of(mapGoogleUser(payload));
        } catch (RestClientException exception) {
            return Optional.empty();
        }
    }

    private ExternalUserProfile mapGoogleUser(Map<String, Object> payload) {
        return ExternalUserProfile.builder()
                .provider("google")
                .providerId(stringValue(payload.get("sub")))
                .email(stringValue(payload.get("email")))
                .firstName(stringValue(payload.get("given_name")))
                .lastName(stringValue(payload.get("family_name")))
                .build();
    }

    private String stringValue(Object value) {
        return value == null ? null : value.toString();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> exchangeCodeForTokenPayload(Registration registration, String code, String codeVerifier) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("code", code);
        body.add("redirect_uri", callbackUri(registration.provider()));
        body.add("client_id", registration.clientId());
        body.add("client_secret", registration.clientSecret());
        if (registration.pkceEnabled()) {
            body.add("code_verifier", codeVerifier);
        }

        return restClient.post()
                .uri(registration.tokenUri())
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .accept(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(Map.class);
    }

    private Optional<Registration> registration(String provider) {
        if (provider == null) {
            return Optional.empty();
        }

        return switch (provider.toLowerCase(Locale.ROOT)) {
            case "google" -> registrationIfConfigured(new Registration(
                    "google",
                    googleClientId,
                    googleClientSecret,
                    "https://accounts.google.com/o/oauth2/v2/auth",
                    "https://oauth2.googleapis.com/token",
                    "https://www.googleapis.com/oauth2/v3/certs",
                    "https://accounts.google.com",
                    "openid email profile",
                    List.of(JWSAlgorithm.RS256),
                    true));
            default -> Optional.empty();
        };
    }

    private Optional<Registration> registrationIfConfigured(Registration registration) {
        if (registration.clientId() == null || registration.clientId().isBlank()) {
            return Optional.empty();
        }
        if (registration.clientSecret() == null || registration.clientSecret().isBlank()) {
            return Optional.empty();
        }
        return Optional.of(registration);
    }

    private String callbackUri(String provider) {
        return externalBaseUrl + "/oauth/callback/" + provider;
    }

    private record Registration(
            String provider,
            String clientId,
            String clientSecret,
            String authorizationUri,
            String tokenUri,
            String jwksUri,
            String issuer,
            String scope,
            List<JWSAlgorithm> supportedJwsAlgorithms,
            boolean pkceEnabled) {
    }
}

