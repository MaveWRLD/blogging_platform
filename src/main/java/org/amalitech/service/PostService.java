package org.amalitech.service;

import org.amalitech.interfaces.PostTagRepository;
import org.amalitech.interfaces.UserRepository;
import org.amalitech.models.Tag;
import org.amalitech.models.User;
import org.amalitech.util.PostValidator;
import org.amalitech.util.exception.ValidationException;
import org.amalitech.models.Post;
import org.amalitech.models.SortOrder;
import org.amalitech.interfaces.PostRepository;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class PostService {

    private final PostRepository postRepository;
    private final PostTagRepository postTagRepository ;
    private final UserRepository userRepository;

    /**
     * Constructor with dependency injection.
     * @param postRepository the post repository (interface)
     * @param postTagRepository the post-tag service
     */
    public PostService(PostRepository postRepository, PostTagRepository postTagRepository, UserRepository userRepository) {
        this.postRepository = postRepository;
        this.postTagRepository = postTagRepository;
        this.userRepository = userRepository;
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

        return postRepository.findByTag(tagId, page, pageSize);
    }

    /**
     * Create a new post.
     * @param post the post to create
     * @param tagIds list of tag IDs to associate
     * @return the created post with generated ID
     */
    public void createPost(Post post, List<Integer> tagIds) {
        PostValidator.validateForCreation(post);

//        int generatedId =
        postRepository.save(post);
//        post.setId(generatedId);
//
//        if (tagIds != null && !tagIds.isEmpty()) {
//            postTagService.addTagsToPost(generatedId, tagIds);
//        }
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
        PostValidator.validateForUpdate(post);
        post.setUpdatedAt(LocalDateTime.now());
        postRepository.update(post);

        if (tagIds != null) {
            postTagRepository.deleteAllTagsForPost(post.getId());
            postTagRepository.deleteAllTagsForPost(post.getId());
        }
    }

    /**
     * Delete a post by ID.
     * @param id the post ID
     */
    public void deletePost(int id) {
        if (id <= 0) throw new ValidationException("Invalid post ID");
        postTagRepository.deleteAllTagsForPost(id);
        postRepository.delete(id);
    }

    /**
     * Get all tags for a post.
     * @param postId the post ID
     * @return list of tag IDs
     */
    public List<Tag> getTagsForPost(int postId) {
        return postTagRepository.findTagsByPostId(postId);
    }

    /**
     * Get a user by their ID.
     * This is used to fetch author information for posts.
     * @param userId the user ID
     * @return the User object
     */
    public List<User> getUserById(int userId) {
        if (userId <= 0) throw new ValidationException("Invalid user ID");
        return userRepository.findByUserId(userId);
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

