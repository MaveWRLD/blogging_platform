package org.amalitech.post;

import org.amalitech.common.security.AuthenticatedUserProvider;
import org.springframework.stereotype.Service;

/**
 * Post-feature authorization checks, split out of the old cross-feature
 * AuthorizationService so Post owns its own PostRepository access instead
 * of that repository being reached into from outside the package.
 */
@Service
public class PostAuthorizationService {

    private final PostRepository postRepository;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    public PostAuthorizationService(PostRepository postRepository, AuthenticatedUserProvider authenticatedUserProvider) {
        this.postRepository = postRepository;
        this.authenticatedUserProvider = authenticatedUserProvider;
    }

    public boolean canUpdatePost(int postId) {
        return postRepository.existsByIdAndUser_Id(postId, authenticatedUserProvider.getRequesterId());
    }

    public boolean canDeletePost(Integer postId) {
        Long requesterId = authenticatedUserProvider.getRequesterId();
        if (requesterId == null) return false;

        if (authenticatedUserProvider.hasAdminRole()) return true;

        return postRepository.existsByIdAndUser_Id(postId, requesterId);
    }
}
