package org.amalitech.interfaces;

import org.amalitech.models.Comment;
import org.amalitech.util.exception.ResourceNotFoundException;

import java.util.List;

/**
 * Repository interface for Comment operations.
 * Abstracts the data access layer (MongoDB) from business logic.
 * Provides clear, unambiguous method names for different query types.
 */
public interface CommentRepository {

    /**
     * Save a new comment to the database.
     * Sets the MongoDB ObjectId on the comment if new.
     * @param comment the comment to save
     */
    void save(Comment comment);

    /**
     * Find a comment by its MongoDB ObjectId (hex string).
     * @param objectId the MongoDB ObjectId as hex string
     * @return the Comment object
     * @throws ResourceNotFoundException if comment doesn't exist
     */
    Comment findByObjectId(String objectId);

    long countByPostId(int postId);

    /**
     * Find all comments for a specific post.
     * @param postId the post ID
     * @return list of comments for the post (may be empty)
     */
    List<Comment> findByPostId(int postId);

    /**
     * Update an existing comment.
     * @param comment the comment with updated data
     */
    void update(Comment comment);

    /**
     * Delete a comment by its ObjectId.
     * @param objectId the MongoDB ObjectId as hex string
     */
    void deleteByObjectId(String objectId);

    /**
     * Delete all comments associated with a post.
     * Used when deleting a post.
     * @param postId the post ID
     */
    void deleteByPostId(int postId);
}