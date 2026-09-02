package org.amalitech.common.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Shared helper for reading the current authenticated user out of the
 * security context. Extracted so feature-specific authorization services
 * (e.g. PostAuthorizationService, CommentAuthorizationService) don't each
 * duplicate this logic.
 */
@Component
public class AuthenticatedUserProvider {

    public Long getRequesterId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || auth.getPrincipal() == null) {
            return null;
        }

        return (Long) auth.getPrincipal();
    }

    public boolean hasAdminRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            return auth.getAuthorities().stream()
                    .anyMatch(a -> "ROLE_ADMIN".equalsIgnoreCase(a.getAuthority()));
        }
        return false;
    }
}
