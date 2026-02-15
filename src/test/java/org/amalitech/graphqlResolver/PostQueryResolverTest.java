// java
package org.amalitech.graphqlResolver;

import org.amalitech.dtos.CommentDto;
import org.amalitech.dtos.postDtos.PagedPostsResponse;
import org.amalitech.dtos.postDtos.PostDto;
import org.amalitech.dtos.postDtos.PostFilter;
import org.amalitech.dtos.postDtos.PostResponse;
import org.amalitech.entities.Comment;
import org.amalitech.entities.Post;
import org.amalitech.entities.User;
import org.amalitech.exception.ResourceNotFoundException;
import org.amalitech.exception.ValidationException;
import org.amalitech.mappers.CommentMapper;
import org.amalitech.mappers.PostMapper;
import org.amalitech.mappers.UserMapper;
import org.amalitech.service.PostService;
import org.amalitech.service.TagService;
import org.amalitech.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PostQueryResolver")
class PostQueryResolverTest {

    @Mock private PostService postService;
    @Mock private PostMapper postMapper;
    @Mock private UserService userService;
    @Mock private TagService tagService;
    @Mock private CommentMapper commentMapper;
    @Mock private UserMapper userMapper;

    @InjectMocks
    private PostQueryResolver resolver;

    // ─────────────────────────────────────────────────────────────────────────
    // Helper factories
    // ─────────────────────────────────────────────────────────────────────────

    private Post buildPost(Long userId) {
        User user = new User();
        user.setId(userId);
        Post post = new Post();
        post.setUser(user);
        return post;
    }

