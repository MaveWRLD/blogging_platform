package org.amalitech.post;

/**
 * Narrow public port exposed by the Post feature so other features (e.g.
 * Comment) can check a post exists without depending on PostRepository or
 * PostService directly. Comment depends on Post through this interface;
 * Post never depends back on Comment.
 */
public interface PostExistenceChecker {

    boolean existsById(int postId);
}
