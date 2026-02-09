package org.amalitech.interfaces;

import org.amalitech.dtos.postDtos.PostDto;
import org.amalitech.models.Post;
import org.amalitech.util.exception.ResourceNotFoundException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Post CRUD operations.
 * Abstracts the data access layer from business logic.
 */
public interface PostRepository {

    /**
     * Save a new post to the database.
     * The database will auto-generate the post ID.
     *
     * @param post the post to save
     * @return the generated post ID
     */
    int save(Post post);

    /**
     * Find a post by its ID.
     *
     * @param id the post ID
     * @return the Post object
     * @throws ResourceNotFoundException if post doesn't exist
     */
    Optional<Post> findById(int id);

    /**
     * Find all posts.
     *
     * @return list of all posts (may be empty)
     */
    List<Post> findAll(int page, int limit);

    List<Post> findRecentForTrending(int limit);

    /**
     * Update an existing post.
     * @param post the post with updated data
     */
    void update(Post post);

    /**
     * Delete a post by its ID.
     * @param id the post ID
     */
    void delete(int id);

    List<Post> findPosts(int page,
                            int size,
                            String tagName,
                            String username,
                            String searchTerm,
                            LocalDateTime createdAfter,
                            LocalDateTime createdBefore
    );

    Long countPosts();

    int countPosts(
            String tagName,
            String username,
            String searchTerm,
            LocalDateTime createdAfter,
            LocalDateTime createdBefore
    );
}

