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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    // ─────────────────────────────────────────────────────────────────────────
    // createPost
    // ─────────────────────────────────────────────────────────────────────────

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

    // ─────────────────────────────────────────────────────────────────────────
    // updatePost
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("updatePost")
    class UpdatePost {

        private Post existingPost;

        @BeforeEach
        void setUp() {
            existingPost = new Post();
            existingPost.setTitle("Old Title");
            existingPost.setBody("Old Body");
            existingPost.setStatus(PostStatus.draft);
        }

        private void stubFindPost(int id) {
            Map<Post, List<Comment>> map = new HashMap<>();
            map.put(existingPost, Collections.emptyList());
            when(postService.findPostById(id)).thenReturn(map);
        }

        @Test
        @DisplayName("updates title only when body and status are null")
        void updatePost_titleOnly_updatesTitle() {
            stubFindPost(1);
            UpdatePostRequest input = mock(UpdatePostRequest.class);
            when(input.getTitle()).thenReturn("New Title");
            when(input.getBody()).thenReturn(null);
            when(input.getStatus()).thenReturn(null);

            Post result = resolver.updatePost(1, input);

            assertThat(result.getTitle()).isEqualTo("New Title");
            assertThat(result.getBody()).isEqualTo("Old Body");
            assertThat(result.getStatus()).isEqualTo(PostStatus.draft);
            verify(postService).updatePost(result, null);
        }

        @Test
        @DisplayName("updates body only when title and status are null")
        void updatePost_bodyOnly_updatesBody() {
            stubFindPost(2);
            UpdatePostRequest input = mock(UpdatePostRequest.class);
            when(input.getTitle()).thenReturn(null);
            when(input.getBody()).thenReturn("New Body");
            when(input.getStatus()).thenReturn(null);

            Post result = resolver.updatePost(2, input);

            assertThat(result.getBody()).isEqualTo("New Body");
            assertThat(result.getTitle()).isEqualTo("Old Title");
        }

        @Test
        @DisplayName("does not overwrite publishedAt when already set and status is PUBLISHED")
        void updatePost_statusToPublished_doesNotOverwriteExistingPublishedAt() {
            Instant alreadySet = Instant.parse("2024-01-01T00:00:00Z");
            existingPost.setPublishedAt(alreadySet);
            existingPost.setStatus(PostStatus.published);
            stubFindPost(4);
            UpdatePostRequest input = mock(UpdatePostRequest.class);
            when(input.getTitle()).thenReturn(null);
            when(input.getBody()).thenReturn(null);
            when(input.getStatus()).thenReturn("published");

            Post result = resolver.updatePost(4, input);

            // publishedAt must remain unchanged because the condition checks for null
            assertThat(result.getPublishedAt()).isEqualTo(alreadySet);
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when post does not exist")
        void updatePost_postNotFound_throwsResourceNotFoundException() {
            when(postService.findPostById(99)).thenReturn(Collections.emptyMap());
            UpdatePostRequest input = mock(UpdatePostRequest.class);

            assertThatThrownBy(() -> resolver.updatePost(99, input))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Post not found");

            verify(postService, never()).updatePost(any(), any());
        }

        @Test
        @DisplayName("does not set publishedAt when status changes to DRAFT")
        void updatePost_statusToDraft_doesNotSetPublishedAt() {
            existingPost.setPublishedAt(null);
            stubFindPost(6);
            UpdatePostRequest input = mock(UpdatePostRequest.class);
            when(input.getTitle()).thenReturn(null);
            when(input.getBody()).thenReturn(null);
            when(input.getStatus()).thenReturn("draft");

            Post result = resolver.updatePost(6, input);

            assertThat(result.getPublishedAt()).isNull();
            assertThat(result.getStatus()).isEqualTo(PostStatus.draft);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // deletePost
    // ─────────────────────────────────────────────────────────────────────────

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

    // ─────────────────────────────────────────────────────────────────────────
    // publishPost
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("publishPost")
    class PublishPost {

        @Test
        @DisplayName("publishes a DRAFT post, sets status and publishedAt")
        void publishPost_draftPost_publishesIt() {
            Post post = new Post();
            post.setStatus(PostStatus.draft);
            post.setPublishedAt(null);
            Map<Post, List<Comment>> map = new HashMap<>();
            map.put(post, Collections.emptyList());
            when(postService.findPostById(1)).thenReturn(map);

            Instant before = Instant.now();
            Post result = resolver.publishPost("1");
            Instant after = Instant.now();

            assertThat(result.getStatus()).isEqualTo(PostStatus.published);
            assertThat(result.getPublishedAt())
                    .isNotNull()
                    .isAfterOrEqualTo(before)
                    .isBeforeOrEqualTo(after);
            verify(postService).updatePost(result, null);
        }

        @Test
        @DisplayName("does not re-publish an already PUBLISHED post")
        void publishPost_alreadyPublished_doesNotUpdate() {
            Post post = new Post();
            post.setStatus(PostStatus.published);  // matches the enum constant used in the guard
            Instant originalTime = Instant.parse("2023-06-01T10:00:00Z");
            post.setPublishedAt(originalTime);
            Map<Post, List<Comment>> map = new HashMap<>();
            map.put(post, Collections.emptyList());
            when(postService.findPostById(2)).thenReturn(map);

            Post result = resolver.publishPost("2");

            assertThat(result.getStatus()).isEqualTo(PostStatus.published);
            assertThat(result.getPublishedAt()).isEqualTo(originalTime);
            verify(postService, never()).updatePost(any(), any());
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when post is missing")
        void publishPost_postNotFound_throwsException() {
            when(postService.findPostById(999)).thenReturn(Collections.emptyMap());

            assertThatThrownBy(() -> resolver.publishPost("999"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Post not found");

            verify(postService, never()).updatePost(any(), any());
        }

        @Test
        @DisplayName("parses string id before querying the service")
        void publishPost_parsesStringId() {
            Post post = new Post();
            post.setStatus(PostStatus.draft);
            Map<Post, List<Comment>> map = new HashMap<>();
            map.put(post, Collections.emptyList());
            when(postService.findPostById(7)).thenReturn(map);

            resolver.publishPost("7");

            verify(postService).findPostById(7);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // likePost
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("likePost")
    class LikePost {

        @Test
        @DisplayName("increments likeCount by 1")
        void likePost_incrementsLikeCount() {
            Post post = new Post();
            post.setLikeCount(5);
            Map<Post, List<Comment>> map = new HashMap<>();
            map.put(post, Collections.emptyList());
            when(postService.findPostById(3)).thenReturn(map);

            Post result = resolver.likePost("3");

            assertThat(result.getLikeCount()).isEqualTo(6);
            verify(postService).updatePost(result, null);
        }

        @Test
        @DisplayName("increments likeCount from zero")
        void likePost_fromZero_incrementsToOne() {
            Post post = new Post();
            post.setLikeCount(0);
            Map<Post, List<Comment>> map = new HashMap<>();
            map.put(post, Collections.emptyList());
            when(postService.findPostById(4)).thenReturn(map);

            Post result = resolver.likePost("4");

            assertThat(result.getLikeCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when post is missing")
        void likePost_postNotFound_throwsException() {
            when(postService.findPostById(404)).thenReturn(Collections.emptyMap());

            assertThatThrownBy(() -> resolver.likePost("404"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Post not found");

            verify(postService, never()).updatePost(any(), any());
        }

        @Test
        @DisplayName("throws NumberFormatException for non-numeric id")
        void likePost_nonNumericId_throwsNumberFormatException() {
            assertThatThrownBy(() -> resolver.likePost("not-a-number"))
                    .isInstanceOf(NumberFormatException.class);

            verify(postService, never()).findPostById(anyInt());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // createComment
    // ─────────────────────────────────────────────────────────────────────────

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