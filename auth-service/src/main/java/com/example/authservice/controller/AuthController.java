package com.example.authservice.controller;

import com.example.authservice.model.AuthRequest;
import com.example.authservice.model.AuthResponse;
import com.example.authservice.model.ExternalLoginRequest;
import com.example.authservice.model.ExternalUserProfile;
import com.example.authservice.model.TokenIntrospectionRequest;
import com.example.authservice.model.User;
import com.example.authservice.model.UserRegistrationRequest;
import com.example.authservice.security.JwtTokenProvider;
import com.example.authservice.service.ExternalAuthStateService;
import com.example.authservice.service.ExternalIdentityService;
import com.example.authservice.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Locale;
import java.util.Optional;
import java.util.LinkedHashMap;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;
    private final ExternalIdentityService externalIdentityService;
    private final ExternalAuthStateService externalAuthStateService;
    private final JwtDecoder jwtDecoder;

    public AuthController(
            UserService userService,
            JwtTokenProvider jwtTokenProvider,
            ExternalIdentityService externalIdentityService,
            ExternalAuthStateService externalAuthStateService,
            JwtDecoder jwtDecoder) {
        this.userService = userService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.externalIdentityService = externalIdentityService;
        this.externalAuthStateService = externalAuthStateService;
        this.jwtDecoder = jwtDecoder;
    }

    @GetMapping("/external/{provider}/login-url")
    public ResponseEntity<?> getExternalLoginUrl(@PathVariable String provider) {
        String state = externalIdentityService.generateState();
        String codeVerifier = externalIdentityService.generateCodeVerifier();
        String codeChallenge = externalIdentityService.generateCodeChallenge(codeVerifier);
        externalAuthStateService.saveState(state, provider, codeVerifier);

        Optional<String> authorizationUrl = externalIdentityService.buildAuthorizationUrl(provider, state, codeChallenge);
        if (authorizationUrl.isEmpty()) {
            return new ResponseEntity<>("Unsupported provider or provider is not configured", HttpStatus.BAD_REQUEST);
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("provider", provider.toLowerCase(Locale.ROOT));
        response.put("authorizationUrl", authorizationUrl.get());
        response.put("state", state);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/external/{provider}/callback")
    public ResponseEntity<?> externalCallback(
            @PathVariable String provider,
            @RequestParam String code,
            @RequestParam String state) {
        Optional<String> codeVerifier = externalAuthStateService.consumeState(state, provider);
        if (codeVerifier.isEmpty()) {
            return new ResponseEntity<>("Invalid or expired state", HttpStatus.UNAUTHORIZED);
        }

        Optional<ExternalUserProfile> verifiedProfile = externalIdentityService.exchangeCodeForProfile(provider, code, codeVerifier.get());
        if (verifiedProfile.isEmpty()) {
            return new ResponseEntity<>("External identity verification failed", HttpStatus.UNAUTHORIZED);
        }

        ExternalUserProfile profile = verifiedProfile.get();
        User user = userService.createOrGetExternalUser(
                profile.getProvider().toLowerCase(Locale.ROOT),
                profile.getProviderId(),
                profile.getEmail(),
                profile.getFirstName(),
                profile.getLastName());

        return ResponseEntity.ok(buildResponse(user));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody AuthRequest authRequest) {
        if (!userService.authenticate(authRequest.getUsername(), authRequest.getPassword())) {
            return new ResponseEntity<>("Invalid credentials", HttpStatus.UNAUTHORIZED);
        }

        User user = userService.getUserByUsername(authRequest.getUsername());
        return ResponseEntity.ok(buildResponse(user));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody UserRegistrationRequest registrationRequest) {
        if (userService.getUserByUsername(registrationRequest.getUsername()) != null) {
            return new ResponseEntity<>("Username already exists", HttpStatus.BAD_REQUEST);
        }

        if (userService.getUserByEmail(registrationRequest.getEmail()) != null) {
            return new ResponseEntity<>("Email already exists", HttpStatus.BAD_REQUEST);
        }

        User newUser = User.builder()
                .username(registrationRequest.getUsername())
                .email(registrationRequest.getEmail())
                .password(registrationRequest.getPassword())
                .firstName(registrationRequest.getFirstName())
                .lastName(registrationRequest.getLastName())
                .build();

        User createdUser = userService.createUser(newUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(buildResponse(createdUser));
    }

    @PostMapping("/external/login")
    public ResponseEntity<?> externalLogin(@Valid @RequestBody ExternalLoginRequest request) {
        Optional<ExternalUserProfile> verifiedProfile = externalIdentityService.verify(
                request.getProvider(),
                request.getAccessToken());

        if (verifiedProfile.isEmpty()) {
            return new ResponseEntity<>("External identity verification failed", HttpStatus.UNAUTHORIZED);
        }

        ExternalUserProfile profile = verifiedProfile.get();
        User user = userService.createOrGetExternalUser(
                profile.getProvider().toLowerCase(Locale.ROOT),
                profile.getProviderId(),
                profile.getEmail(),
                profile.getFirstName(),
                profile.getLastName());

        return ResponseEntity.ok(buildResponse(user));
    }

    @PostMapping("/introspect")
    public ResponseEntity<?> introspect(@Valid @RequestBody TokenIntrospectionRequest request) {
        try {
            Jwt jwt = jwtDecoder.decode(request.getToken());
            return ResponseEntity.ok(Map.of(
                    "active", true,
                    "sub", jwt.getSubject(),
                    "user_id", jwt.getClaimAsString("user_id"),
                    "roles", jwt.getClaimAsStringList("roles"),
                    "issuer", jwt.getIssuer() != null ? jwt.getIssuer().toString() : null,
                    "expires_at", jwt.getExpiresAt()
            ));
        } catch (JwtException exception) {
            return new ResponseEntity<>(Map.of("active", false), HttpStatus.UNAUTHORIZED);
        }
    }

    private AuthResponse buildResponse(User user) {
        return AuthResponse.builder()
                .accessToken(jwtTokenProvider.generateToken(user))
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getJwtExpirationSeconds())
                .username(user.getUsername())
                .email(user.getEmail())
                .build();
    }
}




