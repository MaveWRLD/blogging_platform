package org.amalitech.service;

import org.amalitech.algorithm.TrendingSortAlgorithm;
import org.amalitech.dtos.postDtos.PostDto;
import org.amalitech.dtos.postDtos.PostFilter;
import org.amalitech.dtos.postDtos.UpdatePostRequest;
import org.amalitech.entities.Comment;
import org.amalitech.entities.Post;
import org.amalitech.entities.Tag;
import org.amalitech.entities.User;
import org.amalitech.enums.PostStatus;
import org.amalitech.exception.ResourceNotFoundException;
import org.amalitech.exception.ValidationException;
import org.amalitech.mappers.PostMapper;
import org.amalitech.repositories.PostRepository;
import org.amalitech.repositories.TagRepository;
import org.amalitech.repositories.UserRepository;
import org.amalitech.util.PostValidator;
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
        reset(postRepository, tagRepository, commentService);
    }

    @Mock
    private PostRepository postRepository;

    @Mock
    private TagRepository tagRepository;

    @Mock
    private CommentService commentService;

    @Mock
    private TrendingSortAlgorithm trendingAlgorithm;

    @Mock
    private UserRepository userRepository;

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
        sampleUser =  User.registerReader(
                "author",
                "john@example.com",
                "plainpassword",
                "John",
                "Doe"
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
        @DisplayName("returns map of post to comments when found")
        void found_returnsMap() {
            List<Comment> comments = List.of(new Comment());
            when(postRepository.findById(1)).thenReturn(Optional.of(samplePost));
            when(commentService.getCommentsByPostId(1)).thenReturn(comments);

            Map<Post, List<Comment>> result = postService.findPostById(1);

            assertThat(result).containsKey(samplePost);
            assertThat(result.get(samplePost)).isEqualTo(comments);
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

        @Test
        @DisplayName("returns empty comments list when post has no comments")
        void found_withNoComments_returnsEmptyList() {
            when(postRepository.findById(1)).thenReturn(Optional.of(samplePost));
            when(commentService.getCommentsByPostId(1)).thenReturn(List.of());

            Map<Post, List<Comment>> result = postService.findPostById(1);

            assertThat(result.get(samplePost)).isEmpty();
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
            Page<PostDto> expected = Page.empty();
            when(postRepository.findPostsByUserId(eq(1L), any(Pageable.class))).thenReturn(expected);

            Page<PostDto> result = postService.findPostsByUserId(1L, 0, 10);

            assertThat(result).isEqualTo(expected);

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
            verify(postRepository).findPostsByUserId(eq(1L), pageableCaptor.capture());
            assertThat(pageableCaptor.getValue().getPageNumber()).isZero();
            assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(10);
        }
    }

    // -------------------------------------------------------------------------
    // getTopTrendingPosts()
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("getTopTrendingPosts()")
    class GetTopTrendingPosts {

        @Test
        @DisplayName("returns empty page when no recent posts exist")
        void noCandidates_returnsEmptyPage() {
            Pageable pageable = PageRequest.of(0, 10);
            when(postRepository.findRecentPublishedPosts(any(Instant.class), eq(pageable)))
                    .thenReturn(Page.empty());

            Page<Post> result = postService.getTopTrendingPosts(5, pageable);

            assertThat(result.getContent()).isEmpty();
            verifyNoInteractions(trendingAlgorithm);
        }

        @Test
        @DisplayName("delegates to trending algorithm with correct limit")
        void hasCandidates_callsAlgorithm() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Post> candidates = new PageImpl<>(List.of(samplePost), pageable, 1);
            when(postRepository.findRecentPublishedPosts(any(Instant.class), eq(pageable)))
                    .thenReturn(candidates);
            when(trendingAlgorithm.getTopTrending(anyList(), eq(5))).thenReturn(List.of(samplePost));

            Page<Post> result = postService.getTopTrendingPosts(5, pageable);

            assertThat(result.getContent()).containsExactly(samplePost);
            verify(trendingAlgorithm).getTopTrending(anyList(), eq(5));
        }

        @Test
        @DisplayName("defaults limit to 10 when limit is zero")
        void limitIsZero_defaultsToTen() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Post> candidates = new PageImpl<>(List.of(samplePost), pageable, 1);
            when(postRepository.findRecentPublishedPosts(any(Instant.class), eq(pageable)))
                    .thenReturn(candidates);
            when(trendingAlgorithm.getTopTrending(anyList(), eq(10))).thenReturn(List.of(samplePost));

            postService.getTopTrendingPosts(0, pageable);

            verify(trendingAlgorithm).getTopTrending(anyList(), eq(10));
        }

        @Test
        @DisplayName("defaults limit to 10 when limit exceeds 100")
        void limitExceeds100_defaultsToTen() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Post> candidates = new PageImpl<>(List.of(samplePost), pageable, 1);
            when(postRepository.findRecentPublishedPosts(any(Instant.class), eq(pageable)))
                    .thenReturn(candidates);
            when(trendingAlgorithm.getTopTrending(anyList(), eq(10))).thenReturn(List.of(samplePost));

            postService.getTopTrendingPosts(101, pageable);

            verify(trendingAlgorithm).getTopTrending(anyList(), eq(10));
        }

        @Test
        @DisplayName("preserves total elements from candidate page")
        void preservesTotalElements() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Post> candidates = new PageImpl<>(List.of(samplePost), pageable, 50);
            when(postRepository.findRecentPublishedPosts(any(Instant.class), eq(pageable)))
                    .thenReturn(candidates);
            when(trendingAlgorithm.getTopTrending(anyList(), eq(10))).thenReturn(List.of(samplePost));

            Page<Post> result = postService.getTopTrendingPosts(10, pageable);

            assertThat(result.getTotalElements()).isEqualTo(50);
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

            // Mock security context
            Authentication auth = mock(Authentication.class);
            when(auth.getPrincipal()).thenReturn(1L);
            SecurityContext securityContext = mock(SecurityContext.class);
            when(securityContext.getAuthentication()).thenReturn(auth);
            SecurityContextHolder.setContext(securityContext);

            when(tagService.findOrCreateTagsByName(tagNames)).thenReturn(resolvedTags);
            when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
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

                verify(postRepository, never()).save(any());
            }
        }
    }

    // -------------------------------------------------------------------------
    // updatePost()
    // -------------------------------------------------------------------------
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
            when(tagRepository.findAllById(List.of(2))).thenReturn(List.of(newTag));

            try (MockedStatic<PostValidator> validatorMock = mockStatic(PostValidator.class)) {
                validatorMock.when(() -> PostValidator.validateForUpdate(any(Post.class)))
                        .thenAnswer(inv -> null);

                UpdatePostRequest request = new UpdatePostRequest();
                request.setTagIds(List.of(2));
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
            when(tagRepository.findAllById(List.of(1, 999))).thenReturn(List.of(sampleTag)); // only 1 returned

            try (MockedStatic<PostValidator> validatorMock = mockStatic(PostValidator.class)) {
                validatorMock.when(() -> PostValidator.validateForUpdate(any(Post.class)))
                        .thenAnswer(inv -> null);

                UpdatePostRequest request = new UpdatePostRequest();
                request.setTagIds(List.of(1, 999));
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
                request.setTagIds(List.of());
                postService.updatePost(samplePost.getId(), request);

                assertThat(samplePost.getTags()).isEmpty();
                verify(postRepository).save(samplePost);
                verify(postMapper).updateEntity(request, samplePost);
            }
        }

    }

    // -------------------------------------------------------------------------
    // deletePost()
    // -------------------------------------------------------------------------
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
        @DisplayName("throws ResourceNotFoundException when post does not exist")
        void notFound_throwsResourceNotFoundException() {
            when(postRepository.existsById(99)).thenReturn(false);

            assertThatThrownBy(() -> postService.deletePost(99))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Post not found with id: 99");

            verify(postRepository, never()).deleteById(anyInt());
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
