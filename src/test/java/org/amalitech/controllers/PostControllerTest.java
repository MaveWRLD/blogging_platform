package org.amalitech.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.amalitech.dtos.CommentDto;
import org.amalitech.dtos.postDtos.*;
import org.amalitech.entities.Comment;
import org.amalitech.entities.Post;
import org.amalitech.enums.PostStatus;
import org.amalitech.exception.ResourceNotFoundException;
import org.amalitech.exception.ValidationException;
import org.amalitech.mappers.CommentMapper;
import org.amalitech.mappers.PostMapper;
import org.amalitech.service.PostService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PostController.class)
@DisplayName("PostController Tests")
class PostControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PostService postService;

    @MockitoBean
    private PostMapper postMapper;

    @MockitoBean
    private CommentMapper commentMapper;

    private Post samplePost;
    private PostDto samplePostDto;
    private Comment sampleComment;
    private CommentDto sampleCommentDto;

    @BeforeEach
    void setUp() {
        samplePost = new Post();
        samplePost.setId(1);
        samplePost.setTitle("Sample Post");
        samplePost.setBody("Post body content.");
        samplePost.setStatus(PostStatus.published);

        samplePostDto = new PostDto(
                1, "Sample Post", null, null, null,
                null, null, null, null, null,
                null, null, null
        );

        sampleComment = new Comment();
        sampleComment.setId("comment-1");
        sampleComment.setBody("Nice post");

        sampleCommentDto = new CommentDto();
        sampleCommentDto.setId("comment-1");
        sampleCommentDto.setBody("Nice post");
    }

    @Nested
    @DisplayName("POST /api/posts")
    class CreatePost {

        @Test
        @DisplayName("returns 200 with created post DTO on success")
        void validRequest_returns200WithDto() throws Exception {
            CreatePostRequest request = new CreatePostRequest();
            request.setTitle("Sample Post");
            request.setBody("Post body content.");
            request.setTagNames(Set.of("java"));

            when(postMapper.createPost(any(CreatePostRequest.class))).thenReturn(samplePost);
            when(postService.createPost(eq(samplePost), any(Set.class))).thenReturn(samplePost);
            when(postMapper.toDto(samplePost)).thenReturn(samplePostDto);

            mockMvc.perform(post("/api/posts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Post created successfully"))
                    .andExpect(jsonPath("$.data.id").value(1))
                    .andExpect(jsonPath("$.data.title").value("Sample Post"));

            verify(postService).createPost(eq(samplePost), any(Set.class));
        }

        @Test
        @DisplayName("returns 400 when request body is missing")
        void missingBody_returns400() throws Exception {
            mockMvc.perform(post("/api/posts")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("passes tag names from request through to service")
        void passesTagNamesToService() throws Exception {
            CreatePostRequest request = new CreatePostRequest();
            request.setTitle("Post");
            request.setBody("Body");
            request.setTagNames(Set.of("spring", "java"));

            when(postMapper.createPost(any(CreatePostRequest.class))).thenReturn(samplePost);
            when(postService.createPost(any(Post.class), eq(Set.of("spring", "java"))))
                    .thenReturn(samplePost);
            when(postMapper.toDto(samplePost)).thenReturn(samplePostDto);

            mockMvc.perform(post("/api/posts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());

            verify(postService).createPost(any(Post.class), eq(Set.of("spring", "java")));
        }
    }

    @Nested
    @DisplayName("PUT /api/posts/{id}")
    class UpdatePost {

        @Test
        @DisplayName("returns 200 with updated post DTO")
        void validUpdate_returns200WithDto() throws Exception {
            UpdatePostRequest request = new UpdatePostRequest();
            request.setTitle("Updated Title");
            request.setTagIds(List.of(1, 2));

            when(postService.findPostById(1)).thenReturn(Map.of(samplePost, List.of()));
            doNothing().when(postMapper).updateEntity(any(UpdatePostRequest.class), any(Post.class));
            doNothing().when(postService).updatePost(any(Post.class), any(List.class));
            when(postMapper.toDto(any(Post.class))).thenReturn(samplePostDto);

            mockMvc.perform(put("/api/posts/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Post updated successfully"))
                    .andExpect(jsonPath("$.data.id").value(1));

            verify(postMapper).updateEntity(any(UpdatePostRequest.class), eq(samplePost));
            verify(postService).updatePost(eq(samplePost), eq(List.of(1, 2)));
        }

        @Test
        @DisplayName("returns 404 when post to update is not found")
        void notFound_returns404() throws Exception {
            UpdatePostRequest request = new UpdatePostRequest();
            request.setTitle("Title");

            when(postService.findPostById(99))
                    .thenThrow(new ResourceNotFoundException("Post not found with ID: 99"));

            mockMvc.perform(put("/api/posts/99")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/posts")
    class GetAllPosts {

        @Test
        @DisplayName("returns 200 with paged posts using default params")
        void defaultParams_returns200WithPage() throws Exception {
            Page<PostDto> page = new PageImpl<>(List.of(samplePostDto));
            when(postService.getPosts(any(Pageable.class))).thenReturn(page);

            mockMvc.perform(get("/api/posts"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Posts retrieved successfully"))
                    .andExpect(jsonPath("$.data.posts", hasSize(1)))
                    .andExpect(jsonPath("$.data.posts[0].title").value("Sample Post"));
        }

        @Test
        @DisplayName("returns 200 with empty data when no posts exist")
        void noPosts_returns200WithEmpty() throws Exception {
            when(postService.getPosts(any(Pageable.class))).thenReturn(Page.empty());

            mockMvc.perform(get("/api/posts"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.posts", hasSize(0)));
        }

        @Test
        @DisplayName("respects custom page and size params")
        void customPageAndSize_passedToService() throws Exception {
            when(postService.getPosts(any(Pageable.class))).thenReturn(Page.empty());

            mockMvc.perform(get("/api/posts")
                            .param("page", "2")
                            .param("size", "5"))
                    .andExpect(status().isOk());

            verify(postService).getPosts(argThat(p ->
                    p.getPageNumber() == 2 && p.getPageSize() == 5
            ));
        }

        @Test
        @DisplayName("uses descending sort by default")
        void defaultSortDir_descendingSort() throws Exception {
            when(postService.getPosts(any(Pageable.class))).thenReturn(Page.empty());

            mockMvc.perform(get("/api/posts"))
                    .andExpect(status().isOk());

            verify(postService).getPosts(argThat(p ->
                    p.getSort().getOrderFor("createdAt") != null
                            && p.getSort().getOrderFor("createdAt").isDescending()
            ));
        }

        @Test
        @DisplayName("uses ascending sort when sortDir=ASC")
        void ascSortDir_ascendingSort() throws Exception {
            when(postService.getPosts(any(Pageable.class))).thenReturn(Page.empty());

            mockMvc.perform(get("/api/posts")
                            .param("sortDir", "ASC")
                            .param("sortBy", "createdAt"))
                    .andExpect(status().isOk());

            verify(postService).getPosts(argThat(p ->
                    p.getSort().getOrderFor("createdAt") != null
                            && p.getSort().getOrderFor("createdAt").isAscending()
            ));
        }

        @Test
        @DisplayName("clamps negative page to 0")
        void negativePage_clampsToZero() throws Exception {
            when(postService.getPosts(any(Pageable.class))).thenReturn(Page.empty());

            mockMvc.perform(get("/api/posts").param("page", "-3"))
                    .andExpect(status().isOk());

            verify(postService).getPosts(argThat(p -> p.getPageNumber() == 0));
        }

        @Test
        @DisplayName("clamps size of 0 to 1")
        void zeroSize_clampsToOne() throws Exception {
            when(postService.getPosts(any(Pageable.class))).thenReturn(Page.empty());

            mockMvc.perform(get("/api/posts").param("size", "0"))
                    .andExpect(status().isOk());

            verify(postService).getPosts(argThat(p -> p.getPageSize() == 1));
        }
    }

    // -------------------------------------------------------------------------
    // GET /api/posts/user/{userId}
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("GET /api/posts/user/{userId}")
    class GetPostsByUserId {

        @Test
        @DisplayName("returns 200 with user's posts when found")
        void postsExist_returns200() throws Exception {
            Page<PostDto> page = new PageImpl<>(List.of(samplePostDto));
            when(postService.findPostsByUserId(eq(1L), anyInt(), anyInt())).thenReturn(page);

            mockMvc.perform(get("/api/posts/user/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.posts", hasSize(1)))
                    .andExpect(jsonPath("$.posts[0].title").value("Sample Post"));
        }

        @Test
        @DisplayName("returns 404 when user has no posts")
        void noPosts_returns404() throws Exception {
            when(postService.findPostsByUserId(eq(1L), anyInt(), anyInt()))
                    .thenReturn(Page.empty());

            mockMvc.perform(get("/api/posts/user/1"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("passes custom page and size params to service")
        void customPageSize_passedToService() throws Exception {
            Page<PostDto> page = new PageImpl<>(List.of(samplePostDto));
            when(postService.findPostsByUserId(eq(2L), eq(1), eq(5))).thenReturn(page);

            mockMvc.perform(get("/api/posts/user/2")
                            .param("page", "1")
                            .param("size", "5"))
                    .andExpect(status().isOk());

            verify(postService).findPostsByUserId(2L, 1, 5);
        }
    }

    // -------------------------------------------------------------------------
    // GET /api/posts/trending
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("GET /api/posts/trending")
    class GetTrendingPosts {

        @Test
        @DisplayName("returns 200 with trending posts")
        void trendingPostsExist_returns200() throws Exception {
            Page<Post> page = new PageImpl<>(List.of(samplePost));
            when(postService.getTopTrendingPosts(anyInt(), any(Pageable.class))).thenReturn(page);
            when(postMapper.toDto(samplePost)).thenReturn(samplePostDto);

            mockMvc.perform(get("/api/posts/trending"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.posts", hasSize(1)))
                    .andExpect(jsonPath("$.posts[0].title").value("Sample Post"));
        }

        @Test
        @DisplayName("returns 200 with empty list when no trending posts")
        void noTrending_returns200WithEmpty() throws Exception {
            when(postService.getTopTrendingPosts(anyInt(), any(Pageable.class)))
                    .thenReturn(Page.empty());

            mockMvc.perform(get("/api/posts/trending"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.posts", hasSize(0)));
        }

        @Test
        @DisplayName("passes custom limit to service")
        void customLimit_passedToService() throws Exception {
            when(postService.getTopTrendingPosts(eq(5), any(Pageable.class)))
                    .thenReturn(Page.empty());

            mockMvc.perform(get("/api/posts/trending").param("limit", "5"))
                    .andExpect(status().isOk());

            verify(postService).getTopTrendingPosts(eq(5), any(Pageable.class));
        }

        @Test
        @DisplayName("maps each trending post through the mapper")
        void multipleTrending_mapsAll() throws Exception {
            Post second = new Post();
            second.setId(2);
            PostDto secondDto = new PostDto(
                    2, null, null, null, null,
                    null, null, null, null, null,
                    null, null, null
            );

            Page<Post> page = new PageImpl<>(List.of(samplePost, second));
            when(postService.getTopTrendingPosts(anyInt(), any(Pageable.class))).thenReturn(page);
            when(postMapper.toDto(samplePost)).thenReturn(samplePostDto);
            when(postMapper.toDto(second)).thenReturn(secondDto);

            mockMvc.perform(get("/api/posts/trending"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.posts", hasSize(2)));

            verify(postMapper, times(2)).toDto(any(Post.class));
        }
    }

    // -------------------------------------------------------------------------
    // GET /api/posts/{id}
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("GET /api/posts/{id}")
    class GetPost {

        @Test
        @DisplayName("returns 404 when post does not exist")
        void notFound_returns404() throws Exception {
            when(postService.findPostById(99))
                    .thenThrow(new ResourceNotFoundException("Post not found with ID: 99"));

            mockMvc.perform(get("/api/posts/99"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("returns 400 when id is zero or negative")
        void invalidId_returns400() throws Exception {
            mockMvc.perform(get("/api/posts/0"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("sets totalComments from comment list size")
        void setsTotalCommentsCorrectly() throws Exception {
            Comment second = new Comment();
            second.setId("comment-2");
            CommentDto secondDto = new CommentDto();
            secondDto.setId("comment-2");

            PostWithCommentsDto withComments = new PostWithCommentsDto();

            when(postService.findPostById(1))
                    .thenReturn(Map.of(samplePost, List.of(sampleComment, second)));
            when(postMapper.toDto(samplePost)).thenReturn(samplePostDto);
            when(commentMapper.toDto(sampleComment)).thenReturn(sampleCommentDto);
            when(commentMapper.toDto(second)).thenReturn(secondDto);
            when(postMapper.toDtoWithComments(any(PostDto.class), any(List.class))).thenReturn(withComments);

            mockMvc.perform(get("/api/posts/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.totalComments").value(2));
        }

        @Test
        @DisplayName("returns 404 when service returns empty map")
        void emptyMap_returns404() throws Exception {
            when(postService.findPostById(1)).thenReturn(Map.of());

            mockMvc.perform(get("/api/posts/1"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("includes correct message with post id")
        void responseMessageContainsId() throws Exception {
            PostWithCommentsDto withComments = new PostWithCommentsDto();

            when(postService.findPostById(1)).thenReturn(Map.of(samplePost, List.of()));
            when(postMapper.toDto(samplePost)).thenReturn(samplePostDto);
            when(postMapper.toDtoWithComments(any(), any())).thenReturn(withComments);

            mockMvc.perform(get("/api/posts/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Post with 1 found"));
        }
    }

    @Nested
    @DisplayName("DELETE /api/posts/{id}")
    class DeletePost {

        @Test
        @DisplayName("returns 204 on successful deletion")
        void existingPost_returns204() throws Exception {
            doNothing().when(postService).deletePost(1);

            mockMvc.perform(delete("/api/posts/1"))
                    .andExpect(status().isNoContent());

            verify(postService).deletePost(1);
        }

        @Test
        @DisplayName("returns 404 when post does not exist")
        void notFound_returns404() throws Exception {
            doThrow(new ResourceNotFoundException("Post not found with id: 99"))
                    .when(postService).deletePost(99);

            mockMvc.perform(delete("/api/posts/99"))
                    .andExpect(status().isNotFound());
        }
    }
}