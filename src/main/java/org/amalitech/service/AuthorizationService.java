package org.amalitech.service;

import org.amalitech.repositories.PostRepository;
import org.amalitech.repositories.CommentRepository;
import org.amalitech.repositories.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class AuthorizationService {

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;

    public AuthorizationService(PostRepository postRepository, CommentRepository commentRepository, UserRepository userRepository) {
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.userRepository = userRepository;
    }

    public Long getRequesterId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || auth.getPrincipal() == null) {
            return null;
        }

        return (Long) auth.getPrincipal();
    }

    public boolean canUpdatePost(int postId) {
        return postRepository.existsByIdAndUser_Id(postId, getRequesterId());
    }

    public boolean canAccessUser(Long targetUserId) {
        Long requesterId = getRequesterId();
        if (requesterId == null) return false;

        if (requesterId.equals(targetUserId)) return true;

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            return auth.getAuthorities().stream()
                    .anyMatch(a -> "ROLE_ADMIN".equalsIgnoreCase(a.getAuthority()));
        }

        return false;
    }

    public boolean canDeletePost(Integer postId) {
        Long requesterId = getRequesterId();
        if (requesterId == null) return false;

        if (hasAdminRole()) return true;

        return postRepository.existsByIdAndUser_Id(postId, requesterId);
    }

    public boolean canUpdateComment(String commentId) {
        Long requesterId = getRequesterId();
        if (requesterId == null) return false;


        return commentRepository.existsByIdAndUsername(commentId, getUsername());
    }

    public boolean canDeleteComment(String commentId) {
        Long requesterId = getRequesterId();
        if (requesterId == null) return false;

        if (hasAdminRole()) return true;

        return commentRepository.existsByIdAndUsername(commentId, getUsername());
    }

    public boolean canPromoteUser() {
        return hasAdminRole();
    }

    public boolean canAccessPerformanceMetrics() {
        return hasAdminRole();
    }

    private boolean hasAdminRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            return auth.getAuthorities().stream()
                    .anyMatch(a -> "ROLE_ADMIN".equalsIgnoreCase(a.getAuthority()));
        }
        return false;
    }

    private String getUsername() {
        var user = userRepository.findById(getRequesterId()).orElseThrow(
                () -> new RuntimeException("User not found")
        );

        return user.getUsername();
    }
}
