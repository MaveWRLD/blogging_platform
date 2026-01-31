package org.amalitech.interfaces;

import org.amalitech.models.Post;
import org.amalitech.models.PostTag;
import org.amalitech.models.Tag;

import java.sql.ResultSet;
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

    /**
     * Find all tag IDs associated with a specific post.
     *
     * @param postId the post ID
     * @return list of tag IDs for the post (may be empty)
     */
    List<Tag> findTagsByPostId(int postId);

    /**
     * Find all post IDs associated with a specific tag.
     * @param tagId the tag ID
     * @return list of post IDs with the tag (may be empty)
     */
    List<Post> findPostsByTagId(int tagId);

    /**
     * Remove all tag associations for a specific post.
     * Used when deleting a post.
     * @param postId the post ID
     */
    void deleteAllTagsForPost(int postId);
}

