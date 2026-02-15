package org.amalitech.service;

import org.amalitech.entities.Comment;
import org.amalitech.exception.ResourceNotFoundException;
import org.amalitech.exception.ValidationException;
import org.amalitech.repositories.CommentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CommentService Tests")
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @InjectMocks
    private CommentService commentService;

    private Comment validComment;

    @BeforeEach
    void setUp() {
        validComment = new Comment();
        validComment.setId("comment-123");
        validComment.setPostId(1);
        validComment.setUsername("testuser");
        validComment.setBody("This is a valid comment body.");
    }

    // -------------------------------------------------------------------------
    // save()
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("save()")
    class Save {

        @Test
        @DisplayName("saves a valid comment and sets createdAt")
        void save_validComment_setsCreatedAtAndReturnsInserted() {
            when(commentRepository.insert(any(Comment.class))).thenReturn(validComment);

            Instant before = Instant.now();
            Comment result = commentService.save(validComment);
            Instant after = Instant.now();

            ArgumentCaptor<Comment> captor = ArgumentCaptor.forClass(Comment.class);
            verify(commentRepository).insert(captor.capture());

            Comment captured = captor.getValue();
            assertThat(captured.getCreatedAt()).isBetween(before, after);
            assertThat(result).isEqualTo(validComment);
        }

        @Test
        @DisplayName("throws ValidationException when postId is zero")
        void save_postIdZero_throwsValidationException() {
            validComment.setPostId(0);
            assertThatThrownBy(() -> commentService.save(validComment))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Invalid post ID");
            verifyNoInteractions(commentRepository);
        }

        @Test
        @DisplayName("throws ValidationException when postId is negative")
        void save_negativePostId_throwsValidationException() {
            validComment.setPostId(-5);
            assertThatThrownBy(() -> commentService.save(validComment))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Invalid post ID");
        }

        @Test
        @DisplayName("throws ValidationException when username is null")
        void save_nullUsername_throwsValidationException() {
            validComment.setUsername(null);
            assertThatThrownBy(() -> commentService.save(validComment))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Username cannot be empty");
        }

        @Test
        @DisplayName("throws ValidationException when username is blank")
        void save_blankUsername_throwsValidationException() {
            validComment.setUsername("   ");
            assertThatThrownBy(() -> commentService.save(validComment))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Username cannot be empty");
        }

        @Test
        @DisplayName("throws ValidationException when body is null")
        void save_nullBody_throwsValidationException() {
            validComment.setBody(null);
            assertThatThrownBy(() -> commentService.save(validComment))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Comment body cannot be empty");
        }

        @Test
        @DisplayName("throws ValidationException when body is blank")
        void save_blankBody_throwsValidationException() {
            validComment.setBody("   ");
            assertThatThrownBy(() -> commentService.save(validComment))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Comment body cannot be empty");
        }

        @Test
        @DisplayName("throws ValidationException when body exceeds 5000 characters")
        void save_bodyTooLong_throwsValidationException() {
            validComment.setBody("x".repeat(5001));
            assertThatThrownBy(() -> commentService.save(validComment))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Comment body cannot exceed 5000 characters");
        }

        @Test
        @DisplayName("accepts body of exactly 5000 characters")
        void save_bodyExactly5000Chars_savesSuccessfully() {
            validComment.setBody("x".repeat(5000));
            when(commentRepository.insert(any(Comment.class))).thenReturn(validComment);

            assertThatNoException().isThrownBy(() -> commentService.save(validComment));
            verify(commentRepository).insert(any(Comment.class));
        }
    }

    // -------------------------------------------------------------------------
    // getCommentById()
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("getCommentById()")
    class GetCommentById {

        @Test
        @DisplayName("returns comment when found")
        void getCommentById_found_returnsComment() {
            when(commentRepository.findById("comment-123")).thenReturn(Optional.of(validComment));

            Comment result = commentService.getCommentById("comment-123");

            assertThat(result).isEqualTo(validComment);
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when comment does not exist")
        void getCommentById_notFound_throwsResourceNotFoundException() {
            when(commentRepository.findById("missing")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> commentService.getCommentById("missing"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Comment not found with ID: missing");
        }

        @Test
        @DisplayName("throws IllegalArgumentException when id is null")
        void getCommentById_nullId_throwsIllegalArgumentException() {
            assertThatThrownBy(() -> commentService.getCommentById(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Comment ID cannot be null or empty");
            verifyNoInteractions(commentRepository);
        }

        @Test
        @DisplayName("throws IllegalArgumentException when id is blank")
        void getCommentById_blankId_throwsIllegalArgumentException() {
            assertThatThrownBy(() -> commentService.getCommentById("   "))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Comment ID cannot be null or empty");
            verifyNoInteractions(commentRepository);
        }

        @Test
        @DisplayName("throws IllegalArgumentException when id is empty string")
        void getCommentById_emptyId_throwsIllegalArgumentException() {
            assertThatThrownBy(() -> commentService.getCommentById(""))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Comment ID cannot be null or empty");
        }
    }

    // -------------------------------------------------------------------------
    // getCommentsByPostId()
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("getCommentsByPostId()")
    class GetCommentsByPostId {

        @Test
        @DisplayName("returns comments for a valid postId")
        void getCommentsByPostId_validId_returnsList() {
            List<Comment> expected = List.of(validComment);
            when(commentRepository.findByPostId(1)).thenReturn(expected);

            List<Comment> result = commentService.getCommentsByPostId(1);

            assertThat(result).containsExactlyElementsOf(expected);
        }

        @Test
        @DisplayName("returns empty list when no comments exist for postId")
        void getCommentsByPostId_noComments_returnsEmptyList() {
            when(commentRepository.findByPostId(99)).thenReturn(List.of());

            List<Comment> result = commentService.getCommentsByPostId(99);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("throws ValidationException when postId is zero")
        void getCommentsByPostId_zero_throwsValidationException() {
            assertThatThrownBy(() -> commentService.getCommentsByPostId(0))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Invalid post ID");
            verifyNoInteractions(commentRepository);
        }

        @Test
        @DisplayName("throws ValidationException when postId is negative")
        void getCommentsByPostId_negative_throwsValidationException() {
            assertThatThrownBy(() -> commentService.getCommentsByPostId(-1))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Invalid post ID");
        }
    }

    // -------------------------------------------------------------------------
    // update()
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("update()")
    class Update {

        @Test
        @DisplayName("updates a valid comment")
        void update_validComment_callsRepositorySave() {
            commentService.update(validComment);
            verify(commentRepository).save(validComment);
        }

        @Test
        @DisplayName("throws IllegalArgumentException when comment is null")
        void update_nullComment_throwsIllegalArgumentException() {
            assertThatThrownBy(() -> commentService.update(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Comment or ID cannot be null");
            verifyNoInteractions(commentRepository);
        }

        @Test
        @DisplayName("throws IllegalArgumentException when comment id is null")
        void update_commentWithNullId_throwsIllegalArgumentException() {
            validComment.setId(null);
            assertThatThrownBy(() -> commentService.update(validComment))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Comment or ID cannot be null");
            verifyNoInteractions(commentRepository);
        }

        @Test
        @DisplayName("throws ValidationException when comment body is empty on update")
        void update_emptyBody_throwsValidationException() {
            validComment.setBody("");
            assertThatThrownBy(() -> commentService.update(validComment))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Comment body cannot be empty");
        }

        @Test
        @DisplayName("throws ValidationException when body exceeds 5000 characters on update")
        void update_bodyTooLong_throwsValidationException() {
            validComment.setBody("a".repeat(5001));
            assertThatThrownBy(() -> commentService.update(validComment))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Comment body cannot exceed 5000 characters");
        }
    }

    // -------------------------------------------------------------------------
    // deleteById()
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("deleteById()")
    class DeleteById {

        @Test
        @DisplayName("delegates to repository deleteById")
        void deleteById_callsRepository() {
            commentService.deleteById("comment-123");
            verify(commentRepository).deleteById("comment-123");
        }

        @Test
        @DisplayName("does not throw even when comment does not exist (repository handles it)")
        void deleteById_nonExistent_noException() {
            doNothing().when(commentRepository).deleteById(anyString());
            assertThatNoException().isThrownBy(() -> commentService.deleteById("ghost-id"));
        }
    }

    // -------------------------------------------------------------------------
    // deleteByPostId()
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("deleteByPostId()")
    class DeleteByPostId {

        @Test
        @DisplayName("delegates to repository deleteByPostId for valid id")
        void deleteByPostId_valid_callsRepository() {
            commentService.deleteByPostId(1);
            verify(commentRepository).deleteByPostId(1);
        }

        @Test
        @DisplayName("throws ValidationException when postId is zero")
        void deleteByPostId_zero_throwsValidationException() {
            assertThatThrownBy(() -> commentService.deleteByPostId(0))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Invalid post ID");
            verifyNoInteractions(commentRepository);
        }

        @Test
        @DisplayName("throws ValidationException when postId is negative")
        void deleteByPostId_negative_throwsValidationException() {
            assertThatThrownBy(() -> commentService.deleteByPostId(-10))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Invalid post ID");
        }
    }
}