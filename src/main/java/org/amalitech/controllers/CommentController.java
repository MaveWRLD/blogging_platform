package org.amalitech.controllers;

import jakarta.validation.Valid;
import org.amalitech.dtos.CommentDto;
import org.amalitech.dtos.CreateCommentRequest;
import org.amalitech.dtos.UpdateCommentRequest;
import org.amalitech.mappers.CommentMapper;
import org.amalitech.models.Comment;
import org.amalitech.service.CommentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/comments")
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
    public ResponseEntity<CommentDto> createComment(@Valid @RequestBody CreateCommentRequest request) {
        Comment comment = commentMapper.toEntity(request);
        commentService.save(comment);
        CommentDto dto = commentMapper.toDto(comment);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    /**
     * Update a comment's body
     */
    @PutMapping("/{commentId}")
    public ResponseEntity<CommentDto> updateComment(
            @PathVariable String commentId,
            @Valid @RequestBody UpdateCommentRequest request) {

        Comment existing = commentService.findById(commentId);

        if (request.getBody() != null) {
            existing.setBody(request.getBody());
        }

        commentService.update(existing);
        return ResponseEntity.ok(commentMapper.toDto(existing));
    }

    /**
     * Delete a single comment by its ObjectId
     */
    @DeleteMapping("/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(@PathVariable String commentId) {
        commentService.deleteById(commentId);
    }
}