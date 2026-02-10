package org.amalitech.controllers;

import org.amalitech.dtos.CommentDto;
import org.amalitech.dtos.CreateCommentRequest;
import org.amalitech.dtos.UpdateCommentRequest;
import org.amalitech.mappers.CommentMapper;
import org.amalitech.models.Comment;
import org.amalitech.service.CommentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class CommentControllerTest {
    @Mock
    private CommentService commentService;
    @Mock
    private CommentMapper commentMapper;
    private CommentController controller;
    @BeforeEach
    void setUp() {
        controller = new CommentController(commentService, commentMapper);
    }
    @Test
    void createComment_createsComment() {
        CreateCommentRequest request = new CreateCommentRequest();
        Comment comment = new Comment();
        when(commentMapper.toEntity(request)).thenReturn(comment);
        doReturn(comment).when(commentService).save(comment);
        when(commentMapper.toDto(comment)).thenReturn(new CommentDto());
        ResponseEntity<CommentDto> response = controller.createComment(request);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }
    @Test
    void updateComment_updatesComment() {
        UpdateCommentRequest request = new UpdateCommentRequest();
        request.setBody("new body");
        Comment comment = new Comment();
        comment.setBody("old body");
        when(commentService.findById("id")).thenReturn(comment);
        doNothing().when(commentService).update(comment);
        when(commentMapper.toDto(comment)).thenReturn(new CommentDto());
        ResponseEntity<CommentDto> response = controller.updateComment("id", request);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(comment.getBody()).isEqualTo("new body");
    }
    @Test
    void deleteComment_deletesComment() {
        doNothing().when(commentService).deleteById("id");
        controller.deleteComment("id");
        verify(commentService).deleteById("id");
    }
}
