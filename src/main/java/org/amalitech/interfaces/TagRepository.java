package org.amalitech.interfaces;

import org.amalitech.models.Tag;
import org.amalitech.util.exception.NotFoundException;

import java.util.List;

/**
 * Repository interface for Tag CRUD operations.
 * Abstracts the data access layer from business logic.
 */
public interface TagRepository {

    /**
     * Save a new tag to the database.
     * @param tag the tag to save
     * @return the generated tag ID
     */
    int save(Tag tag);

    /**
     * Find a tag by its ID.
     * @param id the tag ID
     * @return the Tag object
     * @throws NotFoundException if tag doesn't exist
     */
    Tag findById(int id);

    /**
     * Find all tags.
     * @return list of all tags (may be empty)
     */
    List<Tag> findAll();

    /**
     * Update an existing tag.
     * @param tag the tag with updated data
     */
    void update(Tag tag);

    /**
     * Delete a tag by its ID.
     * @param id the tag ID
     */
    void delete(int id);

    /**
     * Find all tags associated with a specific post.
     * @param postId the post ID
     * @return list of tags for the post (may be empty)
     */
    List<Tag> findByPostId(int postId);

    /**
     * Find a tag by its name.
     * @param name the tag name
     * @return the Tag object, or null if not found
     */
    Tag findByName(String name);
}

