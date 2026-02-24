package org.amalitech.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.amalitech.dtos.CommentDto;
import org.amalitech.dtos.CreateCommentRequest;
import org.amalitech.dtos.UpdateCommentRequest;
import org.amalitech.entities.Comment;
import org.amalitech.exception.CustomExceptionHandler;
import org.amalitech.exception.ValidationException;
import org.amalitech.mappers.CommentMapper;
import org.amalitech.service.CommentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CommentControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private CommentService commentService;

    @Mock
    private CommentMapper commentMapper;

    @InjectMocks
    private CommentController commentController;

    private Comment testComment;
    private CommentDto testCommentDto;
    private CreateCommentRequest createCommentRequest;
    private UpdateCommentRequest updateCommentRequest;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(commentController)
                .setControllerAdvice(new CustomExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();

        testComment = new Comment();
        testComment.setId("comment-123");
        testComment.setPostId(1L);
        testComment.setUsername("testuser");
        testComment.setBody("Test comment content");
        testComment.setCreatedAt(Instant.now());

        testCommentDto = new CommentDto();
        testCommentDto.setId("comment-123");
        testCommentDto.setPostId(1L);
        testCommentDto.setUsername("testuser");
        testCommentDto.setBody("Test comment content");
        testCommentDto.setCreatedAt(testComment.getCreatedAt());

        createCommentRequest = new CreateCommentRequest();
        createCommentRequest.setPostId(1L);
        createCommentRequest.setBody("Test comment content");

        updateCommentRequest = new UpdateCommentRequest();
        updateCommentRequest.setBody("Updated comment content");
    }

    @Test
    void createComment_WithValidRequest_ShouldReturnCreatedComment() throws Exception {
        when(commentMapper.toEntity(any(CreateCommentRequest.class))).thenReturn(testComment);
        when(commentService.save(any(Comment.class))).thenReturn(testComment);
        when(commentMapper.toDto(any(Comment.class))).thenReturn(testCommentDto);

        mockMvc.perform(post("/api/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createCommentRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.message").value("Comment Added Successfully"))
                .andExpect(jsonPath("$.data.id").value("comment-123"))
                .andExpect(jsonPath("$.data.body").value("Test comment content"));

        verify(commentMapper).toEntity(createCommentRequest);
        verify(commentService).save(testComment);
        verify(commentMapper).toDto(testComment);
    }

    @Test
    void createComment_WithInvalidRequest_ShouldReturnBadRequest() throws Exception {
        CreateCommentRequest invalidRequest = new CreateCommentRequest();
        invalidRequest.setPostId(0L);
        invalidRequest.setBody("");

        Comment invalidComment = new Comment();
        invalidComment.setPostId(0L);
        invalidComment.setBody("");
        
        when(commentMapper.toEntity(invalidRequest)).thenReturn(invalidComment);
        when(commentService.save(invalidComment)).thenThrow(new ValidationException("Invalid post ID"));

        mockMvc.perform(post("/api/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getCommentsByPostId_WithValidPostId_ShouldReturnComments() throws Exception {
        List<Comment> comments = Arrays.asList(testComment);

        when(commentService.getCommentsByPostId(1)).thenReturn(comments);
        when(commentMapper.toDto(any(Comment.class))).thenReturn(testCommentDto);

        mockMvc.perform(get("/api/comments/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].id").value("comment-123"))
                .andExpect(jsonPath("$.data[0].postId").value(1))
                .andExpect(jsonPath("$.data[0].body").value("Test comment content"));

        verify(commentService).getCommentsByPostId(1);
        verify(commentMapper, times(1)).toDto(testComment);
    }

    @Test
    void getCommentsByPostId_WithInvalidPostId_ShouldReturnBadRequest() throws Exception {
        when(commentService.getCommentsByPostId(-1))
                .thenThrow(new IllegalArgumentException("Invalid post ID"));

        mockMvc.perform(get("/api/comments/-1"))
                .andExpect(status().isBadRequest());

        verify(commentService).getCommentsByPostId(-1);
    }

    @Test
    void getCommentsByPostId_WithNonExistentPost_ShouldReturnNotFound() throws Exception {
        when(commentService.getCommentsByPostId(999))
                .thenThrow(new RuntimeException("Post not found"));

        mockMvc.perform(get("/api/comments/999"))
                .andExpect(status().isNotFound());

        verify(commentService).getCommentsByPostId(999);
    }

    @Test
    void updateComment_WithValidRequest_ShouldReturnUpdatedComment() throws Exception {
        Comment updatedComment = new Comment();
        updatedComment.setId("comment-123");
        updatedComment.setPostId(1L);
        updatedComment.setUsername("testuser");
        updatedComment.setBody("Updated comment content");
        updatedComment.setCreatedAt(Instant.now());

        CommentDto updatedCommentDto = new CommentDto();
        updatedCommentDto.setId("comment-123");
        updatedCommentDto.setPostId(1L);
        updatedCommentDto.setUsername("testuser");
        updatedCommentDto.setBody("Updated comment content");
        updatedCommentDto.setCreatedAt(updatedComment.getCreatedAt());

        when(commentService.getCommentById("comment-123")).thenReturn(testComment);
        when(commentMapper.toDto(any(Comment.class))).thenReturn(updatedCommentDto);

        mockMvc.perform(put("/api/comments/comment-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateCommentRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("comment-123"))
                .andExpect(jsonPath("$.body").value("Updated comment content"));

        verify(commentService).getCommentById("comment-123");
        verify(commentService).update(any(Comment.class));
    }

    @Test
    void updateComment_WithNonExistentComment_ShouldReturnNotFound() throws Exception {
        when(commentService.getCommentById("nonexistent"))
                .thenThrow(new RuntimeException("Comment not found"));

        mockMvc.perform(put("/api/comments/nonexistent")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateCommentRequest)))
                .andExpect(status().isNotFound());

        verify(commentService).getCommentById("nonexistent");
    }

    @Test
    void deleteComment_WithValidCommentId_ShouldDeleteComment() throws Exception {
        doNothing().when(commentService).deleteById("comment-123");

        mockMvc.perform(delete("/api/comments/comment-123"))
                .andExpect(status().isNoContent());

        verify(commentService).deleteById("comment-123");
    }

    @Test
    void deleteComment_WithNonExistentComment_ShouldReturnNotFound() throws Exception {
        doThrow(new RuntimeException("Comment not found"))
                .when(commentService).deleteById("nonexistent");

        mockMvc.perform(delete("/api/comments/nonexistent"))
                .andExpect(status().isNotFound());

        verify(commentService).deleteById("nonexistent");
    }

    @Test
    void deleteComment_WithEmptyCommentId_ShouldReturnBadRequest() throws Exception {
        mockMvc.perform(delete("/api/comments/ "))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createComment_WithNullBody_ShouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/api/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getCommentsByPostId_WithZeroPostId_ShouldReturnBadRequest() throws Exception {
        when(commentService.getCommentsByPostId(0))
                .thenThrow(new IllegalArgumentException("Invalid post ID"));

        mockMvc.perform(get("/api/comments/0"))
                .andExpect(status().isBadRequest());

        verify(commentService).getCommentsByPostId(0);
    }
}
