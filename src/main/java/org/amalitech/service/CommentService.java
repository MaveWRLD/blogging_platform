package org.amalitech.service;

import org.amalitech.interfaces.CommentRepository;
import org.amalitech.models.Comment;
import org.amalitech.util.exception.ResourceNotFoundException;
import org.amalitech.util.exception.ValidationException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CommentService {

    private final CommentRepository commentRepository;

    public CommentService(CommentRepository commentRepository) {
        this.commentRepository = commentRepository;
    }

    public Comment save(Comment comment) {
        validateComment(comment);
        commentRepository.save(comment);
        return comment;
    }

    public Comment findById(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Comment ID cannot be null or empty");
        }
        Comment comment = commentRepository.findByObjectId(id);
        if (comment == null) {
            throw new ResourceNotFoundException("Comment not found with ID: " + id);
        }
        return comment;
    }

    public List<Comment> getCommentsByPostId(int postId) {
        if (postId <= 0) {
            throw new ValidationException("Invalid post ID");
        }
        return commentRepository.findByPostId(postId);
    }

    public void update(Comment comment) {
        if (comment == null || comment.getId() == null) {
            throw new IllegalArgumentException("Comment or ID cannot be null");
        }
        validateComment(comment);
        commentRepository.update(comment);
    }

    public void deleteById(String commentId) {
        commentRepository.deleteByObjectId(commentId);
    }

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