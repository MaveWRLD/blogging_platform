package org.amalitech.service;

import org.amalitech.repositories.PostRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class AuthorizationService {

    private final PostRepository postRepository;

    public AuthorizationService(PostRepository postRepository) {
        this.postRepository = postRepository;
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
}
