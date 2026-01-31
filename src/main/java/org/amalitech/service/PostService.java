package org.amalitech.service;

import org.amalitech.util.PostValidator;
import org.amalitech.util.exception.ValidationException;
import org.amalitech.models.Post;
import org.amalitech.models.SortOrder;
import org.amalitech.interfaces.PostRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
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
     * Get all posts with pagination, sort order, and time filtering.
     * @param page page number (0-indexed)
     * @param pageSize page size
     * @param sortOrder the sort order
     * @param fromDate only include posts created after this date (null for all time)
     * @return list of posts sorted and filtered by time
     */
    public List<Post> getAllPosts(int page, int pageSize, SortOrder sortOrder, LocalDateTime fromDate) {
        List<Post> posts = search(null, Set.of(), Set.of(), null, sortOrder, page, pageSize);

        if (fromDate != null) {
            posts = posts.stream()
                    .filter(p -> p.getCreatedAt() != null && p.getCreatedAt().isAfter(fromDate))
                    .toList();
        }

        return posts;
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
     * Get posts by tag with pagination, sort order, and time filtering.
     * @param tagId the tag ID
     * @param page page number (0-indexed)
     * @param pageSize page size
     * @param sortOrder the sort order
     * @param fromDate only include posts created after this date (null for all time)
     * @return list of posts with the tag, sorted and filtered by time
     */
    public List<Post> listByTag(int tagId, int page, int pageSize, SortOrder sortOrder, LocalDateTime fromDate) {
        if (tagId <= 0) throw new ValidationException("Invalid tag ID");

        List<Post> posts = postRepository.findByTag(tagId, page, pageSize);

        if (fromDate != null) {
            posts = posts.stream()
                    .filter(p -> p.getCreatedAt() != null && p.getCreatedAt().isAfter(fromDate))
                    .toList();
        }

        return sortPosts(posts, sortOrder);
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
     * Create a new post.
     * @param post the post to create
     * @param tagIds list of tag IDs to associate
     * @return the created post with generated ID
     */
    public void createPost(Post post, List<Integer> tagIds) {
        postValidator.validateForCreation(post);

        int generatedId = postRepository.save(post);
        post.setId(generatedId);

        if (tagIds != null && !tagIds.isEmpty()) {
            postTagService.addTagsToPost(generatedId, tagIds);
        }
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
        UserService userService = org.amalitech.config.ServiceContainer.getInstance().getUserService();
        return userService.findByUserId(userId);
    }

    /**
     * Sort posts according to the specified sort order.
     * @param posts the list of posts to sort
     * @param sortOrder the sort order to apply
     * @return sorted list of posts
     */
    private List<Post> sortPosts(List<Post> posts, SortOrder sortOrder) {
        if (sortOrder == null || posts == null) {
            return posts;
        }

        return switch (sortOrder) {
            case NEWEST, MOST_COMMENTED -> posts.stream()
                    .sorted((a, b) -> {
                        if (a.getCreatedAt() == null || b.getCreatedAt() == null) return 0;
                        return b.getCreatedAt().compareTo(a.getCreatedAt());
                    })
                    .toList();
            case OLDEST -> posts.stream()
                    .sorted((a, b) -> {
                        if (a.getCreatedAt() == null || b.getCreatedAt() == null) return 0;
                        return a.getCreatedAt().compareTo(b.getCreatedAt());
                    })
                    .toList();
        };
    }
}

