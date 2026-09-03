package org.amalitech.user;

import org.amalitech.common.security.AuthenticatedUserProvider;
import org.springframework.stereotype.Service;

/**
 * User-feature authorization checks, split out of the old cross-feature
 * AuthorizationService (now deleted - its last remaining method,
 * canAccessPerformanceMetrics, was dead code; PerformanceApi enforces
 * access with a plain hasRole('admin') check).
 */
@Service
public class UserAuthorizationService {

    private final AuthenticatedUserProvider authenticatedUserProvider;

    public UserAuthorizationService(AuthenticatedUserProvider authenticatedUserProvider) {
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
}
