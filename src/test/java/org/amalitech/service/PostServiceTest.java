package org.amalitech.service;

import org.amalitech.algorithm.CacheManager;
import org.amalitech.algorithm.TrendingSortAlgorithm;
import org.amalitech.dtos.postDtos.PostFilter;
import org.amalitech.interfaces.PostRepository;
import org.amalitech.interfaces.PostTagRepository;
import org.amalitech.models.Comment;
import org.amalitech.models.Post;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PostServiceTest {
    @Mock
    private PostRepository postRepository;
    @Mock
    private PostTagRepository postTagRepository;
    @Mock
    private CommentService commentService;
    @Mock
    private TrendingSortAlgorithm trendingSortAlgorithm;
    @Mock
    private CacheManager cacheManager;
    private PostService postService;
    @BeforeEach
    void setUp() {
        postService = new PostService(postRepository, postTagRepository, commentService, trendingSortAlgorithm, cacheManager);
    }
    @Test
    void findPostById_returnsPostAndComments() {
        Post p = new Post();
        p.setId(1);
        when(postRepository.findById(1)).thenReturn(java.util.Optional.of(p));
        List<Comment> comments = List.of(new Comment());
        when(commentService.getCommentsByPostId(1)).thenReturn(comments);
        Map<Post, List<Comment>> result = postService.findPostById(1);
        assertThat(result).containsKey(p);
        assertThat(result.get(p)).isEqualTo(comments);
    }
    @Test
    void createPost_createsPost() {
        Post post = new Post();
        post.setTitle("title");
        post.setBody("body");
        post.setStatus("DRAFT");
        post.setUserId(1);
        when(postRepository.save(any(Post.class))).thenReturn(1);
        doNothing().when(postTagRepository).addTagsToPost(anyInt(), anyList());
        postService.createPost(post, List.of(1));
        verify(postRepository).save(post);
        verify(postTagRepository).addTagsToPost(1, List.of(1));
    }

    @Test
    void updatePost_updatesPost() {
        Post post = new Post();
        post.setId(1);
        post.setTitle("new title");
        post.setBody("updated body");
        doNothing().when(postRepository).update(post);
        doNothing().when(postTagRepository).deleteAllTagsForPost(1);
        postService.updatePost(post, List.of());
        verify(postRepository).update(post);
        verify(postTagRepository).deleteAllTagsForPost(1);
    }

    @Test
    void deletePost_deletesPost() {
        doNothing().when(postTagRepository).deleteAllTagsForPost(1);
        doNothing().when(postRepository).delete(1);
        postService.deletePost(1);
        verify(postRepository).delete(1);
    }
    @Test
    void findPosts_returnsPosts() {
        PostFilter filter = new PostFilter();
        List<Post> posts = List.of(new Post());
        when(postRepository.findPosts(anyInt(), anyInt(), isNull(), isNull(), isNull(), isNull(), isNull())).thenReturn(posts);
        List<Post> result = postService.findPosts(filter);
        assertThat(result).isEqualTo(posts);
    }
}