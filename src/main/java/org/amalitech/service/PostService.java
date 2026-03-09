package org.amalitech.service;

import lombok.AllArgsConstructor;
import org.amalitech.dtos.postDtos.PostDto;
import org.amalitech.dtos.postDtos.PostFilter;
import org.amalitech.dtos.postDtos.UpdatePostRequest;
import org.amalitech.enums.PostStatus;
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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final PostMetricsService postMetricsService;
    private final TagRepository tagRepository;
    private final CommentService commentService;
    private final UserRepository userRepository;
    private final TagService tagService;
    private final PostMapper postMapper;

    private final ConcurrentHashMap<Integer, Object> postLocks = new ConcurrentHashMap<>();

    /**
     * Find posts with filtering, pagination, search
     */
    @Transactional(readOnly = true, propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    @Cacheable(value="filteredPosts",
            key="#f.title + '-' + #f.author + '-' + #f.fromDate + '-' + #f.toDate + '-' + #f.page + '-' + #f.size"
    )
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
    @Cacheable(value="allPosts",
            key="#pageable.pageNumber + '-' + #pageable.pageSize"
    )
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
        postMetricsService.incrementView(id);
        List<Comment> comments = commentService.getCommentsByPostId(id);

        return Map.of(post, comments);
    }

    @Cacheable(value = "postsByUser", key = "#userId + '-' + #page + '-' + #size")
    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW, isolation = Isolation.READ_COMMITTED)
    public Page<Post> findPostsByUserId(Long userId, int page, int size) {
        Pageable pageable = Pageable.ofSize(size).withPage(page);
        return postRepository.findByUserId(userId, pageable);
    }

    /**
     * Create a new post
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    @PreAuthorize("hasRole('writer') or hasRole('admin')")
    @Caching(evict = {
            @CacheEvict(value = "allPosts", condition = "#post.status == 'PUBLISHED'", allEntries = true),
            @CacheEvict(value = "trending-posts", allEntries = true)
    })
    public Post createPost(Post post, Set<String> tagNames) {
        PostValidator.validateForCreation(post);

        Set<Tag> tags = tagService.findOrCreateTagsByName(tagNames);

        var auth = SecurityContextHolder.getContext().getAuthentication();
        var userId = (Long) auth.getPrincipal();
        post.setTags(tags);
        var user = userRepository.findById(userId).orElseThrow();
        post.setUser(user);

        return postRepository.save(post);
    }

    @Transactional
    @PreAuthorize("@authorizationService.canUpdatePost(#postId)")
    @Caching(evict = {
            @CacheEvict(value = "post:detail", key = "#postId")
    })
    public Post updatePost(int postId, UpdatePostRequest request) {

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));

        postMapper.updateEntity(request, post);

        post.setUpdatedAt(Instant.now());

        if (post.getStatus() == PostStatus.published && post.getPublishedAt() == null) {
            post.setPublishedAt(Instant.now());
        }

        setTags(post, request.getTagIds());

        return postRepository.save(post);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    @Caching(evict = {
            @CacheEvict(value = "post:detail", key = "#postId"),
            @CacheEvict(value = "trending-posts", allEntries = true)
    })
    public void incrementLikeCount(int postId) {
        postMetricsService.incrementLike(postId);
    }

    /**
     * Delete a post
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    @PreAuthorize("@authorizationService.canDeletePost(#id)")
    @Caching(evict = {
            @CacheEvict(value = "post:detail", key = "#id"),
            @CacheEvict(value = "allPosts",  allEntries = true),
            @CacheEvict(value = "filteredPosts", allEntries = true)
    })
    public void deletePost(int id) {
        if (id <= 0) throw new ValidationException("Invalid post ID");
        postRepository.deleteById(id);
    }

    private void setTags(Post post, Set<Integer> newTagIds) {

        if (newTagIds == null) {
            return;
        }

        if (newTagIds.isEmpty()) {
            post.getTags().clear();
            return;
        }

        List<Tag> tags = tagRepository.findAllById(newTagIds);

        if (tags.size() != newTagIds.size()) {
            throw new ResourceNotFoundException("One or more tags not found");
        }

        post.getTags().clear();
        post.getTags().addAll(tags);
    }

    private Object getLock(int postId) {
        return postLocks.computeIfAbsent(postId, id -> new Object());
    }
}