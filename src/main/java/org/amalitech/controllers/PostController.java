package org.amalitech.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.amalitech.dtos.*;
import org.amalitech.dtos.postDtos.*;
import org.amalitech.mappers.CommentMapper;
import org.amalitech.mappers.PostMapper;
import org.amalitech.models.Comment;
import org.amalitech.models.Post;
import org.amalitech.service.PostService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

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

        Post post = postMapper.toEntity(request);

        postService.createPost(post, null);

        return ResponseEntity.ok(ApiResponse.success(HttpStatus.CREATED, postMapper.toDto(post), "Post created successfully"));
    }

    @GetMapping
    @Operation(
            summary = "Get all posts with pagination",
            description = "Returns a paginated list of posts. Use 'page' and 'size' query parameters for pagination."
    )
    public ResponseEntity<ApiResponse<PagedPostsResponse>> getAllPosts(
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "12") int size
    ) {
        page = Math.max(page, 0);
        size = Math.max(size, 1);
        List<PostDto> posts = postService.getAllPosts(page, size).stream().map(postMapper::toDto).collect(Collectors.toList());

        long total = postService.countPosts();
        int totalPages = (int) Math.ceil((double) total / size) - 1;
        boolean hasNext = (long) (page + 1) * size < total;
        boolean hasPrevious = page > 0;

        PagedPostsResponse pagedResponse = new PagedPostsResponse(
                posts,
                page,
                size,
                total,
                totalPages,
                hasPrevious,
                hasNext
        );

        return ResponseEntity.ok(ApiResponse.success(pagedResponse, "Posts retrieved successfully"));
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

    @GetMapping("/{id}")
    @Operation(
            summary = "Get post by ID with comments",
            description = "Returns full post details including comments and author information"
    )
    public ResponseEntity<PostWithCommentsDto> getPost(@PathVariable Integer id) {
        if (id == null || id <= 0) {
            return ResponseEntity.badRequest().build();
        }

        var postWithComments = postService.findPostById(id);

        if (postWithComments == null || postWithComments.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Result result = getPostComments(postWithComments);

        return ResponseEntity.ok(postMapper.toDtoWithComments(result.postDto(), result.commentDtos()));
    }

    private record Result(PostDto postDto, List<CommentDto> commentDtos) {
    }

    @GetMapping("/trending")
    @Operation(
            summary = "Get trending posts",
            description = "Returns a list of trending posts based on engagement and recency"
    )
    public ResponseEntity<ApiResponse<List<PostDto>>> getTrendingPosts(
            @RequestParam(required = false, defaultValue = "10") Integer limit
    ) {
        List<PostDto> trendingDtos = postService.getTrendingPosts(limit).stream()
                .map(postMapper::toDto)
                .toList();

        return ResponseEntity.ok(ApiResponse.success(trendingDtos, "Trending posts retrieved successfully"));
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
}
