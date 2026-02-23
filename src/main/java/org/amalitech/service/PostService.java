package org.amalitech.service;

import lombok.AllArgsConstructor;
import org.amalitech.algorithm.TrendingSortAlgorithm;
import org.amalitech.dtos.postDtos.PostDto;
import org.amalitech.dtos.postDtos.PostFilter;
import org.amalitech.dtos.postDtos.UpdatePostRequest;
import org.amalitech.mappers.PostMapper;
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
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@AllArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final TagRepository tagRepository;
    private final CommentService commentService;
    private final TrendingSortAlgorithm trendingAlgorithm;
    private final UserRepository userRepository;
    private final TagService tagService;
    private final PostMapper postMapper;

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
    public Page<PostDto> getPosts(Pageable pageable) {
        return postRepository.findAllProjected(pageable);
    }

    /**
     * Get a post by ID with comments
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "post:detail", key = "#id")
    public Map<Post, List<Comment>> findPostById(int id) {
        if (id <= 0) throw new ValidationException("Invalid post ID");

        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with ID: " + id));

        List<Comment> comments = commentService.getCommentsByPostId(id);

        return Map.of(post, comments);
    }

    @Cacheable(value = "postsByUser", key = "#userId + '-' + #page + '-' + #size")
    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW, isolation = Isolation.READ_COMMITTED)
    public Page<PostDto> findPostsByUserId(Long userId, int page, int size) {
        Pageable pageable = Pageable.ofSize(size).withPage(page);
        return postRepository.findPostsByUserId(userId, pageable);
    }

    @Cacheable(value = "trending-posts", key = "#limit + '-' + #pageable.pageNumber + '-' + #pageable.pageSize")
    @Transactional(readOnly = true)
    public Page<Post> getTopTrendingPosts(int limit, Pageable pageable) {
        if (limit <= 0 || limit > 100) {
            limit = 10;
        }

        Instant threshold = Instant.now().minus(30, ChronoUnit.DAYS);

        Page<Post> candidatePage = postRepository.findRecentPublishedPosts(threshold, pageable);

        if (candidatePage.isEmpty()) {
            return new PageImpl<>(Collections.emptyList(), pageable, 0);
        }

        List<Post> topKList = trendingAlgorithm.getTopTrending(candidatePage.getContent(), limit);

        return new PageImpl<>(topKList, pageable, candidatePage.getTotalElements());
    }

    /**
     * Create a new post
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    @PreAuthorize("hasRole('writer') or hasRole('admin')")
    @Caching(
            evict = {
            @CacheEvict(value = "allPosts", condition = "#post.status == 'PUBLISHED'",  allEntries = true),
            @CacheEvict(value = "filteredPosts", allEntries = true)
    })
    public Post createPost(Post post, Set<String> tagNames) {
        PostValidator.validateForCreation(post);

        Set<Tag> tags = tagService.findOrCreateTagsByName(tagNames);

        post.setTags(tags);
        var user = userRepository.findById(1).orElseThrow();
        post.setUser(user);

        return postRepository.save(post);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    @Caching(evict = {
            @CacheEvict(value = "post:detail", key = "#postId"),
            @CacheEvict(value = "allPosts",  allEntries = true),
            @CacheEvict(value = "filteredPosts", allEntries = true)
    })
    public Post updatePost(Integer postId, UpdatePostRequest request) {

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));

        postMapper.updateEntity(request, post);

        PostValidator.validateForUpdate(post);

        post.setUpdatedAt(Instant.now());

        if ("PUBLISHED".equalsIgnoreCase(String.valueOf(post.getStatus()))
                && post.getPublishedAt() == null) {
            post.setPublishedAt(Instant.now());
        }

        setTags(post, request.getTagIds());

        return postRepository.save(post);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    @Caching(evict = {
            @CacheEvict(value = "post:detail", key = "#postId"),
            @CacheEvict(value = "allPosts", allEntries = true),
            @CacheEvict(value = "filteredPosts", allEntries = true)
    })
    public Post incrementLikeCount(int postId) {
        if (postId <= 0) throw new ValidationException("Invalid post ID");

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with ID: " + postId));

        post.setLikeCount(post.getLikeCount() + 1);

        return postRepository.save(post);
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