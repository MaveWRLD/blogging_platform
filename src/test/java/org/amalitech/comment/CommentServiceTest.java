package org.amalitech.comment;

import org.amalitech.comment.Comment;
import org.amalitech.user.User;
import org.amalitech.exception.ResourceNotFoundException;
import org.amalitech.exception.ValidationException;
import org.amalitech.comment.CommentRepository;
import org.amalitech.post.PostExistenceChecker;
import org.amalitech.user.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CommentService Tests")
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private UserService userService;

    @Mock
    private PostExistenceChecker postExistenceChecker;

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

    @InjectMocks
    private CommentService commentService;

    private Comment validComment;
    private User mockUser;

    @BeforeEach
    void setUp() {
        validComment = new Comment();
        validComment.setId("comment-123");
        validComment.setPostId(1L);
        validComment.setBody("This is a valid comment body.");

        mockUser = mock(User.class);
    }

    // -------------------------------------------------------------------------
    // save()
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("save()")
    class Save {

        @BeforeEach
        void setupSecurity() {
            lenient().when(mockUser.getUsername()).thenReturn("testuser");

            lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
            SecurityContextHolder.setContext(securityContext);

            lenient().when(authentication.getPrincipal()).thenReturn(42L);

            lenient().when(userService.findByUserId(42L)).thenReturn(mockUser);

            lenient().when(postExistenceChecker.existsById(anyInt())).thenReturn(true);
        }

        @AfterEach
        void clearSecurity() {
            SecurityContextHolder.clearContext();
        }

        @Test
        @DisplayName("saves a valid comment and sets createdAt")
        void save_validComment_setsCreatedAtAndReturnsInserted() {

            when(commentRepository.insert(any(Comment.class))).thenReturn(validComment);

            Instant before = Instant.now();
            Comment result = commentService.save(1L, validComment);
            Instant after = Instant.now();

            ArgumentCaptor<Comment> captor = ArgumentCaptor.forClass(Comment.class);
            verify(commentRepository).insert(captor.capture());

            Comment captured = captor.getValue();

            assertThat(captured.getCreatedAt()).isBetween(before, after);
            assertThat(captured.getUsername()).isEqualTo("testuser");
            assertThat(result).isEqualTo(validComment);
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when target post does not exist")
        void save_postDoesNotExist_throwsResourceNotFoundException() {

            when(postExistenceChecker.existsById(99)).thenReturn(false);

            assertThatThrownBy(() -> commentService.save(99L, validComment))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Post not found with ID: 99");

            verifyNoInteractions(commentRepository);
        }

        @Test
        @DisplayName("throws ValidationException when postId is zero")
        void save_postIdZero_throwsValidationException() {

            validComment.setPostId(0L);

            assertThatThrownBy(() -> commentService.save(1L, validComment))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Invalid post ID");

            verifyNoInteractions(commentRepository);
        }

        @Test
        @DisplayName("throws ValidationException when postId is negative")
        void save_negativePostId_throwsValidationException() {

            validComment.setPostId(-5L);

            assertThatThrownBy(() -> commentService.save(1L, validComment))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Invalid post ID");
        }

        @Test
        @DisplayName("throws ValidationException when username is null")
        void save_nullUsername_throwsValidationException() {

            when(mockUser.getUsername()).thenReturn(null);

            assertThatThrownBy(() -> commentService.save(1L, validComment))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Username cannot be empty");
        }

        @Test
        @DisplayName("throws ValidationException when username is blank")
        void save_blankUsername_throwsValidationException() {

            when(mockUser.getUsername()).thenReturn("   ");

            assertThatThrownBy(() -> commentService.save(1L, validComment))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Username cannot be empty");
        }

        @Test
        @DisplayName("throws ValidationException when body is null")
        void save_nullBody_throwsValidationException() {

            validComment.setBody(null);

            assertThatThrownBy(() -> commentService.save(1L, validComment))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Comment body cannot be empty");
        }

        @Test
        @DisplayName("throws ValidationException when body is blank")
        void save_blankBody_throwsValidationException() {

            validComment.setBody("   ");

            assertThatThrownBy(() -> commentService.save(1L, validComment))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Comment body cannot be empty");
        }

        @Test
        @DisplayName("throws ValidationException when body exceeds 5000 characters")
        void save_bodyTooLong_throwsValidationException() {

            validComment.setBody("x".repeat(5001));

            assertThatThrownBy(() -> commentService.save(1L, validComment))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Comment body cannot exceed 5000 characters");
        }

        @Test
        @DisplayName("accepts body of exactly 5000 characters")
        void save_bodyExactly5000Chars_savesSuccessfully() {

            validComment.setBody("x".repeat(5000));

            when(commentRepository.insert(any(Comment.class))).thenReturn(validComment);

            assertThatNoException().isThrownBy(() -> commentService.save(1L, validComment));

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
        void getCommentById_found_returnsComment() {

            when(commentRepository.findById("comment-123")).thenReturn(Optional.of(validComment));

            Comment result = commentService.getCommentById("comment-123");

            assertThat(result).isEqualTo(validComment);
        }

        @Test
        void getCommentById_notFound_throwsResourceNotFoundException() {

            when(commentRepository.findById("missing")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> commentService.getCommentById("missing"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Comment not found with ID: missing");
        }

        @Test
        void getCommentById_nullId_throwsIllegalArgumentException() {

            assertThatThrownBy(() -> commentService.getCommentById(null))
                    .isInstanceOf(IllegalArgumentException.class);

            verifyNoInteractions(commentRepository);
        }

        @Test
        void getCommentById_blankId_throwsIllegalArgumentException() {

            assertThatThrownBy(() -> commentService.getCommentById("   "))
                    .isInstanceOf(IllegalArgumentException.class);

            verifyNoInteractions(commentRepository);
        }
    }

    // -------------------------------------------------------------------------
    // getCommentsByPostId()
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("getCommentsByPostId()")
    class GetCommentsByPostId {

        @Test
        void getCommentsByPostId_validId_returnsList() {

            List<Comment> expected = List.of(validComment);

            when(commentRepository.findByPostId(1)).thenReturn(expected);

            List<Comment> result = commentService.getCommentsByPostId(1);

            assertThat(result).containsExactlyElementsOf(expected);
        }

        @Test
        void getCommentsByPostId_zero_throwsValidationException() {

            assertThatThrownBy(() -> commentService.getCommentsByPostId(0))
                    .isInstanceOf(ValidationException.class);

            verifyNoInteractions(commentRepository);
        }
    }

    // -------------------------------------------------------------------------
    // update()
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("update()")
    class Update {

        @Test
        void update_validComment_callsRepositorySave() {

            commentService.update(validComment);

            verify(commentRepository).save(validComment);
        }

        @Test
        void update_nullComment_throwsIllegalArgumentException() {

            assertThatThrownBy(() -> commentService.update(null))
                    .isInstanceOf(IllegalArgumentException.class);

            verifyNoInteractions(commentRepository);
        }
    }

    // -------------------------------------------------------------------------
    // deleteByPostId()
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("deleteByPostId()")
    class DeleteByPostId {

        @Test
        void deleteByPostId_valid_callsRepository() {

            commentService.deleteByPostId(1);

            verify(commentRepository).deleteByPostId(1);
        }

        @Test
        void deleteByPostId_zero_throwsValidationException() {

            assertThatThrownBy(() -> commentService.deleteByPostId(0))
                    .isInstanceOf(ValidationException.class);

            verifyNoInteractions(commentRepository);
        }
    }
}