package org.amalitech.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.amalitech.dtos.ApiResponse;
import org.amalitech.dtos.CommentDto;
import org.amalitech.dtos.CreateCommentRequest;
import org.amalitech.dtos.UpdateCommentRequest;
import org.amalitech.mappers.CommentMapper;
import org.amalitech.entities.Comment;
import org.amalitech.service.CommentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/comments")
@Tag(
        name = "Comments",
        description = "Endpoints for managing comments and replies"
)
public class CommentController {

    private final CommentService commentService;
    private final CommentMapper commentMapper;

    public CommentController(CommentService commentService, CommentMapper commentMapper) {
        this.commentService = commentService;
        this.commentMapper = commentMapper;
    }

    /**
     * Create a new comment (top-level or reply)
     */
    @PostMapping
    @Operation(
            summary = "Create a new comment",
            description = "Create a new comment. If parentId is provided, the comment will be a reply to the specified parent comment."
    )
    public ResponseEntity<ApiResponse<CommentDto>> createComment(@Valid @RequestBody CreateCommentRequest request) {
        Comment comment = commentMapper.toEntity(request);
        commentService.save(comment);
        CommentDto dto = commentMapper.toDto(comment);
         return ResponseEntity.ok(ApiResponse.success(HttpStatus.CREATED, "Comment Added Successfully", dto));
    }

    @GetMapping("/{postId}")
    @Operation(
            summary = "Find a comment by id",
            description = "Create a new comment. If parentId is provided, the comment will be a reply to the specified parent comment."
    )
    public ResponseEntity<ApiResponse<List<CommentDto>>> getComment(@PathVariable int postId) {
        var comments = commentService.getCommentsByPostId(postId);
        var commentDto = comments.stream().map(commentMapper::toDto).toList();
        return ResponseEntity.ok(ApiResponse.success(commentDto));
    }

    /**
     * Update a comment's body
     */
    @PutMapping("/{commentId}")
    @Operation(
            summary = "Update a comment",
            description = "Update the body of an existing comment. Only the body can be updated."
    )
    public ResponseEntity<CommentDto> updateComment(
            @PathVariable String commentId,
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
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Delete a comment",
            description = "Delete a comment by its ID. If the comment has replies, they will also be deleted."
    )
    public void deleteComment(@PathVariable String commentId) {
        commentService.deleteById(commentId);
    }
}