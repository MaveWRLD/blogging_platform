package org.amalitech.controllers;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.amalitech.api.doc.CommentApi;
import org.amalitech.dtos.CustomApiResponse;
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
@AllArgsConstructor
public class CommentController implements CommentApi {

    private final CommentService commentService;
    private final CommentMapper commentMapper;

    @Override
    @PostMapping
    public ResponseEntity<CustomApiResponse<CommentDto>> createComment(
            @Valid @RequestBody CreateCommentRequest request) {
        Comment comment = commentMapper.toEntity(request);
        commentService.save(comment);
        CommentDto dto = commentMapper.toDto(comment);
        return ResponseEntity.ok(CustomApiResponse.success(HttpStatus.CREATED, "Comment Added Successfully", dto));
    }

    @Override
    @GetMapping("/{postId}")
    public ResponseEntity<CustomApiResponse<List<CommentDto>>> getComment(
            @PathVariable int postId) {
        var comments = commentService.getCommentsByPostId(postId);
        var commentDto = comments.stream().map(commentMapper::toDto).toList();
        return ResponseEntity.ok(CustomApiResponse.success(commentDto));
    }

    @Override
    @PutMapping("/{commentId}")
    public ResponseEntity<CommentDto> updateComment(
            @PathVariable String commentId,
            @Valid @RequestBody UpdateCommentRequest request) {

        Comment existing = commentService.getCommentById(commentId);

        existing.setBody(request.getBody());

        commentService.update(existing);
        return ResponseEntity.ok(commentMapper.toDto(existing));
    }

    @Override
    @DeleteMapping("/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(
            @PathVariable String commentId) {
        if (commentId == null || commentId.trim().isEmpty()) {
            throw new IllegalArgumentException("Comment ID cannot be null or empty");
        }
        commentService.deleteById(commentId);
    }
}