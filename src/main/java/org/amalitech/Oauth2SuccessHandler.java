package org.amalitech;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.amalitech.entities.Role;
import org.amalitech.entities.User;
import org.amalitech.repositories.UserRepository;
import org.amalitech.service.RoleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizationSuccessHandler;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;

@Component
@AllArgsConstructor
public class Oauth2SuccessHandler implements AuthenticationSuccessHandler, OAuth2AuthorizationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(Oauth2SuccessHandler.class);

    private final UserRepository userRepository;
    private final RoleService roleService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        Object userPrincipal = authentication.getPrincipal();
        if (!(userPrincipal instanceof OAuth2User oAuth2User)) {
            log.warn("Principal is not an OAuth2User: {}", userPrincipal.getClass().getSimpleName());
            return;
        }

        Map<String, Object> attrs = oAuth2User.getAttributes();

        String email = extractEmail(attrs);
        if (email == null || email.isBlank()) {
            log.error("Could not extract email from OAuth2 attributes");
            return;
        }

        String username = deriveUsername(attrs, email);

        Set<Role> defaultRoles = getDefaultRoles();

        persistOAuth2User(username, email, defaultRoles, attrs);
    }

    @Override
    public void onAuthorizationSuccess(OAuth2AuthorizedClient authorizedClient, Authentication principal, Map<String, Object> attributes) {
    }

    /**
     * Extract email from OAuth2 attributes, trying multiple common attribute names
     */
    private String extractEmail(Map<String, Object> attrs) {
        String email = extractAttribute(attrs, "email");
        if (!email.isBlank()) {
            return email;
        }

        Object emailsObj = attrs.get("emails");
        if (emailsObj instanceof Iterable) {
            for (Object e : (Iterable<?>) emailsObj) {
                if (e != null) {
                    String emailStr = e.toString();
                    if (emailStr.contains("@")) {
                        return emailStr;
                    }
                }
            }
        }

        return null;
    }

    /**
     * Derive username from OAuth2 attributes, trying multiple common attribute names
     */
    private String deriveUsername(Map<String, Object> attrs, String email) {
        String username = extractAttribute(attrs, "username");
        if (!username.isBlank()) {
            return username.replace(" ", "_").toLowerCase();
        }

        username = extractAttribute(attrs, "login");
        if (!username.isBlank()) {
            return username.replace(" ", "_").toLowerCase();
        }

        username = extractAttribute(attrs, "name");
        if (!username.isBlank()) {
            return username.replace(" ", "_").toLowerCase();
        }

        username = extractAttribute(attrs, "sub");
        if (!username.isBlank()) {
            return username.replace(" ", "_").toLowerCase();
        }

        if (email != null && email.contains("@")) {
            return email.substring(0, email.indexOf("@")).toLowerCase();
        }

        return "oauth-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private String extractAttribute(Map<String, Object> attrs, String key) {
        Object value = attrs.get(key);
        return value != null ? value.toString() : "";
    }

    /**
     * Get default roles for new OAuth2 users
     */
    private Set<Role> getDefaultRoles() {
        Set<Role> roles = new HashSet<>();
        try {
            Role readerRole = roleService.getRoleByName("READER");
            if (readerRole != null) {
                roles.add(readerRole);
            }
        } catch (RuntimeException e) {
            log.warn("Unable to fetch READER role: {}", e.getMessage());
        }
        return roles;
    }

    /**
     * Persist a new OAuth2 user to the database
     */
    private void persistOAuth2User(String username, String email, Set<Role> defaultRoles, Map<String, Object> attrs) {
        try {
            String firstName = extractAttribute(attrs, "given_name");
            String lastName = extractAttribute(attrs, "family_name");

            User newUser = User.registerReader(
                    username,
                    email,
                    null,
                    firstName.isBlank() ? null : firstName,
                    lastName.isBlank() ? null : lastName,
                    defaultRoles
            );

            User savedUser = userRepository.save(newUser);
            log.info("Created and persisted new OAuth2 user: {} ({})", savedUser.getUsername(), savedUser.getEmail());
        } catch (Exception e) {
            log.error("Failed to persist OAuth2 user with email {}: {}", email, e.getMessage(), e);
        }
    }
}
