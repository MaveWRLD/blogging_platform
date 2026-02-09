package org.amalitech.service;

import org.amalitech.interfaces.CommentRepository;
import org.amalitech.models.Comment;
import org.amalitech.util.exception.ResourceNotFoundException;
import org.amalitech.util.exception.ValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
public class CommentServiceTest {
    @Mock
    private CommentRepository commentRepository;
    @InjectMocks
    private CommentService commentService;
    @Test
    void getCommentsByPostId_returnsComments() {
        when(commentRepository.findByPostId(1)).thenReturn(List.of(new Comment()));
        List<Comment> list = commentService.getCommentsByPostId(1);
        assertThat(list).isNotNull().hasSize(1);
    }
    @Test
    void getCommentsByPostId_throwsValidationExceptionForInvalidId() {
        assertThatThrownBy(() -> commentService.getCommentsByPostId(0))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Invalid post ID");
    }
    @Test
    void save_savesValidComment() {
        Comment comment = new Comment();
        comment.setPostId(1);
        comment.setUsername("user");
        comment.setBody("body");
        doNothing().when(commentRepository).save(comment);
        Comment saved = commentService.save(comment);
        assertThat(saved).isEqualTo(comment);
        verify(commentRepository).save(comment);
    }
    @Test
    void save_throwsValidationExceptionForInvalidComment() {
        Comment comment = new Comment();
        comment.setPostId(1);
        comment.setUsername("");
        comment.setBody("body");
        assertThatThrownBy(() -> commentService.save(comment))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Username cannot be empty");
    }
    @Test
    void findById_returnsComment() {
        Comment comment = new Comment();
        when(commentRepository.findByObjectId("id")).thenReturn(comment);
        Comment found = commentService.findById("id");
        assertThat(found).isEqualTo(comment);
    }
    @Test
    void findById_throwsNotFoundWhenMissing() {
        when(commentRepository.findByObjectId("id")).thenReturn(null);
        assertThatThrownBy(() -> commentService.findById("id"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void update_updatesComment() {
        Comment comment = new Comment();
        comment.setId("some-object-id");
        comment.setPostId(42);
        comment.setUsername("john_doe");
        comment.setBody("Updated great comment!");

        doNothing().when(commentRepository).update(any(Comment.class));

        commentService.update(comment);

        verify(commentRepository).update(comment);
    }

    @Test
    void deleteById_deletesComment() {
        doNothing().when(commentRepository).deleteByObjectId("id");
        commentService.deleteById("id");
        verify(commentRepository).deleteByObjectId("id");
    }
}