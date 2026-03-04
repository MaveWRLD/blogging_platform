package org.amalitech.controllers;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.amalitech.api.doc.PostApi;
import org.amalitech.dtos.*;
import org.amalitech.dtos.postDtos.*;
import org.amalitech.mappers.CommentMapper;
import org.amalitech.mappers.PostMapper;
import org.amalitech.entities.Comment;
import org.amalitech.entities.Post;
import org.amalitech.service.PostMetricsService;
import org.amalitech.service.PostService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Parameter;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
@Tag(name = "Blog Posts", description = "Operations for managing blog posts (create, read, update, delete, trending)")
@SecurityRequirement(name = "bearerAuth")
public class PostController implements PostApi {

    private final PostService postService;
    private final PostMetricsService postMetricsService;
    private final PostMapper postMapper;
    private final CommentMapper commentMapper;

    @Override
    public ResponseEntity<CustomApiResponse<PostDto>> createPost(CreatePostRequest request) {

        Post post = postMapper.createPost(request);
        var created = postService.createPost(post, request.getTagNames());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CustomApiResponse.success(
                        HttpStatus.CREATED,
                        "Post created successfully",
                        postMapper.toDto(created)
                ));
    }

    /**
     * Update existing post (partial update)
     * POST /api/posts/{id}
     */
    @Override
    public ResponseEntity<CustomApiResponse<PostDto>> updatePost(
            @Parameter(description = "Post ID", required = true)
            @PathVariable Integer id,
            @Parameter(description = "Post update request", required = true)
            @Valid @RequestBody UpdatePostRequest request) {

        Post updated = postService.updatePost(id, request);

        return ResponseEntity.ok(
                CustomApiResponse.success("Post updated successfully",postMapper.toDto(updated))
        );
    }

    @Override
    public ResponseEntity<CustomApiResponse<PagedPostsResponse>> getAllPosts(
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

        List<PostDto> posts = result.pagedPost().getContent();

        PagedPostsResponse pagedResponse = new PagedPostsResponse(
                posts, result.page(), result.size(), result.total(), result.totalPages(), result.hasPrevious(), result.hasNext()
        );

        return ResponseEntity.ok(CustomApiResponse.success(HttpStatus.OK, "Posts retrieved successfully", pagedResponse));
    }

    @Override
    public ResponseEntity<CustomApiResponse<PagedPostsResponse>> getPostsByUserId(
            @Parameter(description = "User ID", required = true, example = "1")
            @PathVariable Long userId,
            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(required = false, defaultValue = "0") int page,
            @Parameter(description = "Number of posts per page", example = "10")
            @RequestParam(required = false, defaultValue = "10") int size
    ) {
        var pageablePosts = postService.findPostsByUserId(userId, page, size);
        var post = pageablePosts.getContent();

        if (post.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        var postDtos = post.stream().map(postMapper::toDto).toList();

        var totalPosts = pageablePosts.getTotalElements();
        var hasNextPage = pageablePosts.hasNext();
        var hasPreviousPage = pageablePosts.hasPrevious();


        return ResponseEntity.ok(
                CustomApiResponse.success("Posts retrieved successfully", new PagedPostsResponse(
                        postDtos, pageablePosts.getNumber(), pageablePosts.getSize(), totalPosts, pageablePosts.getTotalPages(), hasPreviousPage, hasNextPage
                ))
        );
    }

    @Override
    public ResponseEntity<CustomApiResponse<PagedPostsResponse>> getTrendingPosts(
            @RequestParam(required = false, defaultValue = "10") Integer limit,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "12") int size
    ) {

        limit = (limit == null || limit <= 0 || limit > 100) ? 10 : limit;

        List<Post> cachedTrending = postMetricsService.getCachedTrending();

        if (isEmpty(cachedTrending)) {
            return buildEmptyTrendingResponse(page, size);
        }

        List<Post> limited = cachedTrending.stream().limit(limit).toList();
        PaginationRange range = calculatePaginationRange(page, size, limited.size());

        if (range.isOutOfBounds(limited.size())) {
            return buildEmptyTrendingResponse(page, size, limited.size());
        }

        List<Post> posts = limited.subList(range.start(), range.end());
        List<PostDto> pagedPosts = posts.stream().map(postMapper::toDto).toList();

        PagedPostsResponse response = buildPaginationResponse(pagedPosts, page, size, limited.size());

        return ResponseEntity.ok(
                CustomApiResponse.success("Trending posts retrieved successfully", response)
        );
    }



    @Override
    public ResponseEntity<Void> deletePost(
            @Parameter(description = "Post ID", required = true, example = "1")
            @PathVariable Integer id) {
        postService.deletePost(id);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<CustomApiResponse<PostWithCommentsDto>> getPost(
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

        return ResponseEntity.ok(CustomApiResponse.success("Post with " + id + " found" , postResponse));
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

    private boolean isEmpty(List<Post> list) {
        return list == null || list.isEmpty();
    }

    private PaginationRange calculatePaginationRange(int page, int size, int total) {
        int start = page * size;
        int end = Math.min(start + size, total);
        return new PaginationRange(start, end);
    }

    private record PaginationRange(int start, int end) {
        boolean isOutOfBounds(int total) {
            return start >= total;
        }
    }

    private ResponseEntity<CustomApiResponse<PagedPostsResponse>> buildEmptyTrendingResponse(int page, int size) {
        return buildEmptyTrendingResponse(page, size, 0);
    }

    private ResponseEntity<CustomApiResponse<PagedPostsResponse>> buildEmptyTrendingResponse(int page, int size, int total) {
        int totalPages = total == 0 ? 0 : (int) Math.ceil((double) total / size);
        return ResponseEntity.ok(
                CustomApiResponse.success(
                        "Trending posts retrieved successfully",
                        new PagedPostsResponse(List.of(), page, size, total, totalPages, page > 0, false)
                )
        );
    }

    private PagedPostsResponse buildPaginationResponse(List<PostDto> pagedPosts, int page, int size, int total) {
        int totalPages = (int) Math.ceil((double) total / size);
        boolean hasNext = page < totalPages - 1;
        boolean hasPrevious = page > 0;

        return new PagedPostsResponse(pagedPosts, page, size, total, totalPages, hasPrevious, hasNext);
    }
}
