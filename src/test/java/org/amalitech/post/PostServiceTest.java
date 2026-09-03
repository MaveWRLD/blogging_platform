package org.amalitech.post;

import org.amalitech.post.algorithm.TrendingSortAlgorithm;
import org.amalitech.post.dto.PostDto;
import org.amalitech.post.dto.PostFilter;
import org.amalitech.post.dto.UpdatePostRequest;
import org.amalitech.post.Post;
import org.amalitech.user.Role;
import org.amalitech.post.tag.Tag;
import org.amalitech.user.User;
import org.amalitech.post.PostStatus;
import org.amalitech.common.exception.ResourceNotFoundException;
import org.amalitech.common.exception.ValidationException;
import org.amalitech.post.PostMapper;
import org.amalitech.post.PostRepository;
import org.amalitech.post.tag.TagRepository;
import org.amalitech.user.UserService;
import org.amalitech.post.tag.TagService;
import org.amalitech.post.PostValidator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PostService Tests")
class PostServiceTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        reset(postRepository, tagRepository);
    }

    @Mock
    private PostRepository postRepository;

    @Mock
    private TagRepository tagRepository;

    @Mock
    private PostMetricsService postMetricsService;

    @Mock
    private UserService userService;

    @Mock
    private TagService tagService;

    @Mock
    private PostMapper postMapper;

    @InjectMocks
    private PostService postService;

    private Post samplePost;
    private User sampleUser;
    private Tag sampleTag;

    @BeforeEach
    void setUp() {
        Set<Role> readerRoles = Set.of(new Role());
        sampleUser =  User.registerReader(
                "author",
                "john@example.com",
                "plainpassword",
                "John",
                "Doe",
                readerRoles
        );

        sampleTag = new Tag();
        sampleTag.setId(1);
        sampleTag.setName("java");

        samplePost = new Post();
        samplePost.setId(1);
        samplePost.setTitle("Test Post");
        samplePost.setBody("This is the post body.");
        samplePost.setStatus(PostStatus.draft);
        samplePost.setTags(new HashSet<>());
        samplePost.setUser(sampleUser);
    }

    // -------------------------------------------------------------------------
    // findPosts()
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("findPosts()")
    class FindPosts {

        @Test
        @DisplayName("returns page of posts for given filter")
        void withFilter_returnsPage() {
            PostFilter filter = new PostFilter();
            filter.setPage(0);
            filter.setSize(10);

            Page<Post> expected = new PageImpl<>(List.of(samplePost));
            when(postRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(expected);

            Page<Post> result = postService.findPosts(filter);

            assertThat(result.getContent()).containsExactly(samplePost);
        }

        @Test
        @DisplayName("uses defaults when filter is null")
        void nullFilter_usesDefaultPagination() {
            Page<Post> expected = new PageImpl<>(List.of(samplePost));
            when(postRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(expected);

            Page<Post> result = postService.findPosts(null);

            assertThat(result).isNotNull();

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
            verify(postRepository).findAll(any(Specification.class), pageableCaptor.capture());
            Pageable captured = pageableCaptor.getValue();
            assertThat(captured.getPageNumber()).isZero();
            assertThat(captured.getPageSize()).isEqualTo(12);
        }

        @Test
        @DisplayName("sorts by createdAt descending")
        void sortsByCreatedAtDesc() {
            PostFilter filter = new PostFilter();
            filter.setPage(0);
            filter.setSize(5);

            when(postRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(Page.empty());

            postService.findPosts(filter);

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
            verify(postRepository).findAll(any(Specification.class), captor.capture());

            Sort.Order order = captor.getValue().getSort().getOrderFor("createdAt");
            assertThat(order).isNotNull();
            assertThat(order.isDescending()).isTrue();
        }
    }

    // -------------------------------------------------------------------------
    // getPosts()
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("getPosts()")
    class GetPosts {

        @Test
        @DisplayName("delegates to repository and returns projected page")
        void returnsProjectedPage() {
            Pageable pageable = PageRequest.of(0, 5);
            Page<PostDto> expected = Page.empty();
            when(postRepository.findAllProjected(pageable)).thenReturn(expected);

            Page<PostDto> result = postService.getPosts(pageable);

            assertThat(result).isEqualTo(expected);
            verify(postRepository).findAllProjected(pageable);
        }
    }

    // -------------------------------------------------------------------------
    // findPostById()
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("findPostById()")
    class FindPostById {

        @Test
        @DisplayName("returns post when found")
        void found_returnsPost() {
            when(postRepository.findById(1)).thenReturn(Optional.of(samplePost));

            Post result = postService.findPostById(1);

            assertThat(result).isEqualTo(samplePost);
            verify(postMetricsService).incrementView(1);
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when post does not exist")
        void notFound_throwsResourceNotFoundException() {
            when(postRepository.findById(99)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> postService.findPostById(99))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Post not found with ID: 99");
        }

        @Test
        @DisplayName("throws ValidationException when id is zero")
        void idIsZero_throwsValidationException() {
            assertThatThrownBy(() -> postService.findPostById(0))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Invalid post ID");
            verifyNoInteractions(postRepository);
        }

        @Test
        @DisplayName("throws ValidationException when id is negative")
        void negativeId_throwsValidationException() {
            assertThatThrownBy(() -> postService.findPostById(-1))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Invalid post ID");
        }

    }

    // -------------------------------------------------------------------------
    // findPostsByUserId()
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("findPostsByUserId()")
    class FindPostsByUserId {

        @Test
        @DisplayName("delegates to repository with correct pageable")
        void delegatesToRepository() {
            Page<Post> expected = Page.empty();
            when(postRepository.findByUserId(eq(1L), any(Pageable.class))).thenReturn(expected);

            Page<Post> result = postService.findPostsByUserId(1L, 0, 10);

            assertThat(result).isEqualTo(expected);

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
            verify(postRepository).findByUserId(eq(1L), pageableCaptor.capture());
            assertThat(pageableCaptor.getValue().getPageNumber()).isZero();
            assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(10);
        }
    }

    @Nested
    @DisplayName("createPost()")
    class CreatePost {

        @Test
        @DisplayName("creates post successfully")
        void validPost_createsSuccessfully() {
            Set<String> tagNames = Set.of("java");
            Set<Tag> resolvedTags = Set.of(sampleTag);

            Authentication auth = mock(Authentication.class);
            when(auth.getPrincipal()).thenReturn(1L);
            SecurityContext securityContext = mock(SecurityContext.class);
            when(securityContext.getAuthentication()).thenReturn(auth);
            SecurityContextHolder.setContext(securityContext);

            when(tagService.findOrCreateTagsByName(tagNames)).thenReturn(resolvedTags);
            when(userService.findByUserId(1L)).thenReturn(sampleUser);
            when(postRepository.save(any(Post.class))).thenReturn(samplePost);

            try (MockedStatic<PostValidator> validatorMock = mockStatic(PostValidator.class)) {
                validatorMock.when(() -> PostValidator.validateForCreation(any(Post.class)))
                        .thenAnswer(inv -> null);

                Post result = postService.createPost(samplePost, tagNames);

                assertThat(samplePost.getTags()).isEqualTo(resolvedTags);
                assertThat(samplePost.getUser()).isEqualTo(sampleUser);
                assertThat(result).isEqualTo(samplePost);
                verify(postRepository, times(1)).save(any(Post.class));
            }
            try (MockedStatic<PostValidator> validatorMock = mockStatic(PostValidator.class)) {
                validatorMock.when(() -> PostValidator.validateForCreation(samplePost))
                        .thenThrow(new ValidationException("title required"));

                assertThatThrownBy(() -> postService.createPost(samplePost, Set.of()))
                        .isInstanceOf(ValidationException.class)
                        .hasMessageContaining("title required");
            }
        }
    }

    @Nested
    @DisplayName("updatePost()")
    class UpdatePost {

        @Test
        @DisplayName("updates post and sets updatedAt timestamp")
        void validUpdate_setsUpdatedAt() {
            when(postRepository.findById(samplePost.getId())).thenReturn(Optional.of(samplePost));
            try (MockedStatic<PostValidator> validatorMock = mockStatic(PostValidator.class)) {
                validatorMock.when(() -> PostValidator.validateForUpdate(any(Post.class)))
                        .thenAnswer(inv -> null);

                Instant before = Instant.now();
                UpdatePostRequest updateRequest = new UpdatePostRequest();
                updateRequest.setTitle("Updated Title");
                postService.updatePost(samplePost.getId(), updateRequest);
                Instant after = Instant.now();

                assertThat(samplePost.getUpdatedAt()).isBetween(before, after);
                verify(postRepository).save(samplePost);
            }
        }

        @Test
        @DisplayName("clears and sets new tags when newTagIds is provided")
        void withNewTagIds_replacesTagSet() {
            Tag newTag = new Tag();
            newTag.setId(2);
            newTag.setName("spring");

            samplePost.getTags().add(sampleTag);

            when(postRepository.findById(samplePost.getId())).thenReturn(Optional.of(samplePost));
            when(tagRepository.findAllById(anyCollection())).thenReturn(List.of(newTag));
            try (MockedStatic<PostValidator> validatorMock = mockStatic(PostValidator.class)) {
                validatorMock.when(() -> PostValidator.validateForUpdate(any(Post.class)))
                        .thenAnswer(inv -> null);

                UpdatePostRequest request = new UpdatePostRequest();
                request.setTagIds(Set.of(2));
                postService.updatePost(samplePost.getId(), request);

                assertThat(samplePost.getTags()).containsExactly(newTag);
                verify(postRepository).save(samplePost);
                verify(postMapper).updateEntity(request, samplePost);
            }
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when one or more tag IDs not found")
        void missingTagId_throwsResourceNotFoundException() {
            when(postRepository.findById(samplePost.getId())).thenReturn(Optional.of(samplePost));
            when(tagRepository.findAllById(anyCollection())).thenReturn(List.of(sampleTag));
            try (MockedStatic<PostValidator> validatorMock = mockStatic(PostValidator.class)) {
                validatorMock.when(() -> PostValidator.validateForUpdate(any(Post.class)))
                        .thenAnswer(inv -> null);

                UpdatePostRequest request = new UpdatePostRequest();
                request.setTagIds(Set.of(1, 999));
                assertThatThrownBy(() -> postService.updatePost(samplePost.getId(), request))
                        .isInstanceOf(ResourceNotFoundException.class)
                        .hasMessageContaining("One or more tags not found");
            }
        }

        @Test
        @DisplayName("clears tags when newTagIds is an empty list")
        void emptyTagIdList_clearsTags() {
            samplePost.getTags().add(sampleTag);

            when(postRepository.findById(samplePost.getId())).thenReturn(Optional.of(samplePost));

            try (MockedStatic<PostValidator> validatorMock = mockStatic(PostValidator.class)) {
                validatorMock.when(() -> PostValidator.validateForUpdate(any(Post.class)))
                        .thenAnswer(inv -> null);

                UpdatePostRequest request = new UpdatePostRequest();
                request.setTagIds(Set.of());
                postService.updatePost(samplePost.getId(), request);

                assertThat(samplePost.getTags()).isEmpty();
                verify(postRepository).save(samplePost);
                verify(postMapper).updateEntity(request, samplePost);
            }
        }

    }

    @Nested
    @DisplayName("deletePost()")
    class DeletePost {

        @Test
        @DisplayName("deletes post when it exists")
        void found_deletesSuccessfully() {
            when(postRepository.existsById(1)).thenReturn(true);

            postService.deletePost(1);

            verify(postRepository).deleteById(1);
        }

        @Test
        @DisplayName("throws ValidationException when id is zero")
        void idIsZero_throwsValidationException() {
            assertThatThrownBy(() -> postService.deletePost(0))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Invalid post ID");
            verifyNoInteractions(postRepository);
        }

        @Test
        @DisplayName("throws ValidationException when id is negative")
        void negativeId_throwsValidationException() {
            assertThatThrownBy(() -> postService.deletePost(-5))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Invalid post ID");
        }
    }
}
