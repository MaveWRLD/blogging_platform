package org.amalitech.service;

import org.amalitech.repositories.CommentRepository;
import org.amalitech.entities.Comment;
import org.amalitech.exception.ResourceNotFoundException;
import org.amalitech.exception.ValidationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class CommentService {

    private final CommentRepository commentRepository;

    public CommentService(CommentRepository commentRepository) {
        this.commentRepository = commentRepository;
    }

    @Transactional
    public Comment save(Comment comment) {
        validateComment(comment);
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
        validateComment(comment);
        commentRepository.save(comment);
    }

    @Transactional
    public void deleteById(String commentId) {
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
}