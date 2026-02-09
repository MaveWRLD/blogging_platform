package org.amalitech.controllers;

import org.amalitech.dtos.ApiResponse;
import org.amalitech.dtos.CommentDto;
import org.amalitech.dtos.postDtos.PagedPostsResponse;
import org.amalitech.dtos.postDtos.PostDto;
import org.amalitech.dtos.postDtos.PostWithCommentsDto;
import org.amalitech.dtos.postDtos.CreatePostRequest;
import org.amalitech.mappers.CommentMapper;
import org.amalitech.mappers.PostMapper;
import org.amalitech.models.Comment;
import org.amalitech.models.Post;
import org.amalitech.service.PostService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PostControllerTest {
    @Mock
    private PostService postService;
    @Mock
    private PostMapper postMapper;
    @Mock
    private CommentMapper commentMapper;
    private PostController controller;
    @BeforeEach
    void setUp() {
        controller = new PostController(postService, postMapper, commentMapper);
    }
    @Test
    void getAllPosts_returnsPagedResponse() {
        List<Post> posts = Collections.emptyList();

        when(postService.getAllPosts(0, 12)).thenReturn(posts);
        when(postService.countPosts()).thenReturn(0L);
        ResponseEntity<ApiResponse<PagedPostsResponse>> response = controller.getAllPosts(0, 12);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        ApiResponse<PagedPostsResponse> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getData().getPosts()).isEmpty();
    }
    @Test
    void createPost_createsAndReturnsPost() {
        CreatePostRequest request = new CreatePostRequest();
        request.setTitle("Test Title");
        request.setBody("Test Body");
        Post post = new Post();
        post.setId(1);

        when(postMapper.toEntity(request)).thenReturn(post);
        doNothing().when(postService).createPost(any(Post.class), isNull());
        when(postMapper.toDto(post)).thenReturn(new PostDto());
        ResponseEntity<PostDto> response = controller.createPost(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void getPost_returnsPostWithComments() {
        int id = 1;
        Post post = new Post();
        post.setId(id);
        List<Comment> comments = List.of(new Comment());

        when(postService.findPostById(id)).thenReturn(Map.of(post, comments));
        PostDto postDto = new PostDto();
        when(postMapper.toDto(post)).thenReturn(postDto);
        when(commentMapper.toDto(any(Comment.class))).thenReturn(new CommentDto());
        when(postMapper.toDtoWithComments(postDto, anyList())).thenReturn(new PostWithCommentsDto());
        ResponseEntity<PostWithCommentsDto> response = controller.getPost(id);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void getPost_returnsNotFoundWhenPostNotExists() {
        int id = 1;
        when(postService.findPostById(id)).thenReturn(Collections.emptyMap());
        ResponseEntity<PostWithCommentsDto> response = controller.getPost(id);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getTrendingPosts_returnsTrendingPosts() {
        List<Post> trending = List.of(new Post());
        when(postService.getTrendingPosts(10)).thenReturn(trending);
        when(postMapper.toDto(any(Post.class))).thenReturn(new PostDto());

        ResponseEntity<ApiResponse<List<PostDto>>> response = controller.getTrendingPosts(10);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertNotNull(response.getBody());
        assertThat(response.getBody().getData()).hasSize(1);
    }

    @Test
    void deletePost_deletesPost() {
        int id = 1;
        doNothing().when(postService).deletePost(id);
        controller.deletePost(id);
        verify(postService).deletePost(id);
    }
}