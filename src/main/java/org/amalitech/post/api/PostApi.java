package org.amalitech.post.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.amalitech.dtos.CustomApiResponse;
import org.amalitech.post.dto.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/api/posts")
public interface PostApi {

    @PostMapping
    @PreAuthorize("hasRole('writer') or hasRole('admin')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Create a new post",
            description = "Creates a new blog post with the provided title and body."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Post created"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    ResponseEntity<CustomApiResponse<PostDto>> createPost(
            @Parameter(description = "Post creation request", required = true)
            @Valid @RequestBody CreatePostRequest request
    );

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('writer') or hasRole('admin')")
    @Operation(
            summary = "Update a post",
            description = "Updates an existing blog post with the provided details."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Post updated successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Post not found")
    })
    ResponseEntity<CustomApiResponse<PostDto>> updatePost(
            @Parameter(description = "Post ID", required = true)
            @PathVariable Integer id,
            @Parameter(description = "Post update request", required = true)
            @Valid @RequestBody UpdatePostRequest request
    );

    @GetMapping
    @Operation(
            summary = "Get all posts",
            description = "Retrieves a paginated list of all blog posts with optional sorting."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Posts retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request")
    })
    ResponseEntity<CustomApiResponse<PagedPostsResponse>> getAllPosts(
            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "10")
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Sort by field", example = "createdAt")
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Sort direction", example = "desc")
            @RequestParam(defaultValue = "desc") String sortDir
    );

    @GetMapping("/user/{userId}")
    @Operation(
            summary = "Get posts by user ID",
            description = "Retrieves a paginated list of posts created by a specific user."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Posts retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "No posts found for user")
    })
    ResponseEntity<CustomApiResponse<PagedPostsResponse>> getPostsByUserId(
            @Parameter(description = "User ID", required = true)
            @PathVariable Long userId,
            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "10")
            @RequestParam(defaultValue = "10") int size
    );

    @GetMapping("/trending")
    @Operation(
            summary = "Get trending posts",
            description = "Retrieves a paginated list of trending posts based on engagement metrics."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trending posts retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request")
    })
    ResponseEntity<CustomApiResponse<PagedPostsResponse>> getTrendingPosts(
            @Parameter(description = "Limit for trending posts", example = "10")
            @RequestParam Integer limit,
            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "10")
            @RequestParam(defaultValue = "10") int size
    );

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('writer') or hasRole('admin')")
    @Operation(
            summary = "Delete a post",
            description = "Deletes an existing blog post by its ID."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Post deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Post not found")
    })
    ResponseEntity<Void> deletePost(
            @Parameter(description = "Post ID", required = true)
            @PathVariable Integer id
    );

    @GetMapping("/{id}")
    @Operation(
            summary = "Get post by ID",
            description = "Retrieves a specific blog post along with its comments by post ID."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Post retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid post ID"),
            @ApiResponse(responseCode = "404", description = "Post not found")
    })
    ResponseEntity<CustomApiResponse<PostWithCommentsDto>> getPost(
            @Parameter(description = "Post ID", required = true)
            @PathVariable Integer id
    );
}
