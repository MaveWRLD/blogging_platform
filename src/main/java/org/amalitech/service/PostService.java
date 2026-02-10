package org.amalitech.service;

import org.amalitech.algorithm.CacheManager;
import org.amalitech.algorithm.TrendingSortAlgorithm;
import org.amalitech.aspect.Cacheable;
import org.amalitech.dtos.postDtos.PostFilter;
import org.amalitech.interfaces.PostTagRepository;
import org.amalitech.models.*;
import org.amalitech.util.PostValidator;
import org.amalitech.util.SortOrder;
import org.amalitech.util.exception.ResourceNotFoundException;
import org.amalitech.util.exception.ValidationException;
import org.amalitech.interfaces.PostRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class PostService {

    private final PostRepository postRepository;
    private final PostTagRepository postTagRepository ;
    private final CommentService commentService;
    private static final Logger logger= LoggerFactory.getLogger(PostService.class);
    private final TrendingSortAlgorithm trendingAlgorithm;
    private final CacheManager cacheManager;



    /**
     * Constructor with dependency injection.
     * @param postRepository the post repository (interface)
     * @param postTagRepository the post-tag service
     */
    public PostService(PostRepository postRepository, PostTagRepository postTagRepository, CommentService commentService, TrendingSortAlgorithm trendingAlgorithm, CacheManager cacheManager) {
        this.postRepository = postRepository;
        this.postTagRepository = postTagRepository;
        this.commentService = commentService;
        this.trendingAlgorithm = trendingAlgorithm;
        this.cacheManager = cacheManager;
    }


    public List<Post> findPosts(PostFilter f) {

        if (f == null) {
            f = new PostFilter();
            f.setPage(0);
            f.setSize(12);
        }

        LocalDateTime fromDateTime = null;
        if (f.getFromDate() != null) {
            fromDateTime = f.getFromDate().atStartOfDay();
        }

        LocalDateTime toDateTime = null;
        if (f.getToDate() != null) {
            toDateTime = f.getToDate().atTime(LocalTime.MAX);
        }

        List<Post> posts = postRepository.findPosts(
                f.getPage(),
                f.getSize(),
                f.getTag(),
                f.getAuthor(),
                f.getSearch(),
                fromDateTime,
                toDateTime
        );

        return posts == null ? Collections.emptyList() : posts;
    }

    public int postCount(PostFilter f) {
        if (f == null) {
            f = new PostFilter();
            f.setPage(0);
            f.setSize(12);
        }

        LocalDateTime fromDateTime = null;
        if (f.getFromDate() != null) {
            fromDateTime = f.getFromDate().atStartOfDay();
        }

        LocalDateTime toDateTime = null;
        if (f.getToDate() != null) {
            toDateTime = f.getToDate().atTime(LocalTime.MAX);
        }

        return postRepository.countPosts(
                f.getTag(),
                f.getAuthor(),
                f.getSearch(),
                fromDateTime,
                toDateTime
        );
    }

    public List<Post> getAllPosts(int page, int limit) {
        return postRepository.findAll(page, limit);
    }

    public Long countPosts(){
        return postRepository.countPosts();
    }

    /**
     * Get a post by ID.
     * @param id the post ID
     * @return the Post object
     */
    @Cacheable(keyPrefix = "post:id", ttlSeconds =  3600)
    public Map<Post, List<Comment>> findPostById(int id) {
        if (id <= 0) throw new ValidationException("Invalid post ID");
        var post = postRepository.findById(id);
        var comments = commentService.getCommentsByPostId(id);

        Post postObj = post.orElseThrow(
                () -> new ResourceNotFoundException("Post not found with ID: " + id)
        );

        return Map.of(postObj, comments);
    }


    /**
     * Get trending posts based on a custom algorithm that considers recency and engagement metrics.
      * @param limit the maximum number of trending posts to return
      * @return list of trending posts
     */
    @Cacheable(keyPrefix = "trending:posts", ttlSeconds = 120)
    public List<Post> getTrendingPosts(int limit) {
        logger.info("Calculating trending posts");

        List<Post> candidates = postRepository.findRecentForTrending(200);

        return trendingAlgorithm.getTopTrending(candidates, limit);
    }

    /**
     * Create a new post.
     * @param post the post to create
     * @param tagIds list of tag IDs to associate
     * @return the created post with generated ID
     */
    public void createPost(Post post, List<Integer> tagIds) {
        PostValidator.validateForCreation(post);

        int generatedId = postRepository.save(post);
        post.setId(generatedId);

        cacheManager.invalidatePattern("trending");
        cacheManager.invalidatePattern("recent");

        if (tagIds != null && !tagIds.isEmpty()) {
            postTagRepository.addTagsToPost(generatedId, tagIds);
        }
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
        }

        cacheManager.invalidate("post:slug:" + post.getId());
        cacheManager.invalidatePattern("trending");
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
