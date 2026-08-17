package com.example.orderservice.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Converts a validated {@link Jwt} into an {@link AbstractAuthenticationToken}
 * whose principal is a {@link UserPrincipal} — instead of the raw JWT object.
 *
 * <p>Claims expected in the JWT:
 * <ul>
 *   <li>{@code sub}     — username (subject)</li>
 *   <li>{@code user_id} — internal user ID</li>
 *   <li>{@code roles}   — list of role strings, e.g. ["USER", "ADMIN"]</li>
 * </ul>
 */
public class JwtUserAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private static final String ROLES_CLAIM = "roles";
    private static final String USER_ID_CLAIM = "user_id";
    private static final String ROLE_PREFIX = "ROLE_";

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        String username = jwt.getSubject();
        String userId = jwt.getClaimAsString(USER_ID_CLAIM);
        List<String> roles = resolveRoles(jwt);

        UserPrincipal principal = new UserPrincipal(userId, username, roles);
        Collection<GrantedAuthority> authorities = buildAuthorities(roles);

        return new UsernamePasswordAuthenticationToken(principal, jwt, authorities);
    }

    private List<String> resolveRoles(Jwt jwt) {
        List<String> roles = jwt.getClaimAsStringList(ROLES_CLAIM);
        return roles != null ? roles : Collections.emptyList();
    }

    private Collection<GrantedAuthority> buildAuthorities(List<String> roles) {
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority(ROLE_PREFIX + role))
                .collect(Collectors.toList());
    }
}