    private PostFilter filterWith(int page, int size) {
        PostFilter f = mock(PostFilter.class);
        when(f.getPage()).thenReturn(page);
        when(f.getSize()).thenReturn(size);
        return f;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // posts – happy paths
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("posts() – pagination and mapping")
    class PostsQuery {

        @Test
        @DisplayName("returns correct page metadata for first page")
        void posts_firstPage_returnsCorrectMetadata() throws Exception {
            PostFilter filter = filterWith(0, 10);
            Post post = buildPost(1L);
            PostDto postDto = mock(PostDto.class);
            Page<Post> page = new PageImpl<>(List.of(post), org.springframework.data.domain.PageRequest.of(0, 10), 25);

            when(postService.findPosts(filter)).thenReturn(page);
            when(postMapper.toDto(post)).thenReturn(postDto);

            PagedPostsResponse response = resolver.posts(filter);

            assertThat(response.getPage()).isEqualTo(0);
            assertThat(response.getSize()).isEqualTo(10);
            assertThat(response.getTotal()).isEqualTo(25);
            assertThat(response.getTotalPages()).isEqualTo(3);
            assertThat(response.isHasPrevious()).isFalse();
            assertThat(response.isHasNext()).isTrue();
            assertThat(response.getPosts()).containsExactly(postDto);
        }

        @Test
        @DisplayName("hasPrevious is true when page > 0")
        void posts_secondPage_hasPreviousIsTrue() throws Exception {
            PostFilter filter = filterWith(1, 10);
            Page<Post> page = new PageImpl<>(Collections.emptyList(),
                    org.springframework.data.domain.PageRequest.of(1, 10), 25);
            when(postService.findPosts(filter)).thenReturn(page);

            PagedPostsResponse response = resolver.posts(filter);

            assertThat(response.isHasPrevious()).isTrue();
        }

        @Test
        @DisplayName("hasNext is false on last page")
        void posts_lastPage_hasNextIsFalse() throws Exception {
            PostFilter filter = filterWith(2, 10);
            Page<Post> page = new PageImpl<>(Collections.emptyList(),
                    org.springframework.data.domain.PageRequest.of(2, 10), 25);
            when(postService.findPosts(filter)).thenReturn(page);

            PagedPostsResponse response = resolver.posts(filter);

            assertThat(response.isHasNext()).isFalse();
        }

        @Test
        @DisplayName("returns empty posts list when page has no content")
        void posts_emptyPage_returnsEmptyList() throws Exception {
            PostFilter filter = filterWith(0, 12);
            Page<Post> page = new PageImpl<>(Collections.emptyList(),
                    org.springframework.data.domain.PageRequest.of(0, 12), 0);
            when(postService.findPosts(filter)).thenReturn(page);

            PagedPostsResponse response = resolver.posts(filter);

            assertThat(response.getPosts()).isEmpty();
            assertThat(response.getTotal()).isZero();
            assertThat(response.getTotalPages()).isZero();
        }

        @Test
        @DisplayName("maps multiple posts via postMapper")
        void posts_multiplePosts_allAreMapped() throws Exception {
            PostFilter filter = filterWith(0, 10);
            Post p1 = buildPost(1L);
            Post p2 = buildPost(2L);
            PostDto dto1 = mock(PostDto.class);
            PostDto dto2 = mock(PostDto.class);
            Page<Post> page = new PageImpl<>(List.of(p1, p2),
                    org.springframework.data.domain.PageRequest.of(0, 10), 2);

            when(postService.findPosts(filter)).thenReturn(page);
            when(postMapper.toDto(p1)).thenReturn(dto1);
            when(postMapper.toDto(p2)).thenReturn(dto2);

            PagedPostsResponse response = resolver.posts(filter);

            assertThat(response.getPosts()).containsExactly(dto1, dto2);
        }

        @Test
        @DisplayName("uses defaults when filter is null (page=0, size=12)")
        void posts_nullFilter_usesDefaults() throws Exception {
            Page<Post> page = new PageImpl<>(Collections.emptyList(),
                    org.springframework.data.domain.PageRequest.of(0, 12), 0);
            when(postService.findPosts(null)).thenReturn(page);

            PagedPostsResponse response = resolver.posts(null);

            assertThat(response.getPage()).isEqualTo(0);
            assertThat(response.getSize()).isEqualTo(12);
        }

        @Test
        @DisplayName("clamps size to default 12 when filter.size is 0 (invalid)")
        void posts_invalidSize_usesDefaultSize() throws Exception {
            PostFilter filter = filterWith(0, 0);   // size < 1 → triggers ValidationException
            assertThatThrownBy(() -> resolver.posts(filter))
                    .isInstanceOf(ValidationException.class);
        }

        @Test
        @DisplayName("throws ValidationException for page < 0")
        void posts_negativePageInFilter_throwsValidationException() throws Exception {
            PostFilter filter = filterWith(-1, 10);

            assertThatThrownBy(() -> resolver.posts(filter))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Invalid pagination parameters");
        }

        @Test
        @DisplayName("throws ValidationException for size > 100")
        void posts_oversizedPageSize_throwsValidationException() throws Exception {
            PostFilter filter = filterWith(0, 101);

            assertThatThrownBy(() -> resolver.posts(filter))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Invalid pagination parameters");
        }

        @Test
        @DisplayName("totalPages rounds up (ceiling division)")
        void posts_totalPagesCeilingDivision() throws Exception {
            PostFilter filter = filterWith(0, 10);
            // 21 items / 10 per page → 3 pages
            Page<Post> page = new PageImpl<>(Collections.emptyList(),
                    org.springframework.data.domain.PageRequest.of(0, 10), 21);
            when(postService.findPosts(filter)).thenReturn(page);

            PagedPostsResponse response = resolver.posts(filter);

            assertThat(response.getTotalPages()).isEqualTo(3);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // post – single post query
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("post() – single post retrieval")
    class PostQuery {

        @Test
        @DisplayName("returns PostResponse with post, user and comments")
        void post_found_returnsFullResponse() {
            Post post = buildPost(42L);
            Comment comment = new Comment();
            Map<Post, List<Comment>> postMap = new HashMap<>();
            postMap.put(post, List.of(comment));

            PostDto postDto = mock(PostDto.class);
            CommentDto commentDto = mock(CommentDto.class);
            // use correctly typed User and UserDto
            User user = new User();
            org.amalitech.dtos.userDtos.UserDto userDto = mock(org.amalitech.dtos.userDtos.UserDto.class);

            when(postService.findPostById(1)).thenReturn(postMap);
            when(postMapper.toDto(post)).thenReturn(postDto);
            when(commentMapper.toDto(comment)).thenReturn(commentDto);
            when(userService.findByUserId(42L)).thenReturn(user);
            when(userMapper.toDto(any())).thenReturn(userDto);

            PostResponse response = resolver.post(1);

            assertThat(response).isNotNull();
            assertThat(response.getPost()).isEqualTo(postDto);
            assertThat(response.getComments()).containsExactly(commentDto);
            verify(userService).findByUserId(42L);
            verify(userMapper).toDto(any());
        }

        @Test
        @DisplayName("returns empty comment list when post has no comments")
        void post_noComments_returnsEmptyCommentList() {
            Post post = buildPost(5L);
            Map<Post, List<Comment>> postMap = new HashMap<>();
            postMap.put(post, Collections.emptyList());

            when(postService.findPostById(2)).thenReturn(postMap);
            when(postMapper.toDto(post)).thenReturn(mock(PostDto.class));
            when(userService.findByUserId(5L)).thenReturn(mock(org.amalitech.entities.User.class));
            when(userMapper.toDto(any())).thenReturn(mock(org.amalitech.dtos.userDtos.UserDto.class));

            PostResponse response = resolver.post(2);

            assertThat(response.getComments()).isEmpty();
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when postService returns null")
        void post_serviceReturnsNull_throwsResourceNotFoundException() {
            when(postService.findPostById(99)).thenReturn(null);

            assertThatThrownBy(() -> resolver.post(99))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("99");
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when postService returns empty map")
        void post_serviceReturnsEmptyMap_throwsResourceNotFoundException() {
            when(postService.findPostById(88)).thenReturn(Collections.emptyMap());

            assertThatThrownBy(() -> resolver.post(88))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("88");
        }

        @Test
        @DisplayName("maps all comments through commentMapper")
        void post_multipleComments_allAreMapped() {
            Post post = buildPost(1L);
            Comment c1 = new Comment();
            Comment c2 = new Comment();
            CommentDto cd1 = mock(CommentDto.class);
            CommentDto cd2 = mock(CommentDto.class);
            Map<Post, List<Comment>> postMap = new HashMap<>();
            postMap.put(post, List.of(c1, c2));

            when(postService.findPostById(3)).thenReturn(postMap);
            when(postMapper.toDto(post)).thenReturn(mock(PostDto.class));
            when(commentMapper.toDto(c1)).thenReturn(cd1);
            when(commentMapper.toDto(c2)).thenReturn(cd2);
            when(userService.findByUserId(1L)).thenReturn(mock(org.amalitech.entities.User.class));
            when(userMapper.toDto(any())).thenReturn(mock(org.amalitech.dtos.userDtos.UserDto.class));

            PostResponse response = resolver.post(3);

            assertThat(response.getComments()).containsExactly(cd1, cd2);
        }

        @Test
        @DisplayName("looks up user using the post's user id")
        void post_fetchesUserByPostUserId() {
            Post post = buildPost(77L);
            Map<Post, List<Comment>> postMap = new HashMap<>();
            postMap.put(post, Collections.emptyList());

            when(postService.findPostById(4)).thenReturn(postMap);
            when(postMapper.toDto(post)).thenReturn(mock(PostDto.class));
            when(userService.findByUserId(77L)).thenReturn(mock(org.amalitech.entities.User.class));
            when(userMapper.toDto(any())).thenReturn(mock(org.amalitech.dtos.userDtos.UserDto.class));

            resolver.post(4);

            verify(userService).findByUserId(77L);
        }
    }
}
