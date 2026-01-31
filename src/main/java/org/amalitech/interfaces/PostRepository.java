package org.amalitech.interfaces;

import org.amalitech.models.Post;
import org.amalitech.models.SortOrder;
import org.amalitech.util.exception.NotFoundException;

import java.sql.ResultSet;
import java.util.List;

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
     * @throws NotFoundException if post doesn't exist
     */
    Post findById(int id);

    /**
     * Find all posts.
     *
     * @return list of all posts (may be empty)
     */
    List<Post> findAll();

    /**
     * Find posts by tag ID.
     *
     * @param tagId the tag ID
     * @param page  page number (0-indexed)
     * @param size  page size
     * @return list of posts with the specified tag
     */
    List<Post> findByTag(int tagId, int page, int size);

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


}

