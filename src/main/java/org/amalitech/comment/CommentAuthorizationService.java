package org.amalitech.comment;

import org.amalitech.common.security.AuthenticatedUserProvider;
import org.amalitech.repositories.UserRepository;
import org.springframework.stereotype.Service;

/**
 * Comment-feature authorization checks, split out of the old cross-feature
 * AuthorizationService so Comment owns its own CommentRepository access.
 *
 * Note: this still reaches into UserRepository directly to resolve the
 * requester's username (same pre-existing pattern as CommentService). That
 * coupling isn't fixed in this pilot - it needs the User feature to expose
 * its own public port, which is out of scope until User gets its own
 * package-by-feature pass.
 */
@Service
public class CommentAuthorizationService {

    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    public CommentAuthorizationService(CommentRepository commentRepository, UserRepository userRepository, AuthenticatedUserProvider authenticatedUserProvider) {
        this.commentRepository = commentRepository;
        this.userRepository = userRepository;
        this.authenticatedUserProvider = authenticatedUserProvider;
    }

    public boolean canUpdateComment(String commentId) {
        Long requesterId = authenticatedUserProvider.getRequesterId();
        if (requesterId == null) return false;

        return commentRepository.existsByIdAndUsername(commentId, getUsername(requesterId));
    }

    public boolean canDeleteComment(String commentId) {
        Long requesterId = authenticatedUserProvider.getRequesterId();
        if (requesterId == null) return false;

        if (authenticatedUserProvider.hasAdminRole()) return true;

        return commentRepository.existsByIdAndUsername(commentId, getUsername(requesterId));
    }

    private String getUsername(Long requesterId) {
        var user = userRepository.findById(requesterId).orElseThrow(
                () -> new RuntimeException("User not found")
        );

        return user.getUsername();
    }
}
