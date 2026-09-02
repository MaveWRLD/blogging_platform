package org.amalitech.service;

import org.amalitech.common.security.AuthenticatedUserProvider;
import org.springframework.stereotype.Service;

/**
 * Authorization checks for features not yet migrated to package-by-feature
 * (User, Performance). Post-specific and Comment-specific checks moved to
 * org.amalitech.post.PostAuthorizationService and
 * org.amalitech.comment.CommentAuthorizationService respectively.
 */
@Service
public class AuthorizationService {

    private final AuthenticatedUserProvider authenticatedUserProvider;

    public AuthorizationService(AuthenticatedUserProvider authenticatedUserProvider) {
        this.authenticatedUserProvider = authenticatedUserProvider;
    }

    public boolean canAccessUser(Long targetUserId) {
        Long requesterId = authenticatedUserProvider.getRequesterId();
        if (requesterId == null) return false;

        if (requesterId.equals(targetUserId)) return true;

        return authenticatedUserProvider.hasAdminRole();
    }

    public boolean canPromoteUser() {
        return authenticatedUserProvider.hasAdminRole();
    }

    public boolean canAccessPerformanceMetrics() {
        return authenticatedUserProvider.hasAdminRole();
    }
}
