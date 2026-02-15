package org.amalitech.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.amalitech.dtos.CommentDto;
import org.amalitech.dtos.CreateCommentRequest;
import org.amalitech.dtos.UpdateCommentRequest;
import org.amalitech.entities.Comment;
import org.amalitech.exception.ResourceNotFoundException;
import org.amalitech.mappers.CommentMapper;
import org.amalitech.service.CommentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CommentController.class)
@DisplayName("CommentController Tests")
class CommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CommentService commentService;

    @MockitoBean
    private CommentMapper commentMapper;

    private Comment sampleComment;
    private CommentDto sampleCommentDto;

    @BeforeEach
    void setUp() {
        sampleComment = new Comment();
        sampleComment.setId("comment-abc");
        sampleComment.setPostId(1);
        sampleComment.setUsername("johndoe");
        sampleComment.setBody("A great post!");
        sampleComment.setCreatedAt(Instant.parse("2024-06-01T10:00:00Z"));

        sampleCommentDto = new CommentDto();
        sampleCommentDto.setId("comment-abc");
        sampleCommentDto.setUsername("johndoe");
        sampleCommentDto.setBody("A great post!");
    }

    // -------------------------------------------------------------------------
    // POST /api/comments
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("POST /api/comments")
    class CreateComment {

        @Test
        @DisplayName("returns 200 with created comment DTO on success")
        void validRequest_returns200WithDto() throws Exception {
            CreateCommentRequest request = new CreateCommentRequest();
            request.setPostId(1);
            request.setUsername("johndoe");
            request.setBody("A great post!");

            when(commentMapper.toEntity(any(CreateCommentRequest.class))).thenReturn(sampleComment);
            when(commentService.save(any(Comment.class))).thenReturn(sampleComment);
            when(commentMapper.toDto(any(Comment.class))).thenReturn(sampleCommentDto);

            mockMvc.perform(post("/api/comments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Comment Added Successfully"))
                    .andExpect(jsonPath("$.data.id").value("comment-abc"))
                    .andExpect(jsonPath("$.data.username").value("johndoe"))
                    .andExpect(jsonPath("$.data.body").value("A great post!"));

            verify(commentService).save(sampleComment);
        }

        @Test
        @DisplayName("calls mapper to convert request to entity and entity to DTO")
        void validRequest_invokesMapperBothWays() throws Exception {
            CreateCommentRequest request = new CreateCommentRequest();
            request.setPostId(1);
            request.setUsername("johndoe");
            request.setBody("A great post!");

            when(commentMapper.toEntity(any(CreateCommentRequest.class))).thenReturn(sampleComment);
            when(commentMapper.toDto(any(Comment.class))).thenReturn(sampleCommentDto);

            mockMvc.perform(post("/api/comments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());

            verify(commentMapper).toEntity(any(CreateCommentRequest.class));
            verify(commentMapper).toDto(sampleComment);
        }

        @Test
        @DisplayName("returns 400 when request body is missing")
        void missingBody_returns400() throws Exception {
            mockMvc.perform(post("/api/comments")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());
        }
    }

    // -------------------------------------------------------------------------
    // GET /api/comments/{postId}
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("GET /api/comments/{postId}")
    class GetCommentsByPostId {

        @Test
        @DisplayName("returns 200 with list of comment DTOs")
        void validPostId_returns200WithList() throws Exception {
            when(commentService.getCommentsByPostId(1)).thenReturn(List.of(sampleComment));
            when(commentMapper.toDto(sampleComment)).thenReturn(sampleCommentDto);

            mockMvc.perform(get("/api/comments/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(1)))
                    .andExpect(jsonPath("$.data[0].id").value("comment-abc"))
                    .andExpect(jsonPath("$.data[0].username").value("johndoe"));
        }

        @Test
        @DisplayName("returns 200 with empty list when no comments exist")
        void noComments_returns200WithEmptyList() throws Exception {
            when(commentService.getCommentsByPostId(42)).thenReturn(List.of());

            mockMvc.perform(get("/api/comments/42"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(0)));
        }

        @Test
        @DisplayName("returns 200 with multiple comments mapped correctly")
        void multipleComments_returnsAll() throws Exception {
            Comment second = new Comment();
            second.setId("comment-xyz");
            CommentDto secondDto = new CommentDto();
            secondDto.setId("comment-xyz");

            when(commentService.getCommentsByPostId(1)).thenReturn(List.of(sampleComment, second));
            when(commentMapper.toDto(sampleComment)).thenReturn(sampleCommentDto);
            when(commentMapper.toDto(second)).thenReturn(secondDto);

            mockMvc.perform(get("/api/comments/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(2)));
        }
    }

    // -------------------------------------------------------------------------
    // PUT /api/comments/{commentId}
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("PUT /api/comments/{commentId}")
    class UpdateComment {

        @Test
        @DisplayName("returns 200 with updated comment DTO")
        void validUpdate_returns200WithDto() throws Exception {
            UpdateCommentRequest request = new UpdateCommentRequest();
            request.setBody("Updated body text.");

            when(commentService.getCommentById("comment-abc")).thenReturn(sampleComment);
            when(commentMapper.toDto(sampleComment)).thenReturn(sampleCommentDto);

            mockMvc.perform(put("/api/comments/comment-abc")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value("comment-abc"));

            verify(commentService).getCommentById("comment-abc");
            verify(commentService).update(sampleComment);
        }

        @Test
        @DisplayName("sets new body on existing comment before updating")
        void validUpdate_setsBodyOnEntity() throws Exception {
            UpdateCommentRequest request = new UpdateCommentRequest();
            request.setBody("Updated body text.");

            when(commentService.getCommentById("comment-abc")).thenReturn(sampleComment);
            when(commentMapper.toDto(any(Comment.class))).thenReturn(sampleCommentDto);

            mockMvc.perform(put("/api/comments/comment-abc")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());

            verify(commentService).update(argThat(c -> "Updated body text.".equals(c.getBody())));
        }

        @Test
        @DisplayName("returns 404 when comment to update is not found")
        void commentNotFound_returns404() throws Exception {
            UpdateCommentRequest request = new UpdateCommentRequest();
            request.setBody("Something");

            when(commentService.getCommentById("missing-id"))
                    .thenThrow(new ResourceNotFoundException("Comment not found with ID: missing-id"));

            mockMvc.perform(put("/api/comments/missing-id")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("returns 400 when request body is missing")
        void missingBody_returns400() throws Exception {
            mockMvc.perform(put("/api/comments/comment-abc")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());
        }
    }

    // -------------------------------------------------------------------------
    // DELETE /api/comments/{commentId}
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("DELETE /api/comments/{commentId}")
    class DeleteComment {

        @Test
        @DisplayName("returns 204 on successful deletion")
        void existingComment_returns204() throws Exception {
            doNothing().when(commentService).deleteById("comment-abc");

            mockMvc.perform(delete("/api/comments/comment-abc"))
                    .andExpect(status().isNoContent());

            verify(commentService).deleteById("comment-abc");
        }

        @Test
        @DisplayName("propagates service exception when comment not found")
        void notFound_propagatesException() throws Exception {
            doThrow(new ResourceNotFoundException("Comment not found with ID: ghost"))
                    .when(commentService).deleteById("ghost");

            mockMvc.perform(delete("/api/comments/ghost"))
                    .andExpect(status().isNotFound());
        }
    }
}