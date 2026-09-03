package org.amalitech.comment;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.amalitech.comment.api.CommentApi;
import org.amalitech.common.dto.CustomApiResponse;
import org.amalitech.comment.dto.CommentDto;
import org.amalitech.comment.dto.CreateCommentRequest;
import org.amalitech.comment.dto.UpdateCommentRequest;
import org.amalitech.comment.CommentMapper;
import org.amalitech.comment.Comment;
import org.amalitech.comment.CommentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/posts/{postId}/comments")
@RequiredArgsConstructor
public class CommentController implements CommentApi {

    private final CommentService commentService;
    private final CommentMapper commentMapper;

    @Override
    @PostMapping
    public ResponseEntity<CustomApiResponse<CommentDto>> createComment(
            @Valid @RequestBody CreateCommentRequest request,
            @PathVariable Long postId
            ) {
        Comment comment = commentMapper.toEntity(request);
        commentService.save(postId, comment);
        CommentDto dto = commentMapper.toDto(comment);
        return ResponseEntity.ok(CustomApiResponse.success(HttpStatus.CREATED, "Comment Added Successfully", dto));
    }

    @Override
    @GetMapping
    public ResponseEntity<CustomApiResponse<List<CommentDto>>> getComment(
            @PathVariable Long postId) {
        var comments = commentService.getCommentsByPostId(postId.intValue());
        var commentDto = comments.stream().map(commentMapper::toDto).toList();
        return ResponseEntity.ok(CustomApiResponse.success(commentDto));
    }

    @Override
    @PutMapping("/{commentId}")
    public ResponseEntity<CommentDto> updateComment(
            @PathVariable Long postId,
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
            @PathVariable Long postId,
            @PathVariable String commentId) {
        if (commentId == null || commentId.trim().isEmpty()) {
            throw new IllegalArgumentException("Comment ID cannot be null or empty");
        }
        commentService.deleteById(commentId);
    }
}