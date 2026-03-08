package org.amalitech.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.amalitech.dtos.postDtos.*;
import org.amalitech.entities.Post;
import org.amalitech.entities.Role;
import org.amalitech.entities.User;
import org.amalitech.exception.CustomExceptionHandler;
import org.amalitech.exception.ResourceNotFoundException;
import org.amalitech.mappers.CommentMapper;
import org.amalitech.mappers.PostMapper;
import org.amalitech.service.PostService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.*;

import java.util.Objects;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PostControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private PostService postService;
    
    @Mock
    private PostMapper postMapper;
    
    @Mock
    private CommentMapper commentMapper;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private PostController postController;

    private Post testPost;
    private PostDto testPostDto;
    private PostWithCommentsDto testPostWithCommentsDto;
    private CreatePostRequest createPostRequest;
    private UpdatePostRequest updatePostRequest;
    private User testUser;

    @BeforeEach
    void setUp() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn("testuser");
        SecurityContextHolder.setContext(securityContext);

        mockMvc = MockMvcBuilders.standaloneSetup(postController)
                .setControllerAdvice(new CustomExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();

        Role readerRole = new Role();
        readerRole.setName("READER");
        Set<Role> readerRoles = new HashSet<>(Arrays.asList(readerRole));
        testUser = User.registerReader("testuser", "test@example.com", "password", "Test", "User", readerRoles);
        testUser.setId(1L);

        testPost = new Post("Test Title", "Test Body", org.amalitech.enums.PostStatus.draft, Instant.now(), null);
        testPost.setId(1);
        testPost.setUser(testUser);

        testPostDto = new PostDto(
                1,
                "Test Title",
                "Test Body",
                Instant.now(),
                Instant.now(),
                "Test excerpt",
                null,
                5,
                10,
                3,
                org.amalitech.enums.PostStatus.draft,
                1L,
                "testuser"
        );

        testPostWithCommentsDto = PostWithCommentsDto.builder()
                .postDto(testPostDto)
                .comments(new ArrayList<>())
                .totalComments(0)
                .build();

        createPostRequest = new CreatePostRequest();
        createPostRequest.setTitle("Test Title");
        createPostRequest.setBody("Test Body");
        createPostRequest.setTagNames(new HashSet<>(Arrays.asList("java", "spring")));
        createPostRequest.setStatus("draft");

        updatePostRequest = new UpdatePostRequest();
        updatePostRequest.setTitle("Updated Title");
        updatePostRequest.setBody("Updated Body");
        updatePostRequest.setStatus("published");
        updatePostRequest.setTagIds(Set.of(1, 2));
    }

    @Test
    @SuppressWarnings("unchecked")
    void createPost_WithValidRequest_ShouldReturnCreatedPost() throws Exception {
        when(postMapper.createPost(any(CreatePostRequest.class))).thenReturn(testPost);
        when(postService.createPost(any(Post.class), any(Set.class))).thenReturn(testPost);
        when(postMapper.toDto(any(Post.class))).thenReturn(testPostDto);

        mockMvc.perform(post("/api/posts")
                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(createPostRequest))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.message").value("Post created successfully"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.title").value("Test Title"))
                .andExpect(jsonPath("$.data.body").value("Test Body"))
                .andExpect(jsonPath("$.data.author").value("testuser"));

        verify(postMapper).createPost(any(CreatePostRequest.class));
        verify(postService).createPost(any(Post.class), any(Set.class));
        verify(postMapper).toDto(any(Post.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void createPost_WithInvalidRequest_ShouldReturnBadRequest() throws Exception {
        CreatePostRequest invalidRequest = new CreatePostRequest();

        mockMvc.perform(post("/api/posts")
                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(invalidRequest))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updatePost_WithValidRequest_ShouldReturnUpdatedPost() throws Exception {
        Post updatedPost = new Post("Updated Title", "Updated Body", org.amalitech.enums.PostStatus.published, Instant.now(), Instant.now());
        updatedPost.setId(1);
        updatedPost.setUser(testUser);

        PostDto updatedPostDto = new PostDto(
                1,
                "Updated Title",
                "Updated Body",
                Instant.now(),
                Instant.now(),
                "Updated excerpt",
                Instant.now(),
                8,
                15,
                5,
                org.amalitech.enums.PostStatus.published,
                1L,
                "testuser"
        );

        when(postService.updatePost(anyInt(), any(UpdatePostRequest.class))).thenReturn(updatedPost);
        when(postMapper.toDto(any(Post.class))).thenReturn(updatedPostDto);

        mockMvc.perform(put("/api/posts/1")
                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(updatePostRequest))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.message").value("Post updated successfully"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.title").value("Updated Title"))
                .andExpect(jsonPath("$.data.body").value("Updated Body"))
                .andExpect(jsonPath("$.data.status").value("published"));

        verify(postService).updatePost(eq(1), any(UpdatePostRequest.class));
        verify(postMapper).toDto(any(Post.class));
    }

    @Test
    void updatePost_WithNonExistentId_ShouldReturnNotFound() throws Exception {
        when(postService.updatePost(anyInt(), any(UpdatePostRequest.class)))
                .thenThrow(new ResourceNotFoundException("Post not found"));

        mockMvc.perform(put("/api/posts/999")
                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(updatePostRequest))))
                .andExpect(status().isNotFound());

        verify(postService).updatePost(eq(999), any(UpdatePostRequest.class));
    }

    @Test
    @WithMockUser
    void getAllPosts_WithDefaultParameters_ShouldReturnPagedPosts() throws Exception {
        List<PostDto> posts = Objects.requireNonNull(Arrays.asList(testPostDto));
        Page<PostDto> postPage = new PageImpl<>(posts, 
                org.springframework.data.domain.PageRequest.of(0, 12), 1);

        when(postService.getPosts(any(org.springframework.data.domain.Pageable.class))).thenReturn(postPage);

        mockMvc.perform(get("/api/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.message").value("Posts retrieved successfully"))
                .andExpect(jsonPath("$.data.posts").isArray())
                .andExpect(jsonPath("$.data.posts[0].id").value(1))
                .andExpect(jsonPath("$.data.posts[0].title").value("Test Title"))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(12))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.totalPages").value(1))
                .andExpect(jsonPath("$.data.hasPrevious").value(false))
                .andExpect(jsonPath("$.data.hasNext").value(false));

        verify(postService).getPosts(any(org.springframework.data.domain.Pageable.class));
    }

    @Test
    @WithMockUser
    void getAllPosts_WithCustomParameters_ShouldReturnPagedPosts() throws Exception {
        List<PostDto> posts = Objects.requireNonNull(Arrays.asList(testPostDto));
        Page<PostDto> postPage = new PageImpl<>(posts, 
                org.springframework.data.domain.PageRequest.of(1, 5), 15);

        when(postService.getPosts(any(org.springframework.data.domain.Pageable.class))).thenReturn(postPage);

        mockMvc.perform(get("/api/posts?page=1&size=5&sortBy=title&sortDir=asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(5))
                .andExpect(jsonPath("$.data.hasPrevious").value(true))
                .andExpect(jsonPath("$.data.hasNext").value(true));

        verify(postService).getPosts(any(org.springframework.data.domain.Pageable.class));
    }

    @Test
    void getPostsByUserId_WithValidUserId_ShouldReturnUserPosts() throws Exception {
        List<Post> posts = Objects.requireNonNull(Arrays.asList(testPost));
        Page<Post> postPage = new PageImpl<>(posts, 
                org.springframework.data.domain.PageRequest.of(0, 10), 1);

        when(postService.findPostsByUserId(anyLong(), anyInt(), anyInt())).thenReturn(postPage);

        mockMvc.perform(get("/api/posts/user/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.message").value("Posts retrieved successfully"))
                .andExpect(jsonPath("$.data.posts").isArray())
                .andExpect(jsonPath("$.data.posts[0].id").value(1))
                .andExpect(jsonPath("$.data.posts[0].userId").value(1L))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(10))
                .andExpect(jsonPath("$.data.total").value(1));

        verify(postService).findPostsByUserId(eq(1L), eq(0), eq(10));
    }

    @Test
    void getPostsByUserId_WithNonExistentUser_ShouldReturnNotFound() throws Exception {
        Page<Post> emptyPage = new PageImpl<>(Collections.emptyList(), 
                org.springframework.data.domain.PageRequest.of(0, 10), 0);

        when(postService.findPostsByUserId(anyLong(), anyInt(), anyInt())).thenReturn(emptyPage);

        mockMvc.perform(get("/api/posts/user/999"))
                .andExpect(status().isNotFound());

        verify(postService).findPostsByUserId(eq(999L), eq(0), eq(10));
    }


    @Test
    void getPost_WithValidId_ShouldReturnPostWithComments() throws Exception {
        Map<Post, List<org.amalitech.entities.Comment>> postWithComments = new HashMap<>();
        postWithComments.put(testPost, new ArrayList<>());

        when(postService.findPostById(anyInt())).thenReturn(postWithComments);
        when(postMapper.toDto(any(Post.class))).thenReturn(testPostDto);
        when(postMapper.toDtoWithComments(any(PostDto.class), anyList())).thenReturn(testPostWithCommentsDto);

        mockMvc.perform(get("/api/posts/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.message").value("Post with 1 found"))
                .andExpect(jsonPath("$.data.postDto.id").value(1))
                .andExpect(jsonPath("$.data.postDto.title").value("Test Title"))
                .andExpect(jsonPath("$.data.totalComments").value(0));

        verify(postService).findPostById(eq(1));
        verify(postMapper).toDto(any(Post.class));
        verify(postMapper).toDtoWithComments(any(PostDto.class), anyList());
    }

    @Test
    void getPost_WithInvalidId_ShouldReturnBadRequest() throws Exception {
        mockMvc.perform(get("/api/posts/0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getPost_WithNonExistentId_ShouldReturnNotFound() throws Exception {
        when(postService.findPostById(anyInt())).thenReturn(null);

        mockMvc.perform(get("/api/posts/999"))
                .andExpect(status().isNotFound());

        verify(postService).findPostById(eq(999));
    }

    @Test
    void deletePost_WithValidId_ShouldDeletePost() throws Exception {
        doNothing().when(postService).deletePost(anyInt());

        mockMvc.perform(delete("/api/posts/1"))
                .andExpect(status().isNoContent());

        verify(postService).deletePost(eq(1));
    }

    @Test
    void deletePost_WithNonExistentId_ShouldReturnNotFound() throws Exception {
        doThrow(new ResourceNotFoundException("Post not found"))
                .when(postService).deletePost(anyInt());

        mockMvc.perform(delete("/api/posts/999"))
                .andExpect(status().isNotFound());

        verify(postService).deletePost(eq(999));
    }
}
