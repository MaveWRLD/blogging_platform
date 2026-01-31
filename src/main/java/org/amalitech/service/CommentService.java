package org.amalitech.service;

import org.amalitech.util.exception.ValidationException;
import org.amalitech.models.Comment;
import org.amalitech.interfaces.CommentRepository;
import org.bson.Document;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.util.*;

/**
 * Service for comment operations.
 * Handles comment business logic and validation.
 * Depends on CommentRepository interface (not concrete implementation).
 */
@Service
public class CommentService {

    private final CommentRepository commentRepository;

    /**
     * Constructor with dependency injection.
     * @param commentRepository the comment repository (interface)
     */
    public CommentService(CommentRepository commentRepository) {
        this.commentRepository = commentRepository;
    }

    /**
     * Create a new comment.
     * @param comment the comment to create
     */
    public void createComment(Comment comment) {
        validateComment(comment);
        commentRepository.save(comment);
    }

    /**
     * Get all comments for a specific post.
     * @param postId the post ID
     * @return list of comments
     */
    public List<Comment> getCommentsByPostId(int postId) {
        if (postId <= 0) {
            throw new ValidationException("Invalid post ID");
        }
        return commentRepository.findByPostId(postId);
    }

    /**
     * Validate a comment.
     * @param comment the comment to validate
     * @throws ValidationException if validation fails
     */
    private void validateComment(Comment comment) {
        if (comment.getPostId() <= 0) {
            throw new ValidationException("Invalid post ID");
        }
        if (comment.getUserName() == null || comment.getUserName().isEmpty()) {
            throw new ValidationException("Invalid user name");
        }
        if (comment.getBody() == null || comment.getBody().trim().isEmpty()) {
            throw new ValidationException("Comment body cannot be empty");
        }
        if (comment.getBody().length() > 5000) {
            throw new ValidationException("Comment body cannot exceed 5000 characters");
        }
    }

    private Comment mapToComment(Document doc) {
        Comment comment = new Comment();
        comment.setId(doc.getObjectId("_id").toHexString());
        comment.setPostId(doc.getInteger("postId"));
        comment.setUserName(doc.getString("userName"));
        comment.setBody(doc.getString("body"));
        comment.setParentCommentId(doc.getInteger("parentCommentId"));

        return comment;
    }
}

