package org.amalitech.service;

import org.amalitech.util.exception.ValidationException;
import org.amalitech.models.Post;
import org.amalitech.models.SortOrder;
import org.amalitech.interfaces.PostRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;


public class PostService {

    private final PostRepository postRepository;
    private final PostValidator postValidator;
    private final PostTagService postTagService;

    /**
     * Constructor with dependency injection.
     * @param postRepository the post repository (interface)
     * @param postValidator the post validator
     * @param postTagService the post-tag service
     */
    public PostService(PostRepository postRepository, PostValidator postValidator, PostTagService postTagService) {
        this.postRepository = postRepository;
        this.postValidator = postValidator;
        this.postTagService = postTagService;
    }

    /**
     * Get all posts with pagination.
     * @param page page number (0-indexed)
     * @param pageSize page size
     * @return list of posts
     */
    public List<Post> getAllPosts(int page, int pageSize) {
        return postRepository.findAllPaged(page, pageSize);
    }

    /**
     * Get posts by tag with pagination.
     * @param tagId the tag ID
     * @param page page number (0-indexed)
     * @param pageSize page size
     * @return list of posts with the tag
     */
    public List<Post> listByTag(int tagId, int page, int pageSize) {
        if (tagId <= 0) throw new ValidationException("Invalid tag ID");
        return postRepository.findByTag(tagId, page, pageSize);
    }

    /**
     * Count posts with a specific tag.
     * @param tagId the tag ID
     * @return count of posts
     */
    public int countByTag(int tagId) {
        if (tagId <= 0) throw new ValidationException("Invalid tag ID");
        return postRepository.countByTag(tagId);
    }

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
    public List<Post> search(String query, Set<Integer> tagIds, Set<String> statuses,
                            Integer authorId, SortOrder order, int page, int size) {
        return postRepository.search(query, tagIds, statuses, authorId, order, page, size);
    }

    /**
     * Count posts matching search criteria.
     * @param query search query string
     * @param tagIds set of tag IDs to filter by
     * @param statuses set of post statuses to filter by
     * @param authorId user ID of the post author (optional)
     * @return count of matching posts
     */
    public int countSearch(String query, Set<Integer> tagIds, Set<String> statuses, Integer authorId) {
        return postRepository.countSearch(query, tagIds, statuses, authorId);
    }

    /**
     * Create a new post.
     * @param post the post to create
     * @param tagIds list of tag IDs to associate
     * @return the created post with generated ID
     */
    public Post createPost(Post post, List<Integer> tagIds) {
        postValidator.validateForCreation(post);

        int generatedId = postRepository.save(post);
        post.setId(generatedId);

        if (tagIds != null && !tagIds.isEmpty()) {
            postTagService.addTagsToPost(generatedId, tagIds);
        }

        return post;
    }

    /**
     * Get a post by ID.
     * @param id the post ID
     * @return the Post object
     */
    public Post getPostById(int id) {
        if (id <= 0) throw new ValidationException("Invalid post ID");
        return postRepository.findById(id);
    }

    /**
     * Update an existing post.
     * @param post the post with updated data
     * @param tagIds list of tag IDs to associate
     */
    public void updatePost(Post post, List<Integer> tagIds) {
        postValidator.validateForUpdate(post);
        post.setUpdatedAt(LocalDateTime.now());
        postRepository.update(post);

        if (tagIds != null) {
            postTagService.removeAllTagsFromPost(post.getId());
            postTagService.addTagsToPost(post.getId(), tagIds);
        }
    }

    /**
     * Delete a post by ID.
     * @param id the post ID
     */
    public void deletePost(int id) {
        if (id <= 0) throw new ValidationException("Invalid post ID");
        postTagService.removeAllTagsFromPost(id);
        postRepository.delete(id);
    }

    /**
     * Count all posts.
     * @return total number of posts
     */
    public int countAllPosts() {
        return postRepository.countAll();
    }

    /**
     * Get all tags for a post.
     * @param postId the post ID
     * @return list of tag IDs
     */
    public List<Integer> getTagsForPost(int postId) {
        return postTagService.getTagsForPost(postId);
    }

    /**
     * Get a user by their ID.
     * This is used to fetch author information for posts.
     * @param userId the user ID
     * @return the User object
     */
    public org.amalitech.models.User getUserById(int userId) {
        if (userId <= 0) throw new ValidationException("Invalid user ID");
        // Get the UserService from the ServiceContainer to avoid circular dependencies
        UserService userService = org.amalitech.config.ServiceContainer.getInstance().getUserService();
        return userService.findByUserId(userId);
    }
}

