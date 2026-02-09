package org.amalitech.graphqlResolver;

import org.amalitech.dtos.CreateCommentRequest;
import org.amalitech.dtos.postDtos.CreatePostRequest;
import org.amalitech.dtos.postDtos.UpdatePostRequest;
import org.amalitech.models.Comment;
import org.amalitech.models.Post;
import org.amalitech.service.CommentService;
import org.amalitech.service.PostService;
import org.amalitech.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class PostMutationResolverTest {
    @Mock
    private PostService postService;
    @Mock
    private CommentService commentService;
    @Mock
    private UserService userService;
    private PostMutationResolver resolver;
    @BeforeEach
    void setUp() {
        resolver = new PostMutationResolver(postService, commentService, userService);
    }
    @Test
    void createPost_createsPost() {
        CreatePostRequest input = new CreatePostRequest();
        input.setTitle("title");
        input.setBody("body");
        doNothing().when(postService).createPost(any(Post.class), isNull());
        Post post = resolver.createPost(input);
        assertThat(post.getTitle()).isEqualTo("title");
        assertThat(post.getUserId()).isEqualTo(1);
        assertThat(post.getStatus()).isEqualTo("DRAFT");
    }
    @Test
    void updatePost_updatesPost() {
        int id = 1;
        UpdatePostRequest input = new UpdatePostRequest();
        input.setTitle("new title");
        Post existing = new Post();
        when(postService.findPostById(id)).thenReturn(Map.of(existing, List.of()));
        doNothing().when(postService).updatePost(any(Post.class), isNull());
        Post updated = resolver.updatePost(id, input);
        assertThat(updated.getTitle()).isEqualTo("new title");
    }
    @Test
    void deletePost_deletesPost() {
        doNothing().when(postService).deletePost(1);
        Boolean deleted = resolver.deletePost("1");
        assertThat(deleted).isTrue();
    }
    @Test
    void publishPost_publishesPost() {
        int id = 1;
        Post post = new Post();
        post.setStatus("DRAFT");
        when(postService.findPostById(id)).thenReturn(Map.of(post, List.of()));
        doNothing().when(postService).updatePost(any(Post.class), isNull());
        Post published = resolver.publishPost("1");
        assertThat(published.getStatus()).isEqualTo("PUBLISHED");
        assertThat(published.getPublishedAt()).isNotNull();
    }
    @Test
    void likePost_likesPost() {
        int id = 1;
        Post post = new Post();
        post.setLikeCount(0);
        when(postService.findPostById(id)).thenReturn(Map.of(post, List.of()));
        doNothing().when(postService).updatePost(any(Post.class), isNull());
        Post liked = resolver.likePost("1");
        assertThat(liked.getLikeCount()).isEqualTo(1);
    }

    @Test
    void createComment_createsComment() {
        CreateCommentRequest input = new CreateCommentRequest();
        input.setPostId(1);
        input.setBody("Great post!");

        Comment savedComment = new Comment();
        savedComment.setPostId(1);
        savedComment.setUsername("current_user");
        savedComment.setBody("Great post!");

        when(commentService.save(any(Comment.class))).thenReturn(savedComment);

        Comment result = resolver.createComment(input);

        assertThat(result).isNotNull();
        assertThat(result.getPostId()).isEqualTo(1);
        assertThat(result.getUsername()).isEqualTo("current_user");
        assertThat(result.getBody()).isEqualTo("Great post!");
    }
}
