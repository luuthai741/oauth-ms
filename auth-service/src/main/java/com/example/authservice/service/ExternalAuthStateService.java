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

    public void saveState(String state, String provider, String codeVerifier) {
        states.put(state, new StateEntry(provider, codeVerifier, Instant.now().plusSeconds(STATE_TTL_SECONDS)));
    }

    public Optional<String> consumeState(String state, String provider) {
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
        return Optional.of(entry.codeVerifier());
    }

    private record StateEntry(String provider, String codeVerifier, Instant expiresAt) {
    }
}

