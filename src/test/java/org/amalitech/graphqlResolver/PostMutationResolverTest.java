package org.amalitech.graphqlResolver;

import org.amalitech.dtos.CreateCommentRequest;
import org.amalitech.dtos.postDtos.CreatePostRequest;
import org.amalitech.dtos.postDtos.UpdatePostRequest;
import org.amalitech.entities.Comment;
import org.amalitech.entities.Post;
import org.amalitech.enums.PostStatus;
import org.amalitech.exception.ResourceNotFoundException;
import org.amalitech.service.CommentService;
import org.amalitech.service.PostService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

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
       @DisplayName("delegates to postService.incrementLikeCount and returns result")
       void likePost_delegatesAndReturnsResult() {
           Post post = new Post();
           post.setLikeCount(6);

           when(postService.incrementLikeCount(3)).thenReturn(post);

           Post result = resolver.likePost("3");

           assertThat(result).isSameAs(post);
           verify(postService).incrementLikeCount(3);
       }

       @Test
       @DisplayName("throws ResourceNotFoundException when post is missing")
       void likePost_postNotFound_throwsException() {
           when(postService.incrementLikeCount(404))
                   .thenThrow(new ResourceNotFoundException("Post not found"));

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
           when(input.getPostId()).thenReturn(10);
           when(input.getBody()).thenReturn("Nice post!");

           Comment result = resolver.createComment(input);

           assertThat(result).isNotNull();
           assertThat(result.getPostId()).isEqualTo(10);
           assertThat(result.getBody()).isEqualTo("Nice post!");
           assertThat(result.getUsername()).isEqualTo("current_user");
           verify(commentService).save(result);
       }

       @Test
       @DisplayName("hardcodes username as 'current_user'")
       void createComment_usernameIsAlwaysCurrentUser() {
           CreateCommentRequest input = mock(CreateCommentRequest.class);
           when(input.getPostId()).thenReturn(1);
           when(input.getBody()).thenReturn("Test");

           Comment result = resolver.createComment(input);

           assertThat(result.getUsername()).isEqualTo("current_user");
       }

       @Test
       @DisplayName("returns the comment that was passed to commentService.save")
       void createComment_returnsSavedComment() {
           CreateCommentRequest input = mock(CreateCommentRequest.class);
           when(input.getPostId()).thenReturn(5);
           when(input.getBody()).thenReturn("A comment");

           ArgumentCaptor<Comment> captor = ArgumentCaptor.forClass(Comment.class);
           Comment result = resolver.createComment(input);

           verify(commentService).save(captor.capture());
           assertThat(result).isSameAs(captor.getValue());
       }
   }
}