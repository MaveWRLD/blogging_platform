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
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
@Tag(name = "Blog Posts", description = "Operations for managing blog posts (create, read, update, delete, trending)")
@SecurityRequirement(name = "bearerAuth")
public class PostController {

    private final PostService postService;
    private final PostMapper postMapper;
    private final CommentMapper commentMapper;

    @PostMapping
    @PreAuthorize("hasRole('writer') or hasRole('admin')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Create a new post",
            description = "Creates a new blog post with the provided title and body. The post will be created in DRAFT status. Requires authentication."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "Post created successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Bad request - Invalid post data or missing required fields",
                    content = @Content(schema = @Schema(example = "{\"timestamp\":\"2024-01-01T12:00:00\",\"errors\":{\"title\":\"Title is required\",\"body\":\"Body must not be blank\"},\"message\":\"Validation failed\"}"))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Authentication required",
                    content = @Content(schema = @Schema(example = "{\"error\":\"Unauthorized\",\"message\":\"Full authentication is required to access this resource\",\"path\":\"/api/posts\"}"))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - User does not have permission to create posts",
                    content = @Content(schema = @Schema(example = "{\"error\":\"Forbidden\",\"message\":\"Access is denied\",\"path\":\"/api/posts\"}"))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "422",
                    description = "Unprocessable Entity - Validation errors in request body",
                    content = @Content(schema = @Schema(example = "{\"timestamp\":\"2024-01-01T12:00:00\",\"errors\":{\"title\":\"Title must be between 1 and 200 characters\"},\"message\":\"Validation failed\"}"))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "Internal server error - Database or system failure",
                    content = @Content(schema = @Schema(example = "{\"timestamp\":\"2024-01-01T12:00:00\",\"message\":\"Failed to create post\",\"path\":\"/api/posts\"}"))
            )
    })
    public ResponseEntity<ApiResponse<PostDto>> createPost(
            @Parameter(description = "Post creation request", required = true)
            @Valid @RequestBody CreatePostRequest request) {

        Post post = postMapper.createPost(request);

        var createdPost = postService.createPost(post, request.getTagNames());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED, "Post created successfully", postMapper.toDto(createdPost)));
    }


    /**
     * Update existing post (partial update)
     * POST /api/posts/{id}
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update a post", description = "Partially updates a post. Only provided fields are updated. Requires authentication and proper authorization.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Post updated successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Bad request - Invalid post ID or update data",
                    content = @Content(schema = @Schema(example = "{\"timestamp\":\"2024-01-01T12:00:00\",\"message\":\"Invalid post ID\",\"path\":\"/api/posts/1\"}"))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Authentication required",
                    content = @Content(schema = @Schema(example = "{\"error\":\"Unauthorized\",\"message\":\"Full authentication is required to access this resource\",\"path\":\"/api/posts/1\"}"))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - User does not have permission to update this post",
                    content = @Content(schema = @Schema(example = "{\"error\":\"Forbidden\",\"message\":\"You can only update your own posts\",\"path\":\"/api/posts/1\"}"))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Post not found",
                    content = @Content(schema = @Schema(example = "{\"timestamp\":\"2024-01-01T12:00:00\",\"message\":\"Post not found with id: 1\",\"path\":\"/api/posts/1\"}"))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "422",
                    description = "Unprocessable Entity - Validation errors in request body",
                    content = @Content(schema = @Schema(example = "{\"timestamp\":\"2024-01-01T12:00:00\",\"errors\":{\"title\":\"Title must be between 1 and 200 characters\"},\"message\":\"Validation failed\"}"))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "Internal server error - Database or system failure",
                    content = @Content(schema = @Schema(example = "{\"timestamp\":\"2024-01-01T12:00:00\",\"message\":\"Failed to update post\",\"path\":\"/api/posts/1\"}"))
            )
    })
    public ResponseEntity<ApiResponse<PostDto>> updatePost(
            @Parameter(description = "Post ID", required = true)
            @PathVariable Integer id,
            @Parameter(description = "Post update request", required = true)
            @Valid @RequestBody UpdatePostRequest request) {

        Post updated = postService.updatePost(id, request);

        return ResponseEntity.ok(
                ApiResponse.success("Post updated successfully",postMapper.toDto(updated))
        );
    }


    @GetMapping
    @Operation(
            summary = "Get all posts with pagination",
            description = "Returns a paginated list of posts. Use 'page' and 'size' query parameters for pagination. No authentication required."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Posts retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Bad request - Invalid pagination parameters",
                    content = @Content(schema = @Schema(example = "{\"timestamp\":\"2024-01-01T12:00:00\",\"message\":\"Invalid page parameter\",\"path\":\"/api/posts\"}"))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "Internal server error - Database or system failure",
                    content = @Content(schema = @Schema(example = "{\"timestamp\":\"2024-01-01T12:00:00\",\"message\":\"Failed to retrieve posts\",\"path\":\"/api/posts\"}"))
            )
    })
    public ResponseEntity<ApiResponse<PagedPostsResponse>> getAllPosts(
            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(required = false, defaultValue = "0") int page,
            @Parameter(description = "Number of posts per page", example = "12")
            @RequestParam(required = false, defaultValue = "12") int size,
            @Parameter(description = "Sort field", example = "createdAt")
            @RequestParam(required = false, defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Sort direction (asc/desc)", example = "desc")
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
    @Operation(
            summary = "Get posts by user ID",
            description = "Returns a paginated list of posts created by a specific user. No authentication required."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Posts retrieved successfully",
                    content = @Content(schema = @Schema(implementation = PagedPostsResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Bad request - Invalid user ID or pagination parameters",
                    content = @Content(schema = @Schema(example = "{\"timestamp\":\"2024-01-01T12:00:00\",\"message\":\"Invalid user ID\",\"path\":\"/api/posts/user/invalid\"}"))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "User not found or no posts exist for this user",
                    content = @Content(schema = @Schema(example = "{\"timestamp\":\"2024-01-01T12:00:00\",\"message\":\"No posts found for user\",\"path\":\"/api/posts/user/999\"}"))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "Internal server error - Database or system failure",
                    content = @Content(schema = @Schema(example = "{\"timestamp\":\"2024-01-01T12:00:00\",\"message\":\"Failed to retrieve user posts\",\"path\":\"/api/posts/user/1\"}"))
            )
    })
    public ResponseEntity<ApiResponse<PagedPostsResponse>> getPostsByUserId(
            @Parameter(description = "User ID", required = true, example = "1")
            @PathVariable Long userId,
            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(required = false, defaultValue = "0") int page,
            @Parameter(description = "Number of posts per page", example = "10")
            @RequestParam(required = false, defaultValue = "10") int size
    ) {
        var posts = postService.findPostsByUserId(userId, page, size);
        var postDtos = posts.getContent();

        if (postDtos.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        var totalPosts = posts.getTotalElements();
        var hasNextPage = posts.hasNext();
        var hasPreviousPage = posts.hasPrevious();


        return ResponseEntity.ok(
                ApiResponse.success("Posts retrieved successfully", new PagedPostsResponse(
                        postDtos, posts.getNumber(), posts.getSize(), totalPosts, posts.getTotalPages(), hasPreviousPage, hasNextPage
                ))
        );
    }

    @GetMapping("/trending")
    @Operation(
            summary = "Get trending posts",
            description = "Returns a list of trending posts based on engagement and recency. No authentication required."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Trending posts retrieved successfully",
                    content = @Content(schema = @Schema(implementation = PagedPostsResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Bad request - Invalid pagination parameters",
                    content = @Content(schema = @Schema(example = "{\"timestamp\":\"2024-01-01T12:00:00\",\"message\":\"Invalid limit parameter\",\"path\":\"/api/posts/trending\"}"))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "Internal server error - Database or system failure",
                    content = @Content(schema = @Schema(example = "{\"timestamp\":\"2024-01-01T12:00:00\",\"message\":\"Failed to retrieve trending posts\",\"path\":\"/api/posts/trending\"}"))
            )
    })
    public ResponseEntity<ApiResponse<PagedPostsResponse>> getTrendingPosts(
            @Parameter(description = "Maximum number of trending posts to return", example = "10")
            @RequestParam(required = false, defaultValue = "10") Integer limit,
            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(required = false, defaultValue = "0") int page,
            @Parameter(description = "Number of posts per page", example = "12")
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
                ApiResponse.success("Trending posts retrieved successfully", new PagedPostsResponse(
                        postDtos, trendingDtos.getNumber(), trendingDtos.getSize(), totalPosts, trendingDtos.getTotalPages(), hasPreviousPage, hasNextPage
                ))
        );
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Delete a post",
            description = "Deletes the post with the specified ID. This action is irreversible. Requires authentication and proper authorization."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "204",
                    description = "Post deleted successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Bad request - Invalid post ID",
                    content = @Content(schema = @Schema(example = "{\"timestamp\":\"2024-01-01T12:00:00\",\"message\":\"Invalid post ID\",\"path\":\"/api/posts/1\"}"))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Authentication required",
                    content = @Content(schema = @Schema(example = "{\"error\":\"Unauthorized\",\"message\":\"Full authentication is required to access this resource\",\"path\":\"/api/posts/1\"}"))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - User does not have permission to delete this post",
                    content = @Content(schema = @Schema(example = "{\"error\":\"Forbidden\",\"message\":\"You can only delete your own posts\",\"path\":\"/api/posts/1\"}"))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Post not found",
                    content = @Content(schema = @Schema(example = "{\"timestamp\":\"2024-01-01T12:00:00\",\"message\":\"Post not found with id: 1\",\"path\":\"/api/posts/1\"}"))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "Internal server error - Database or system failure",
                    content = @Content(schema = @Schema(example = "{\"timestamp\":\"2024-01-01T12:00:00\",\"message\":\"Failed to delete post\",\"path\":\"/api/posts/1\"}"))
            )
    })
    public ResponseEntity<Void> deletePost(
            @Parameter(description = "Post ID", required = true, example = "1")
            @PathVariable Integer id) {
        postService.deletePost(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get post by ID with comments",
            description = "Returns full post details including comments and author information. No authentication required."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Post retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Bad request - Invalid post ID",
                    content = @Content(schema = @Schema(example = "{\"timestamp\":\"2024-01-01T12:00:00\",\"message\":\"Invalid post ID\",\"path\":\"/api/posts/invalid\"}"))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Post not found",
                    content = @Content(schema = @Schema(example = "{\"timestamp\":\"2024-01-01T12:00:00\",\"message\":\"Post not found with id: 1\",\"path\":\"/api/posts/1\"}"))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "Internal server error - Database or system failure",
                    content = @Content(schema = @Schema(example = "{\"timestamp\":\"2024-01-01T12:00:00\",\"message\":\"Failed to retrieve post\",\"path\":\"/api/posts/1\"}"))
            )
    })
    public ResponseEntity<ApiResponse<PostWithCommentsDto>> getPost(
            @Parameter(description = "Post ID", required = true, example = "1")
            @PathVariable Integer id) {
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
