package org.amalitech.comment.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.amalitech.common.dto.CustomApiResponse;
import org.amalitech.comment.dto.CommentDto;
import org.amalitech.comment.dto.CreateCommentRequest;
import org.amalitech.comment.dto.UpdateCommentRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("/api/posts/{postId}/comments")
@Tag(
        name = "Comments",
        description = "Endpoints for managing comments and replies"
)
@SecurityRequirement(name = "bearerAuth")
public interface CommentApi {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Create a new comment",
            description = "Create a new comment on a specific post. If parentId is provided, the comment will be a reply to the specified parent comment. Requires authentication."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "Comment created successfully",
                    content = @io.swagger.v3.oas.annotations.media.Content(schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = CustomApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Bad request - Invalid comment data or missing required fields",
                    content = @io.swagger.v3.oas.annotations.media.Content(schema = @io.swagger.v3.oas.annotations.media.Schema(example = "{\"timestamp\":\"2024-01-01T12:00:00\",\"errors\":{\"body\":\"Comment body is required\"},\"message\":\"Validation failed\"}"))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Authentication required",
                    content = @io.swagger.v3.oas.annotations.media.Content(schema = @io.swagger.v3.oas.annotations.media.Schema(example = "{\"error\":\"Unauthorized\",\"message\":\"Full authentication is required to access this resource\",\"path\":\"/api/posts/1/comments\"}"))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - User does not have permission to comment",
                    content = @io.swagger.v3.oas.annotations.media.Content(schema = @io.swagger.v3.oas.annotations.media.Schema(example = "{\"error\":\"Forbidden\",\"message\":\"You do not have permission to comment\",\"path\":\"/api/posts/1/comments\"}"))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Post not found - The post to comment on does not exist",
                    content = @io.swagger.v3.oas.annotations.media.Content(schema = @io.swagger.v3.oas.annotations.media.Schema(example = "{\"timestamp\":\"2024-01-01T12:00:00\",\"message\":\"Post not found\",\"path\":\"/api/posts/999/comments\"}"))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "422",
                    description = "Unprocessable Entity - Validation errors in request body",
                    content = @io.swagger.v3.oas.annotations.media.Content(schema = @io.swagger.v3.oas.annotations.media.Schema(example = "{\"timestamp\":\"2024-01-01T12:00:00\",\"errors\":{\"body\":\"Comment body must be between 1 and 1000 characters\"},\"message\":\"Validation failed\"}"))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "Internal server error - Database or system failure",
                    content = @io.swagger.v3.oas.annotations.media.Content(schema = @io.swagger.v3.oas.annotations.media.Schema(example = "{\"timestamp\":\"2024-01-01T12:00:00\",\"message\":\"Failed to create comment\",\"path\":\"/api/posts/1/comments\"}"))
            )
    })
    ResponseEntity<CustomApiResponse<CommentDto>> createComment(
            @Parameter(description = "Comment creation request", required = true)
            @Valid @RequestBody CreateCommentRequest request,
            @Parameter(description = "Post ID to add comment to", required = true, example = "1")
            @PathVariable Long postId
    );

    @GetMapping
    @Operation(
            summary = "Get comments by post ID",
            description = "Retrieve all comments for a specific post. No authentication required."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Comments retrieved successfully",
                    content = @io.swagger.v3.oas.annotations.media.Content(schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = CustomApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Bad request - Invalid post ID",
                    content = @io.swagger.v3.oas.annotations.media.Content(schema = @io.swagger.v3.oas.annotations.media.Schema(example = "{\"timestamp\":\"2024-01-01T12:00:00\",\"message\":\"Invalid post ID\",\"path\":\"/api/posts/invalid/comments\"}"))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Post not found",
                    content = @io.swagger.v3.oas.annotations.media.Content(schema = @io.swagger.v3.oas.annotations.media.Schema(example = "{\"timestamp\":\"2024-01-01T12:00:00\",\"message\":\"Post not found\",\"path\":\"/api/posts/999/comments\"}"))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "Internal server error - Database or system failure",
                    content = @io.swagger.v3.oas.annotations.media.Content(schema = @io.swagger.v3.oas.annotations.media.Schema(example = "{\"timestamp\":\"2024-01-01T12:00:00\",\"message\":\"Failed to retrieve comments\",\"path\":\"/api/posts/1/comments\"}"))
            )
    })
    ResponseEntity<CustomApiResponse<List<CommentDto>>> getComment(
            @Parameter(description = "Post ID to retrieve comments for", required = true, example = "1")
            @PathVariable Long postId
    );

    @PutMapping("/{commentId}")
    @PreAuthorize("@commentAuthorizationService.canUpdateComment(#commentId)")
    @Operation(
            summary = "Update a comment",
            description = "Update the body of an existing comment. Only the body can be updated. Requires authentication and proper authorization."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Comment updated successfully",
                    content = @io.swagger.v3.oas.annotations.media.Content(schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = CommentDto.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Bad request - Invalid comment ID or update data",
                    content = @io.swagger.v3.oas.annotations.media.Content(schema = @io.swagger.v3.oas.annotations.media.Schema(example = "{\"timestamp\":\"2024-01-01T12:00:00\",\"message\":\"Invalid comment ID\",\"path\":\"/api/posts/1/comments/invalid\"}"))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Authentication required",
                    content = @io.swagger.v3.oas.annotations.media.Content(schema = @io.swagger.v3.oas.annotations.media.Schema(example = "{\"error\":\"Unauthorized\",\"message\":\"Full authentication is required to access this resource\",\"path\":\"/api/posts/1/comments/1\"}"))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - User does not have permission to update this comment",
                    content = @io.swagger.v3.oas.annotations.media.Content(schema = @io.swagger.v3.oas.annotations.media.Schema(example = "{\"error\":\"Forbidden\",\"message\":\"You can only update your own comments\",\"path\":\"/api/posts/1/comments/1\"}"))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Comment not found",
                    content = @io.swagger.v3.oas.annotations.media.Content(schema = @io.swagger.v3.oas.annotations.media.Schema(example = "{\"timestamp\":\"2024-01-01T12:00:00\",\"message\":\"Comment not found\",\"path\":\"/api/posts/1/comments/1\"}"))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "422",
                    description = "Unprocessable Entity - Validation errors in request body",
                    content = @io.swagger.v3.oas.annotations.media.Content(schema = @io.swagger.v3.oas.annotations.media.Schema(example = "{\"timestamp\":\"2024-01-01T12:00:00\",\"errors\":{\"body\":\"Comment body must be between 1 and 1000 characters\"},\"message\":\"Validation failed\"}"))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "Internal server error - Database or system failure",
                    content = @io.swagger.v3.oas.annotations.media.Content(schema = @io.swagger.v3.oas.annotations.media.Schema(example = "{\"timestamp\":\"2024-01-01T12:00:00\",\"message\":\"Failed to update comment\",\"path\":\"/api/posts/1/comments/1\"}"))
            )
    })
    ResponseEntity<CommentDto> updateComment(
            @Parameter(description = "Post ID", required = true, example = "1")
            @PathVariable Long postId,
            @Parameter(description = "Comment ID", required = true, example = "1")
            @PathVariable String commentId,
            @Parameter(description = "Comment update request", required = true)
            @Valid @RequestBody UpdateCommentRequest request
    );

    @DeleteMapping("/{commentId}")
    @PreAuthorize("@commentAuthorizationService.canDeleteComment(#commentId)")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Delete a comment",
            description = "Delete a comment by its ID. If the comment has replies, they will also be deleted. Requires authentication and proper authorization. This action is irreversible."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "204",
                    description = "Comment deleted successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Bad request - Invalid comment ID",
                    content = @io.swagger.v3.oas.annotations.media.Content(schema = @io.swagger.v3.oas.annotations.media.Schema(example = "{\"timestamp\":\"2024-01-01T12:00:00\",\"message\":\"Invalid comment ID\",\"path\":\"/api/posts/1/comments/invalid\"}"))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Authentication required",
                    content = @io.swagger.v3.oas.annotations.media.Content(schema = @io.swagger.v3.oas.annotations.media.Schema(example = "{\"error\":\"Unauthorized\",\"message\":\"Full authentication is required to access this resource\",\"path\":\"/api/posts/1/comments/1\"}"))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - User does not have permission to delete this comment",
                    content = @io.swagger.v3.oas.annotations.media.Content(schema = @io.swagger.v3.oas.annotations.media.Schema(example = "{\"error\":\"Forbidden\",\"message\":\"You can only delete your own comments\",\"path\":\"/api/posts/1/comments/1\"}"))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Comment not found",
                    content = @io.swagger.v3.oas.annotations.media.Content(schema = @io.swagger.v3.oas.annotations.media.Schema(example = "{\"timestamp\":\"2024-01-01T12:00:00\",\"message\":\"Comment not found\",\"path\":\"/api/posts/1/comments/1\"}"))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "Internal server error - Database or system failure",
                    content = @io.swagger.v3.oas.annotations.media.Content(schema = @io.swagger.v3.oas.annotations.media.Schema(example = "{\"timestamp\":\"2024-01-01T12:00:00\",\"message\":\"Failed to delete comment\",\"path\":\"/api/posts/1/comments/1\"}"))
            )
    })
    void deleteComment(
            @Parameter(description = "Post ID", required = true, example = "1")
            @PathVariable Long postId,
            @Parameter(description = "Comment ID", required = true, example = "1")
            @PathVariable String commentId
    );
}
