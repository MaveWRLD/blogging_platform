package org.amalitech.user;

import org.amalitech.common.security.AuthenticatedUserProvider;
import org.springframework.stereotype.Service;

/**
 * User-feature authorization checks, split out of the old cross-feature
 * AuthorizationService. Performance-related checks stay on
 * org.amalitech.service.AuthorizationService until Performance gets its
 * own package-by-feature pass.
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
