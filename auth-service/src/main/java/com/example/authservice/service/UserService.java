package com.example.authservice.service;

import com.example.authservice.model.Role;
import com.example.authservice.model.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UserService {

    private static final Role USER_ROLE = Role.builder()
            .id("1")
            .name("USER")
            .description("Standard user role")
            .build();

    private static final Role ADMIN_ROLE = Role.builder()
            .id("2")
            .name("ADMIN")
            .description("Administrator role")
            .build();

    private final Map<String, User> users = new ConcurrentHashMap<>();
    private final PasswordEncoder passwordEncoder;

    public UserService(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
        initializeDefaultUsers();
    }

    private void initializeDefaultUsers() {
        User user1 = User.builder()
                .id("1")
                .username("user")
                .email("user@example.com")
                .password(passwordEncoder.encode("password123"))
                .firstName("John")
                .lastName("Doe")
                .enabled(true)
                .roles(new HashSet<>(Collections.singletonList(USER_ROLE)))
                .provider("internal")
                .build();

        User admin = User.builder()
                .id("2")
                .username("admin")
                .email("admin@example.com")
                .password(passwordEncoder.encode("admin123"))
                .firstName("Admin")
                .lastName("User")
                .enabled(true)
                .roles(new HashSet<>(Arrays.asList(USER_ROLE, ADMIN_ROLE)))
                .provider("internal")
                .build();

        users.put("user", user1);
        users.put("admin", admin);
    }

    public User getUserByUsername(String username) {
        return users.get(username);
    }

    public User getUserById(String id) {
        return users.values().stream()
                .filter(u -> u.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    public User getUserByEmail(String email) {
        return users.values().stream()
                .filter(u -> email.equalsIgnoreCase(u.getEmail()))
                .findFirst()
                .orElse(null);
    }

    public User getUserByProviderAndProviderId(String provider, String providerId) {
        return users.values().stream()
                .filter(u -> provider.equalsIgnoreCase(u.getProvider()) && providerId.equals(u.getProviderId()))
                .findFirst()
                .orElse(null);
    }

    public User createUser(User user) {
        if (users.containsKey(user.getUsername())) {
            throw new IllegalArgumentException("User already exists");
        }
        if (getUserByEmail(user.getEmail()) != null) {
            throw new IllegalArgumentException("Email already exists");
        }
        if (user.getId() == null) {
            user.setId(UUID.randomUUID().toString());
        }
        user.setEnabled(true);
        user.setProvider(user.getProvider() == null ? "internal" : user.getProvider());
        if (user.getRoles() == null) {
            user.setRoles(new HashSet<>(Collections.singletonList(USER_ROLE)));
        }
        if (user.getPassword() != null && !user.getPassword().startsWith("$2a$") && !user.getPassword().startsWith("$2b$") && !user.getPassword().startsWith("$2y$")) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        users.put(user.getUsername(), user);
        return user;
    }

    public User createOrGetExternalUser(String provider, String providerId, String email, String firstName, String lastName) {
        User existingByProvider = getUserByProviderAndProviderId(provider, providerId);
        if (existingByProvider != null) {
            return existingByProvider;
        }

        User existingByEmail = email == null ? null : getUserByEmail(email);
        if (existingByEmail != null) {
            existingByEmail.setProvider(provider);
            existingByEmail.setProviderId(providerId);
            return updateUser(existingByEmail);
        }

        String generatedUsername = buildUniqueUsername(provider, email, providerId);
        User externalUser = User.builder()
                .username(generatedUsername)
                .email(email == null ? generatedUsername + "@external.local" : email)
                .password(null)
                .firstName(firstName)
                .lastName(lastName)
                .provider(provider)
                .providerId(providerId)
                .enabled(true)
                .roles(new HashSet<>(Collections.singletonList(USER_ROLE)))
                .build();

        return createUser(externalUser);
    }

    public User updateUser(User user) {
        if (!users.containsKey(user.getUsername())) {
            throw new IllegalArgumentException("User not found");
        }
        users.put(user.getUsername(), user);
        return user;
    }

    public boolean authenticate(String username, String password) {
        User user = users.get(username);
        return user != null
                && user.isEnabled()
                && user.getPassword() != null
                && passwordEncoder.matches(password, user.getPassword());
    }

    public List<User> getAllUsers() {
        return new ArrayList<>(users.values());
    }

    private String buildUniqueUsername(String provider, String email, String providerId) {
        String base = email != null && email.contains("@")
                ? email.substring(0, email.indexOf('@'))
                : provider + "_" + providerId;
        String normalized = base.replaceAll("[^a-zA-Z0-9._-]", "_").toLowerCase(Locale.ROOT);
        String candidate = normalized;
        int suffix = 1;
        while (users.containsKey(candidate)) {
            candidate = normalized + suffix++;
        }
        return candidate;
    }
}


