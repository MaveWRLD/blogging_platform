package org.amalitech.interfaces;

import org.amalitech.models.PostTag;
import org.amalitech.models.Tag;

import java.util.List;

/**
 * Repository interface for PostTag (join table) operations.
 * Manages the relationships between posts and tags.
 */
public interface PostTagRepository {

    /**
     * Create an association between a post and a tag.
     * @param postTag the PostTag object containing post and tag IDs
     */
    int save(PostTag postTag);

    /**
     * Remove an association between a post and a tag.
     * @param postTag the PostTag object containing post and tag IDs
     */
    void delete(PostTag postTag);


    void addTagsToPost(int postId, List<Integer> tagIds);

    /**
     * Remove all tag associations for a specific post.
     * Used when deleting a post.
     * @param postId the post ID
     */
    void deleteAllTagsForPost(int postId);
}

