package org.amalitech.interfaces;

import org.amalitech.models.Tag;
import org.amalitech.util.exception.ResourceNotFoundException;

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
     * @throws ResourceNotFoundException if tag doesn't exist
     */
    List<Tag> findById(int id);

    List<Tag> findTagsByPostId(int postId);

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
     * Find a tag by its name.
     * @param name the tag name
     * @return the Tag object, or null if not found
     */
    List<Tag> findByName(String name);
}

