package org.amalitech.comment;

import org.amalitech.comment.CommentRepository;
import org.amalitech.comment.Comment;
import org.amalitech.exception.ResourceNotFoundException;
import org.amalitech.exception.ValidationException;
import org.amalitech.post.PostExistenceChecker;
import org.amalitech.repositories.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final PostExistenceChecker postExistenceChecker;

    public CommentService(CommentRepository commentRepository, UserRepository userRepository, PostExistenceChecker postExistenceChecker) {
        this.commentRepository = commentRepository;
        this.userRepository = userRepository;
        this.postExistenceChecker = postExistenceChecker;
    }

    @Transactional
    public Comment save(Long postId, Comment comment) {
        if (postId == null || postId <= 0 || !postExistenceChecker.existsById(Math.toIntExact(postId))) {
            throw new ResourceNotFoundException("Post not found with ID: " + postId);
        }

        String username = getAuthenticatedUsername();
        comment.setUsername(username);
        validateComment(comment);
        comment.setPostId(postId);
        comment.setCreatedAt(Instant.now());
        return commentRepository.insert(comment);
    }

    @Transactional(readOnly = true)
    public Comment getCommentById(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Comment ID cannot be null or empty");
        }
        return commentRepository.findById(id).orElseThrow(
                () ->  new ResourceNotFoundException("Comment not found with ID: " + id)
        );
    }

    @Transactional(readOnly = true)
    public List<Comment> getCommentsByPostId(int postId) {
        if (postId <= 0) {
            throw new ValidationException("Invalid post ID");
        }
        return commentRepository.findByPostId(postId);
    }

    @Transactional
    public void update(Comment comment) {
        if (comment == null || comment.getId() == null) {
            throw new IllegalArgumentException("Comment or ID cannot be null");
        }
        if (comment.getBody() == null || comment.getBody().trim().isEmpty()) {
            throw new ValidationException("Comment body cannot be empty");
        }
        commentRepository.save(comment);
    }

    @Transactional
    public void deleteById(String commentId) {
        if (!commentRepository.existsById(commentId)) {
            throw new RuntimeException("Comment not found");
        }
        commentRepository.deleteById(commentId);
    }

    @Transactional
    public void deleteByPostId(int postId) {
        if (postId <= 0) {
            throw new ValidationException("Invalid post ID");
        }
        commentRepository.deleteByPostId(postId);
    }

    private void validateComment(Comment comment) {
        if (comment.getPostId() <= 0) {
            throw new ValidationException("Invalid post ID");
        }
        if (comment.getUsername() == null || comment.getUsername().trim().isEmpty()) {
            throw new ValidationException("Username cannot be empty");
        }
        if (comment.getBody() == null || comment.getBody().trim().isEmpty()) {
            throw new ValidationException("Comment body cannot be empty");
        }
        if (comment.getBody().length() > 5000) {
            throw new ValidationException("Comment body cannot exceed 5000 characters");
        }
    }

    private String getAuthenticatedUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        var userId = (Long) auth.getPrincipal();

        var user = userRepository.findById(userId).orElseThrow();

        return user.getUsername();
    }
}
