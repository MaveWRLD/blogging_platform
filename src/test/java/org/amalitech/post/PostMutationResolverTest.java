package org.amalitech.post;

import org.amalitech.comment.dto.CreateCommentRequest;
import org.amalitech.post.dto.CreatePostRequest;
import org.amalitech.post.dto.UpdatePostRequest;
import org.amalitech.comment.Comment;
import org.amalitech.post.Post;
import org.amalitech.post.PostStatus;
import org.amalitech.common.exception.ResourceNotFoundException;
import org.amalitech.comment.CommentService;
import org.amalitech.post.PostService;
import org.amalitech.post.graphqlResolver.PostMutationResolver;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("PostMutationResolver")
class PostMutationResolverTest {

   @Mock
   private PostService postService;

   @Mock
   private CommentService commentService;

   @InjectMocks
   private PostMutationResolver resolver;

   @Nested
   @DisplayName("createPost")
   class CreatePost {

       @Test
       @DisplayName("creates a post with explicit PUBLISHED status")
       void createPost_withExplicitStatus_setsStatusCorrectly() {
           CreatePostRequest input = mock(CreatePostRequest.class);
           when(input.getTitle()).thenReturn("Hello World");
           when(input.getBody()).thenReturn("Some body text");
           when(input.getStatus()).thenReturn("published");

           Post result = resolver.createPost(input);

           assertThat(result.getTitle()).isEqualTo("Hello World");
           assertThat(result.getBody()).isEqualTo("Some body text");
           assertThat(result.getStatus()).isEqualTo(PostStatus.published);
           verify(postService).createPost(result, null);
       }

       @Test
       @DisplayName("defaults to DRAFT when status is null")
       void createPost_withNullStatus_defaultsToDraft() {
           CreatePostRequest input = mock(CreatePostRequest.class);
           when(input.getTitle()).thenReturn("Draft Post");
           when(input.getBody()).thenReturn("Draft body");
           when(input.getStatus()).thenReturn(null);

           Post result = resolver.createPost(input);

           assertThat(result.getStatus()).isEqualTo(PostStatus.draft);
           verify(postService).createPost(result, null);
       }

       @Test
       @DisplayName("returns the post object that was persisted")
       void createPost_returnsCreatedPost() {
           CreatePostRequest input = mock(CreatePostRequest.class);
           when(input.getTitle()).thenReturn("My Post");
           when(input.getBody()).thenReturn("Body");
           when(input.getStatus()).thenReturn("draft");

           Post result = resolver.createPost(input);

           assertThat(result).isNotNull();
           assertThat(result.getTitle()).isEqualTo("My Post");
       }

       @Test
       @DisplayName("passes null as second arg to postService.createPost")
       void createPost_passesNullSecondArg() {
           CreatePostRequest input = mock(CreatePostRequest.class);
           when(input.getTitle()).thenReturn("T");
           when(input.getBody()).thenReturn("B");
           when(input.getStatus()).thenReturn("draft");

           resolver.createPost(input);

           ArgumentCaptor<Post> postCaptor = ArgumentCaptor.forClass(Post.class);
           verify(postService).createPost(postCaptor.capture(), isNull());
           assertThat(postCaptor.getValue().getTitle()).isEqualTo("T");
       }
   }


   @Nested
   @DisplayName("updatePost")
   class UpdatePost {

       @Test
       @DisplayName("delegates to postService.updatePost and returns result")
       void updatePost_delegatesAndReturnsResult() {
           UpdatePostRequest input = mock(UpdatePostRequest.class);
           Post updated = new Post();
           updated.setTitle("New Title");
           updated.setBody("Body");
           updated.setStatus(PostStatus.draft);

           when(postService.updatePost(1, input)).thenReturn(updated);

           Post result = resolver.updatePost(1, input);

           assertThat(result).isSameAs(updated);
           verify(postService).updatePost(1, input);
       }

