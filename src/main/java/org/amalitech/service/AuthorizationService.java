package org.amalitech.service;

import org.amalitech.common.security.AuthenticatedUserProvider;
import org.springframework.stereotype.Service;

/**
 * Authorization checks for features not yet migrated to package-by-feature
 * (Performance). Post-specific, Comment-specific, and User-specific checks
 * moved to org.amalitech.post.PostAuthorizationService,
 * org.amalitech.comment.CommentAuthorizationService, and
 * org.amalitech.user.UserAuthorizationService respectively.
 */
@Service
public class AuthorizationService {

    private final AuthenticatedUserProvider authenticatedUserProvider;

    public AuthorizationService(AuthenticatedUserProvider authenticatedUserProvider) {
        this.authenticatedUserProvider = authenticatedUserProvider;
    }

    public boolean canAccessPerformanceMetrics() {
        return authenticatedUserProvider.hasAdminRole();
    }
}
