package org.amalitech.service;

import org.amalitech.util.exception.ValidationException;
import org.amalitech.models.Post;

/**
 * Validator for Post objects.
 * Handles all validation logic for posts.
 * Follows Single Responsibility Principle.
 */
public class PostValidator {

    private static final int MAX_TITLE_LENGTH = 255;
    private static final int MAX_BODY_LENGTH = 50000;

    /**
     * Validate a post for creation.
     * @param post the post to validate
     * @throws ValidationException if validation fails
     */
    public void validateForCreation(Post post) {
        validateTitle(post.getTitle());
        validateBody(post.getBody());
        validateUserId(post.getUserId());
    }

    /**
     * Validate a post for update.
     * @param post the post to validate
     * @throws ValidationException if validation fails
     */
    public void validateForUpdate(Post post) {
        validateId(post.getId());
        validateTitle(post.getTitle());
        validateBody(post.getBody());
    }

    /**
     * Validate post ID.
     * @param id the post ID
     * @throws ValidationException if ID is invalid
     */
    private void validateId(int id) {
        if (id <= 0) {
            throw new ValidationException("Post ID must be positive");
        }
    }

    /**
     * Validate post title.
     * @param title the title
     * @throws ValidationException if title is invalid
     */
    private void validateTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            throw new ValidationException("Post title cannot be empty");
        }
        if (title.length() > MAX_TITLE_LENGTH) {
            throw new ValidationException("Post title cannot exceed " + MAX_TITLE_LENGTH + " characters");
        }
    }

    /**
     * Validate post body.
     * @param body the body
     * @throws ValidationException if body is invalid
     */
    private void validateBody(String body) {
        if (body == null || body.trim().isEmpty()) {
            throw new ValidationException("Post body cannot be empty");
        }
        if (body.length() > MAX_BODY_LENGTH) {
            throw new ValidationException("Post body cannot exceed " + MAX_BODY_LENGTH + " characters");
        }
    }

    /**
     * Validate user ID (post author).
     * @param userId the user ID
     * @throws ValidationException if user ID is invalid
     */
    private void validateUserId(int userId) {
        if (userId <= 0) {
            throw new ValidationException("Post author user ID must be positive");
        }
    }



}