       @Test
       @DisplayName("propagates ResourceNotFoundException from service")
       void updatePost_postNotFound_propagatesException() {
           UpdatePostRequest input = mock(UpdatePostRequest.class);

           when(postService.updatePost(99, input))
                   .thenThrow(new ResourceNotFoundException("Post not found"));

           assertThatThrownBy(() -> resolver.updatePost(99, input))
                   .isInstanceOf(ResourceNotFoundException.class)
                   .hasMessageContaining("Post not found");
       }
   }


   @Nested
   @DisplayName("deletePost")
   class DeletePost {

       @Test
       @DisplayName("returns true on successful deletion")
       void deletePost_success_returnsTrue() {
           doNothing().when(postService).deletePost(10);

           Boolean result = resolver.deletePost("10");

           assertThat(result).isTrue();
           verify(postService).deletePost(10);
       }

       @Test
       @DisplayName("parses string id to int and delegates to service")
       void deletePost_parsesIdCorrectly() {
           doNothing().when(postService).deletePost(42);

           resolver.deletePost("42");

           verify(postService).deletePost(42);
       }

       @Test
       @DisplayName("throws NumberFormatException when id is not numeric")
       void deletePost_nonNumericId_throwsNumberFormatException() {
           assertThatThrownBy(() -> resolver.deletePost("abc"))
                   .isInstanceOf(NumberFormatException.class);

           verify(postService, never()).deletePost(anyInt());
       }
   }

   @Nested
   @DisplayName("likePost")
   class LikePost {

       @Test
       @DisplayName("delegates to postService.incrementLikeCount")
       void likePost_delegates() {
           Post post = new Post();
           post.setLikeCount(6);

           doNothing().when(postService).incrementLikeCount(3);

           resolver.likePost("3");

           verify(postService).incrementLikeCount(3);
       }

       @Test
       @DisplayName("throws ResourceNotFoundException when post is missing")
       void likePost_postNotFound_throwsException() {
           doThrow(new ResourceNotFoundException("Post not found"))
                   .when(postService).incrementLikeCount(404);

           assertThatThrownBy(() -> resolver.likePost("404"))
                   .isInstanceOf(ResourceNotFoundException.class)
                   .hasMessageContaining("Post not found");
       }

       @Test
       @DisplayName("throws NumberFormatException for non-numeric id")
       void likePost_nonNumericId_throwsNumberFormatException() {
           assertThatThrownBy(() -> resolver.likePost("not-a-number"))
                   .isInstanceOf(NumberFormatException.class);
       }
   }

   @Nested
   @DisplayName("createComment")
   class CreateComment {

       @Test
       @DisplayName("creates and saves a comment with correct fields")
       void createComment_setsFieldsAndSaves() {
           CreateCommentRequest input = mock(CreateCommentRequest.class);
           when(input.getBody()).thenReturn("Nice post!");

           Comment result = resolver.createComment(10L, input);

           assertThat(result).isNotNull();
           assertThat(result.getPostId()).isEqualTo(10L);
           assertThat(result.getBody()).isEqualTo("Nice post!");
           assertThat(result.getUsername()).isEqualTo("current_user");
           verify(commentService).save(10L, result);
       }

       @Test
       @DisplayName("hardcodes username as 'current_user'")
       void createComment_usernameIsAlwaysCurrentUser() {
           CreateCommentRequest input = mock(CreateCommentRequest.class);
           when(input.getBody()).thenReturn("Test");

           Comment result = resolver.createComment(1L, input);

           assertThat(result.getUsername()).isEqualTo("current_user");
       }

       @Test
       @DisplayName("returns the comment that was passed to commentService.save")
       void createComment_returnsSavedComment() {
           CreateCommentRequest input = mock(CreateCommentRequest.class);
           when(input.getBody()).thenReturn("A comment");

           ArgumentCaptor<Comment> captor = ArgumentCaptor.forClass(Comment.class);
           Comment result = resolver.createComment(5L, input);

           verify(commentService).save(eq(5L), captor.capture());
           assertThat(result).isSameAs(captor.getValue());
       }
   }
}