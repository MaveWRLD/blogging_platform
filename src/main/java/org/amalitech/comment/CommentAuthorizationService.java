package org.amalitech.comment;

import org.amalitech.common.security.AuthenticatedUserProvider;
import org.amalitech.user.UserService;
import org.springframework.stereotype.Service;

/**
 * Comment-feature authorization checks, split out of the old cross-feature
 * AuthorizationService so Comment owns its own CommentRepository access.
 * Resolves the requester's username through User's public UserService
 * instead of reaching into UserRepository directly.
 */
@Service
public class CommentAuthorizationService {

    private final CommentRepository commentRepository;
    private final UserService userService;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    public CommentAuthorizationService(CommentRepository commentRepository, UserService userService, AuthenticatedUserProvider authenticatedUserProvider) {
        this.commentRepository = commentRepository;
        this.userService = userService;
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
        return userService.findByUserId(requesterId).getUsername();
    }
}
