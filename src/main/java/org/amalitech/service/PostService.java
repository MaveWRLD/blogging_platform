package org.amalitech.service;

import org.amalitech.algorithm.TrendingSortAlgorithm;
import org.amalitech.dtos.postDtos.PostFilter;
import org.amalitech.repositories.TagRepository;
import org.amalitech.entities.*;
import org.amalitech.repositories.PostRepository;
import org.amalitech.repositories.UserRepository;
import org.amalitech.repositories.specifications.PostSpecification;
import org.amalitech.util.PostValidator;
import org.amalitech.exception.ResourceNotFoundException;
import org.amalitech.exception.ValidationException;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class PostService {

    private final PostRepository postRepository;
    private final TagRepository tagRepository;
    private final CommentService commentService;
    private final TrendingSortAlgorithm trendingAlgorithm;
    private final UserRepository userRepository;

    public PostService(
            PostRepository postRepository,
            TagRepository tagRepository,
            CommentService commentService,
            TrendingSortAlgorithm trendingAlgorithm,
            UserRepository userRepository) {
        this.postRepository = postRepository;
        this.tagRepository = tagRepository;
        this.commentService = commentService;
        this.trendingAlgorithm = trendingAlgorithm;
        this.userRepository = userRepository;
    }

    /**
     * Find posts with filtering, pagination, search
     */
    @Transactional(readOnly = true, propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    @Cacheable(value="filteredPosts", key="#f != null ? #f.hashCode() : 'all'")
    public Page<Post> findPosts(PostFilter f) {
        if (f == null) {
            f = new PostFilter();
            f.setPage(0);
            f.setSize(12);
        }

        Pageable pageable = PageRequest.of(
                f.getPage(),
                f.getSize(),
                Sort.by("createdAt").descending()
        );

        Specification<Post> spec = Specification.where(PostSpecification.byTitle(f.getTitle()))
                .and(PostSpecification.byAuthor(f.getAuthor()))
                .and(PostSpecification.byPublicatedAtRange(f.getFromDate(), f.getToDate()));

        return postRepository.findAll(spec, pageable);
    }

    /**
     * Get paged posts (simple latest)
     */
    @Transactional(readOnly = true, propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    @Cacheable(value="allPosts")
    public Page<Post> getPosts(Pageable pageable) {
        return postRepository.findAll(pageable);
    }


    /**
     * Get a post by ID with comments
     */
    @Transactional(readOnly = true, propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    @Cacheable(value = "post:detail", key = "#id")
    public Map<Post, List<Comment>> findPostById(int id) {
        if (id <= 0) throw new ValidationException("Invalid post ID");

        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with ID: " + id));

        List<Comment> comments = commentService.getCommentsByPostId(id);

        return Map.of(post, comments);
    }

    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW, isolation = Isolation.READ_COMMITTED)
    public Page<Post> findPostsByUserId(Long userId, int page, int size) {
        Pageable pageable = Pageable.ofSize(size).withPage(page);
        return postRepository.findPostsByUserId(userId, pageable);
    }

    @Cacheable(value = "trending-posts", key = "#limit", unless = "#result.isEmpty()")
    @Transactional(readOnly = true)
    public List<Post> getTopTrendingPosts(int limit) {
        if (limit <= 0 || limit > 100) {
            limit = 10;
        }

        Instant threshold = Instant.now().minus(30, ChronoUnit.DAYS);

        List<Post> candidatePosts = postRepository.findRecentPublishedPosts(threshold);

        if (candidatePosts.isEmpty()) {
            return Collections.emptyList();
        }

        return trendingAlgorithm.getTopTrending(candidatePosts, limit);
    }

    /**
     * Create a new post
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    @Caching(
            evict = {
            @CacheEvict(value = "allPosts", condition = "#post.status == 'PUBLISHED'",  allEntries = true),
            @CacheEvict(value = "filteredPosts", allEntries = true)
    })
    public Post createPost(Post post, List<Long> tagIds) {
        PostValidator.validateForCreation(post);

//        Set<Tag> tags = tagIds.stream().map(tagId -> tagRepository.findById(Math.toIntExact(tagId))
//                        .orElseThrow(() -> new ResourceNotFoundException("Tag not found: " + tagId)))
//                .collect(Collectors.toSet());

//        post.setTags(tags);
        var user = userRepository.findById(1).orElseThrow();
        post.setUser(user);

        return postRepository.save(post);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    @Caching(evict = {
            @CacheEvict(value = "post:detail", key = "#post.id"),
            @CacheEvict(value = "allPosts",  allEntries = true),
            @CacheEvict(value = "filteredPosts", allEntries = true)
    })
    public void updatePost(Post post, List<Integer> newTagIds) {
        PostValidator.validateForUpdate(post);
        post.setUpdatedAt(Instant.now());
        if ("PUBLISHED".equalsIgnoreCase(String.valueOf(post.getStatus())) && post.getPublishedAt() == null) {
            post.setPublishedAt(Instant.now());
        }

        setTags(post, newTagIds);

        postRepository.save(post);

    }

    /**
     * Delete a post
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    @Caching(evict = {
            @CacheEvict(value = "post:detail", key = "#id"),
            @CacheEvict(value = "allPosts",  allEntries = true),
            @CacheEvict(value = "filteredPosts", allEntries = true)
    })
    public void deletePost(int id) {
        if (id <= 0) throw new ValidationException("Invalid post ID");

        if (!postRepository.existsById(id)) {
            throw new ResourceNotFoundException("Post not found with id: " + id);
        }

        postRepository.deleteById(id);
    }

    private void setTags(Post post, List<Integer> newTagIds) {
        if (newTagIds != null) {
            post.getTags().clear();

            if (!newTagIds.isEmpty()) {
                List<Tag> newTags = tagRepository.findAllById(newTagIds);
                if (newTags.size() != newTagIds.size()) {
                    throw new ResourceNotFoundException("One or more tags not found");
                }
                post.getTags().addAll(newTags);
            }
        }
    }
}