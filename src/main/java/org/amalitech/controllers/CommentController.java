package org.amalitech.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.amalitech.dtos.ApiResponse;
import org.amalitech.dtos.CommentDto;
import org.amalitech.dtos.CreateCommentRequest;
import org.amalitech.dtos.UpdateCommentRequest;
import org.amalitech.mappers.CommentMapper;
import org.amalitech.entities.Comment;
import org.amalitech.service.CommentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/comments")
@Tag(
        name = "Comments",
        description = "Endpoints for managing comments and replies"
)
@SecurityRequirement(name = "bearerAuth")
@AllArgsConstructor
public class CommentController {

    private final CommentService commentService;
    private final CommentMapper commentMapper;

    /**
     * Create a new comment
     */
    @PostMapping
    @Operation(
            summary = "Create a new comment",
            description = "Create a new comment. If parentId is provided, the comment will be a reply to the specified parent comment. Requires authentication."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "Comment created successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Bad request - Invalid comment data or missing required fields",
                    content = @Content(schema = @Schema(example = "{\"timestamp\":\"2024-01-01T12:00:00\",\"errors\":{\"body\":\"Comment body is required\",\"postId\":\"Post ID is required\"},\"message\":\"Validation failed\"}"))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Authentication required",
                    content = @Content(schema = @Schema(example = "{\"error\":\"Unauthorized\",\"message\":\"Full authentication is required to access this resource\",\"path\":\"/api/comments\"}"))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - User does not have permission to comment",
                    content = @Content(schema = @Schema(example = "{\"error\":\"Forbidden\",\"message\":\"You do not have permission to comment\",\"path\":\"/api/comments\"}"))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Post not found - The post to comment on does not exist",
                    content = @Content(schema = @Schema(example = "{\"timestamp\":\"2024-01-01T12:00:00\",\"message\":\"Post not found\",\"path\":\"/api/comments\"}"))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "422",
                    description = "Unprocessable Entity - Validation errors in request body",
                    content = @Content(schema = @Schema(example = "{\"timestamp\":\"2024-01-01T12:00:00\",\"errors\":{\"body\":\"Comment body must be between 1 and 1000 characters\"},\"message\":\"Validation failed\"}"))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "Internal server error - Database or system failure",
                    content = @Content(schema = @Schema(example = "{\"timestamp\":\"2024-01-01T12:00:00\",\"message\":\"Failed to create comment\",\"path\":\"/api/comments\"}"))
            )
    })
    public ResponseEntity<ApiResponse<CommentDto>> createComment(
            @Parameter(description = "Comment creation request", required = true)
            @Valid @RequestBody CreateCommentRequest request) {
        Comment comment = commentMapper.toEntity(request);
        commentService.save(comment);
        CommentDto dto = commentMapper.toDto(comment);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.CREATED, "Comment Added Successfully", dto));
    }

    @GetMapping("/{postId}")
    @Operation(
            summary = "Get comments by post ID",
            description = "Retrieve all comments for a specific post. No authentication required."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Comments retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Bad request - Invalid post ID",
                    content = @Content(schema = @Schema(example = "{\"timestamp\":\"2024-01-01T12:00:00\",\"message\":\"Invalid post ID\",\"path\":\"/api/comments/invalid\"}"))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Post not found",
                    content = @Content(schema = @Schema(example = "{\"timestamp\":\"2024-01-01T12:00:00\",\"message\":\"Post not found\",\"path\":\"/api/comments/999\"}"))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "Internal server error - Database or system failure",
                    content = @Content(schema = @Schema(example = "{\"timestamp\":\"2024-01-01T12:00:00\",\"message\":\"Failed to retrieve comments\",\"path\":\"/api/comments/1\"}"))
            )
    })
    public ResponseEntity<ApiResponse<List<CommentDto>>> getComment(
            @Parameter(description = "Post ID to retrieve comments for", required = true, example = "1")
            @PathVariable int postId) {
        var comments = commentService.getCommentsByPostId(postId);
        var commentDto = comments.stream().map(commentMapper::toDto).toList();
        return ResponseEntity.ok(ApiResponse.success(commentDto));
    }

    /**
     * Update a comment's body
     */
    @PutMapping("/{commentId}")
    @PreAuthorize("@authorizationService.canUpdateComment(#commentId)")
    @Operation(
            summary = "Update a comment",
            description = "Update the body of an existing comment. Only the body can be updated. Requires authentication and proper authorization."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Comment updated successfully",
                    content = @Content(schema = @Schema(implementation = CommentDto.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Bad request - Invalid comment ID or update data",
                    content = @Content(schema = @Schema(example = "{\"timestamp\":\"2024-01-01T12:00:00\",\"message\":\"Invalid comment ID\",\"path\":\"/api/comments/invalid\"}"))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Authentication required",
                    content = @Content(schema = @Schema(example = "{\"error\":\"Unauthorized\",\"message\":\"Full authentication is required to access this resource\",\"path\":\"/api/comments/1\"}"))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - User does not have permission to update this comment",
                    content = @Content(schema = @Schema(example = "{\"error\":\"Forbidden\",\"message\":\"You can only update your own comments\",\"path\":\"/api/comments/1\"}"))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Comment not found",
                    content = @Content(schema = @Schema(example = "{\"timestamp\":\"2024-01-01T12:00:00\",\"message\":\"Comment not found\",\"path\":\"/api/comments/1\"}"))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "422",
                    description = "Unprocessable Entity - Validation errors in request body",
                    content = @Content(schema = @Schema(example = "{\"timestamp\":\"2024-01-01T12:00:00\",\"errors\":{\"body\":\"Comment body must be between 1 and 1000 characters\"},\"message\":\"Validation failed\"}"))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "Internal server error - Database or system failure",
                    content = @Content(schema = @Schema(example = "{\"timestamp\":\"2024-01-01T12:00:00\",\"message\":\"Failed to update comment\",\"path\":\"/api/comments/1\"}"))
            )
    })
    public ResponseEntity<CommentDto> updateComment(
            @Parameter(description = "Comment ID", required = true, example = "1")
            @PathVariable String commentId,
            @Parameter(description = "Comment update request", required = true)
            @Valid @RequestBody UpdateCommentRequest request) {

        Comment existing = commentService.getCommentById(commentId);

        existing.setBody(request.getBody());

        commentService.update(existing);
        return ResponseEntity.ok(commentMapper.toDto(existing));
    }

    /**
     * Delete a single comment by its ObjectId
     */
    @DeleteMapping("/{commentId}")
    @PreAuthorize("@authorizationService.canDeleteComment(#commentId)")
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
                    content = @Content(schema = @Schema(example = "{\"timestamp\":\"2024-01-01T12:00:00\",\"message\":\"Invalid comment ID\",\"path\":\"/api/comments/invalid\"}"))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Authentication required",
                    content = @Content(schema = @Schema(example = "{\"error\":\"Unauthorized\",\"message\":\"Full authentication is required to access this resource\",\"path\":\"/api/comments/1\"}"))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - User does not have permission to delete this comment",
                    content = @Content(schema = @Schema(example = "{\"error\":\"Forbidden\",\"message\":\"You can only delete your own comments\",\"path\":\"/api/comments/1\"}"))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Comment not found",
                    content = @Content(schema = @Schema(example = "{\"timestamp\":\"2024-01-01T12:00:00\",\"message\":\"Comment not found\",\"path\":\"/api/comments/1\"}"))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "Internal server error - Database or system failure",
                    content = @Content(schema = @Schema(example = "{\"timestamp\":\"2024-01-01T12:00:00\",\"message\":\"Failed to delete comment\",\"path\":\"/api/comments/1\"}"))
            )
    })
    public void deleteComment(
            @Parameter(description = "Comment ID", required = true, example = "1")
            @PathVariable String commentId) {
        if (commentId == null || commentId.trim().isEmpty()) {
            throw new IllegalArgumentException("Comment ID cannot be null or empty");
        }
        commentService.deleteById(commentId);
    }
}