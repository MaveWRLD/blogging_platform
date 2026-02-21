package org.amalitech.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.amalitech.dtos.*;
import org.amalitech.dtos.postDtos.*;
import org.amalitech.mappers.CommentMapper;
import org.amalitech.mappers.PostMapper;
import org.amalitech.entities.Comment;
import org.amalitech.entities.Post;
import org.amalitech.service.PostService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
@Tag(name = "Blog Posts", description = "Operations for managing blog posts (create, read, update, delete, trending)")
public class PostController {

    private final PostService postService;
    private final PostMapper postMapper;
    private final CommentMapper commentMapper;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Create a new post",
            description = "Creates a new blog post with the provided title and body. The post will be created in DRAFT status."
    )
    public ResponseEntity<ApiResponse<PostDto>> createPost(@Valid @RequestBody CreatePostRequest request) {

        Post post = postMapper.createPost(request);

        var createdPost = postService.createPost(post, request.getTagNames());

        return ResponseEntity.ok(ApiResponse.success(HttpStatus.CREATED, "Post created successfully", postMapper.toDto(createdPost)));
    }


    /**
     * Update existing post (partial update)
     * POST /api/posts/{id}
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update a post", description = "Partially updates a post. Only provided fields are updated.")
    public ResponseEntity<ApiResponse<PostDto>> updatePost(
            @PathVariable Integer id,
            @Valid @RequestBody UpdatePostRequest request) {

        Post updated = postService.updatePost(id, request);

        return ResponseEntity.ok(
                ApiResponse.success("Post updated successfully",postMapper.toDto(updated))
        );
    }


    @GetMapping
    @Operation(
            summary = "Get all posts with pagination",
            description = "Returns a paginated list of posts. Use 'page' and 'size' query parameters for pagination."
    )
    public ResponseEntity<ApiResponse<PagedPostsResponse>> getAllPosts(
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "12") int size,
            @RequestParam(required = false, defaultValue = "createdAt") String sortBy,
            @RequestParam(required = false, defaultValue = "desc") String sortDir
    ) {
        PostPagination result = getPostpagination(page, size, sortBy, sortDir);

        var userId = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        System.out.println("Authenticated user ID: " + userId);

        List<PostDto> posts = result.pagedPost().getContent();

        PagedPostsResponse pagedResponse = new PagedPostsResponse(
                posts, result.page(), result.size(), result.total(), result.totalPages(), result.hasPrevious(), result.hasNext()
        );

        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "Posts retrieved successfully", pagedResponse));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<PagedPostsResponse> getPostsByUserId(
            @PathVariable Long userId,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "10") int size
    ) {
        var posts = postService.findPostsByUserId(userId, page, size);
        var totalPosts = posts.getTotalElements();
        var hasNextPage = posts.hasNext();
        var hasPreviousPage = posts.hasPrevious();

        var postDtos = posts.getContent();

        if (posts.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(
                new PagedPostsResponse(
                        postDtos, posts.getNumber(), posts.getSize(), totalPosts, posts.getTotalPages(), hasPreviousPage, hasNextPage
                )
        );
    }

    @GetMapping("/trending")
    @Operation(
            summary = "Get trending posts",
            description = "Returns a list of trending posts based on engagement and recency"
    )
    public ResponseEntity<PagedPostsResponse> getTrendingPosts(
            @RequestParam(required = false, defaultValue = "10") Integer limit,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "12") int size
    ) {

        Pageable pageable = Pageable.ofSize(size).withPage(page);

        Page<Post> trendingDtos = postService.getTopTrendingPosts(limit, pageable);

        var totalPosts = trendingDtos.getTotalElements();
        var hasNextPage = trendingDtos.hasNext();
        var hasPreviousPage = trendingDtos.hasPrevious();


         List<PostDto> postDtos = trendingDtos.getContent().stream()
                .map(postMapper::toDto)
                .toList();

        return ResponseEntity.ok(
                new PagedPostsResponse(
                        postDtos, trendingDtos.getNumber(), trendingDtos.getSize(), totalPosts, trendingDtos.getTotalPages(), hasPreviousPage, hasNextPage
                )
        );
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Delete a post",
            description = "Deletes the post with the specified ID. This action is irreversible."
    )
    public void deletePost(@PathVariable Integer id) {
        postService.deletePost(id);
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get post by ID with comments",
            description = "Returns full post details including comments and author information"
    )
    public ResponseEntity<ApiResponse<PostWithCommentsDto>> getPost(@PathVariable Integer id) {
        if (id == null || id <= 0) {
            return ResponseEntity.badRequest().build();
        }

        var postWithComments = postService.findPostById(id);

        if (postWithComments == null || postWithComments.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Result comments = getPostComments(postWithComments);
        var postResponse = postMapper.toDtoWithComments(comments.postDto(), comments.commentDtos());
        postResponse.setTotalComments(comments.commentDtos().size());

        return ResponseEntity.ok(ApiResponse.success("Post with " + id + " found" , postResponse));
    }

    private record Result(PostDto postDto, List<CommentDto> commentDtos) {
    }

    private PostPagination getPostpagination(int page, int size, String sortBy, String sortDir) {
        page = Math.max(page, 0);
        size = Math.max(size, 1);

        Sort sort =
                sortDir.equalsIgnoreCase("ASC") ?
                        Sort.by(sortBy).ascending() :
                        Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(
                page,
                size,
                sort
        );

        Page<PostDto> pagedPost = postService.getPosts(pageable);

        long total = pagedPost.getTotalElements();
        int totalPages = pagedPost.getTotalPages();
        boolean hasNext = pagedPost.hasNext();
        boolean hasPrevious = pagedPost.hasPrevious();
        return new PostPagination(page, size, pagedPost, total, totalPages, hasNext, hasPrevious);
    }

    private record PostPagination(int page, int size, Page<PostDto> pagedPost, long total, int totalPages, boolean hasNext, boolean hasPrevious) {
    }

    private Result getPostComments(Map<Post, List<Comment>> postWithComments) {
        PostDto postDto = postWithComments.keySet().stream()
                .findFirst()
                .map(postMapper::toDto)
                .orElse(null);

        List<CommentDto> commentDtos = postWithComments.values().stream()
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .map(commentMapper::toDto)
                .toList();


        return new Result(postDto, commentDtos);
    }
}
