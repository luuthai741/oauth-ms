package com.example.authservice.service;

import com.example.authservice.model.ExternalUserProfile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.Base64;

@Service
public class ExternalIdentityService {

    private final RestClient restClient;
    private final String externalBaseUrl;
    private final String googleClientId;
    private final String googleClientSecret;

    public ExternalIdentityService(
            RestClient restClient,
            Environment environment,
            @Value("${auth.external.base-url:http://localhost:8080}") String externalBaseUrl,
            @Value("${auth.external.google.client-id:}") String googleClientId,
            @Value("${auth.external.google.client-secret:}") String googleClientSecret) {
        System.out.println("ENV GOOGLE_CLIENT_ID = " +
                System.getenv("GOOGLE_CLIENT_ID"));
        this.restClient = restClient;
        this.externalBaseUrl = externalBaseUrl;
        this.googleClientId = googleClientId;
        this.googleClientSecret = googleClientSecret;
    }

    public Optional<String> buildAuthorizationUrl(String provider, String state, String codeChallenge) {
        return registration(provider).map(registration -> {
            UriComponentsBuilder builder = UriComponentsBuilder
                    .fromUriString(registration.authorizationUri())
                    .queryParam("client_id", registration.clientId())
                    .queryParam("redirect_uri", callbackUri(registration.provider()))
                    .queryParam("response_type", "code")
                    .queryParam("scope", registration.scope())
                    .queryParam("state", state);

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

    public Optional<ExternalUserProfile> exchangeCodeForProfile(String provider, String code, String codeVerifier) {
        Optional<Registration> registration = registration(provider);
        if (registration.isEmpty()) {
            return Optional.empty();
        }

        try {
            String accessToken = exchangeCodeForAccessToken(registration.get(), code, codeVerifier);
            if (accessToken == null || accessToken.isBlank()) {
                return Optional.empty();
            }
            return verify(registration.get().provider(), accessToken);
        } catch (RestClientException exception) {
            return Optional.empty();
        }
    }

    public Optional<ExternalUserProfile> verify(String provider, String accessToken) {
        return switch (provider.toLowerCase(Locale.ROOT)) {
            case "google" -> verifyGoogle(accessToken);
            default -> Optional.empty();
        };
    }

    private Optional<ExternalUserProfile> verifyGoogle(String accessToken) {
        try {
            Map<String, Object> payload = restClient.get()
                    .uri("https://openidconnect.googleapis.com/v1/userinfo")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(Map.class);
            return Optional.of(mapGoogle(payload));
        } catch (RestClientException exception) {
            return Optional.empty();
        }
    }

    private ExternalUserProfile mapGoogle(Map<String, Object> payload) {
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

    private String exchangeCodeForAccessToken(Registration registration, String code, String codeVerifier) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("code", code);
        body.add("redirect_uri", callbackUri(registration.provider()));
        body.add("client_id", registration.clientId());
        body.add("client_secret", registration.clientSecret());
        if (registration.pkceEnabled()) {
            body.add("code_verifier", codeVerifier);
        }

        Map<String, Object> tokenPayload = restClient.post()
                .uri(registration.tokenUri())
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .accept(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(Map.class);

        return tokenPayload == null ? null : stringValue(tokenPayload.get("access_token"));
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
                    "openid email profile",
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
        return externalBaseUrl + "/auth/external/" + provider + "/callback";
    }

    private record Registration(
            String provider,
            String clientId,
            String clientSecret,
            String authorizationUri,
            String tokenUri,
            String scope,
            boolean pkceEnabled) {
    }
}

