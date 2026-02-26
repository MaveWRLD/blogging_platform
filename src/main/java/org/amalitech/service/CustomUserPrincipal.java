package org.amalitech.service;

import lombok.Getter;
import org.amalitech.entities.Role;
import org.amalitech.entities.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.io.Serializable;
import java.util.*;

@Getter
public class CustomUserPrincipal implements OAuth2User, UserDetails, Serializable {
    private final User user;
    private final Map<String, Object> attributes;

    public CustomUserPrincipal(User user, Map<String, Object> attributes) {
        this.user = Objects.requireNonNull(user, "user must not be null");
        this.attributes = attributes == null ? Collections.emptyMap() : attributes;
    }

    @Override
    public Map<String, Object> getAttributes() { return Collections.unmodifiableMap(attributes); }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (user.getRoles() == null || user.getRoles().isEmpty()) return Collections.emptyList();

        return user.getRoles().stream()
                .map(Role::getName)
                .filter(Objects::nonNull)
                .map(r -> r.startsWith("ROLE_") ? r : "ROLE_" + r)
                .map(SimpleGrantedAuthority::new)
                .toList();
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() { return user.getUsername(); }

    @Override public boolean isAccountNonExpired() { return true;}
    @Override public boolean isAccountNonLocked() { return true;}
    @Override public boolean isCredentialsNonExpired() { return true;}
    @Override public boolean isEnabled() { return true;}

    @Override
    public String getName() {
        Object sub = attributes.get("sub");
        return sub != null ? sub.toString()
                : (user.getId() != null ? user.getId().toString() : user.getEmail());
    }
}
