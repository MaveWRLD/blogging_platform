package org.amalitech.interfaces;

import org.amalitech.models.Post;
import org.amalitech.models.SortOrder;
import org.amalitech.util.exception.NotFoundException;

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
     * @param id the post ID
     * @return the Post object
     * @throws NotFoundException if post doesn't exist
     */
    Post findById(int id);

    /**
     * Find all posts.
     * @return list of all posts (may be empty)
     */
    List<Post> findAll();

    /**
     * Find all posts with pagination.
     * @param page the page number (0-indexed)
     * @param pageSize the number of posts per page
     * @return list of posts for the page
     */
    List<Post> findAllPaged(int page, int pageSize);

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

    /**
     * Count all posts in the database.
     * @return total number of posts
     */
    int countAll();

    /**
     * Search posts by various criteria.
     * @param query search query string
     * @param tagIds set of tag IDs to filter by
     * @param statuses set of post statuses to filter by
     * @param authorId user ID of the post author (optional)
     * @param order sort order for results
     * @param page page number (0-indexed)
     * @param size page size
     * @return list of matching posts
     */
    List<Post> search(String query, java.util.Set<Integer> tagIds,
                      java.util.Set<String> statuses, Integer authorId,
                      SortOrder order, int page, int size);

    /**
     * Count posts matching search criteria.
     * @param query search query string
     * @param tagIds set of tag IDs to filter by
     * @param statuses set of post statuses to filter by
     * @param authorId user ID of the post author (optional)
     * @return count of matching posts
     */
    int countSearch(String query, java.util.Set<Integer> tagIds,
                    java.util.Set<String> statuses, Integer authorId);

    /**
     * Find posts by tag ID.
     * @param tagId the tag ID
     * @param page page number (0-indexed)
     * @param size page size
     * @return list of posts with the specified tag
     */
    List<Post> findByTag(int tagId, int page, int size);

    /**
     * Count posts with a specific tag.
     * @param tagId the tag ID
     * @return count of posts with the tag
     */
    int countByTag(int tagId);
}

