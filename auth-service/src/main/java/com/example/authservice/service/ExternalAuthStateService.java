package com.example.authservice.service;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ExternalAuthStateService {

    private static final long STATE_TTL_SECONDS = 300;
    private final Map<String, StateEntry> states = new ConcurrentHashMap<>();

    public void saveState(String state, String provider, String codeVerifier, String nonce) {
        states.put(state, new StateEntry(provider, codeVerifier, nonce, Instant.now().plusSeconds(STATE_TTL_SECONDS)));
    }

    public Optional<ConsumedState> consumeState(String state, String provider) {
        if (state == null || state.isBlank() || provider == null || provider.isBlank()) {
            return Optional.empty();
        }
        if (!states.containsKey(state)) {
            return Optional.empty();
        }

        StateEntry entry = states.remove(state);
        if (entry == null) {
            return Optional.empty();
        }
        if (!entry.provider().equalsIgnoreCase(provider)) {
            return Optional.empty();
        }
        if (!Instant.now().isBefore(entry.expiresAt())) {
            return Optional.empty();
        }
        return Optional.of(new ConsumedState(entry.codeVerifier(), entry.nonce()));
    }

    public record ConsumedState(String codeVerifier, String nonce) {
    }

    private record StateEntry(String provider, String codeVerifier, String nonce, Instant expiresAt) {
    }
}

