package org.amalitech.post;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.amalitech.comment.dto.CommentDto;
import org.amalitech.post.dto.PagedPostsResponse;
import org.amalitech.post.dto.PostDto;
import org.amalitech.post.dto.PostFilter;
import org.amalitech.post.dto.PostResponse;
import org.amalitech.user.dto.UserDto;
import org.amalitech.comment.Comment;
import org.amalitech.post.Post;
import org.amalitech.user.Role;
import org.amalitech.user.User;
import org.amalitech.common.exception.ResourceNotFoundException;
import org.amalitech.common.exception.ValidationException;
import org.amalitech.comment.CommentMapper;
import org.amalitech.post.PostMapper;
import org.amalitech.user.UserMapper;
import org.amalitech.comment.CommentService;
import org.amalitech.post.PostService;
import org.amalitech.post.graphqlResolver.PostQueryResolver;
import org.amalitech.user.UserService;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

@ExtendWith(MockitoExtension.class)
@DisplayName("PostQueryResolver")
class PostQueryResolverTest {

   @Mock private PostService postService;
   @Mock private CommentService commentService;
   @Mock private PostMapper postMapper;
   @Mock private UserService userService;
   @Mock private CommentMapper commentMapper;
   @Mock private UserMapper userMapper;

   @InjectMocks
   private PostQueryResolver resolver;

   private Post buildPost(Long userId) {
       Set<Role> roles = new HashSet<>();
       User user = User.registerReader(
               "user-" + userId,
               "user" + userId + "@example.com",
               "password",
               "First",
               "Last",
               roles
       );
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
           PostFilter filter = filterWith(0, 0);
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
           Page<Post> page = new PageImpl<>(Collections.emptyList(),
                   org.springframework.data.domain.PageRequest.of(0, 10), 21);
           when(postService.findPosts(filter)).thenReturn(page);

           PagedPostsResponse response = resolver.posts(filter);

           assertThat(response.getTotalPages()).isEqualTo(3);
       }
   }

   @Nested
   @DisplayName("post() – single post retrieval")
   class PostQuery {

       @Test
       @DisplayName("returns PostResponse with post, user and comments")
       void post_found_returnsFullResponse() {
           Post post = buildPost(42L);
           Comment comment = new Comment();

           PostDto postDto = mock(PostDto.class);
           CommentDto commentDto = mock(CommentDto.class);
           User user = User.registerReader(
                   "user-42",
                   "user42@example.com",
                   "password",
                   "First",
                   "Last",
                   Set.of()
           );
           user.setId(42L);
           UserDto userDto = mock(UserDto.class);

           when(postService.findPostById(1)).thenReturn(post);
           when(commentService.getCommentsByPostId(1)).thenReturn(List.of(comment));
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

           when(postService.findPostById(2)).thenReturn(post);
           when(commentService.getCommentsByPostId(2)).thenReturn(Collections.emptyList());
           when(postMapper.toDto(post)).thenReturn(mock(PostDto.class));
           when(userService.findByUserId(5L)).thenReturn(mock(org.amalitech.user.User.class));
           when(userMapper.toDto(any())).thenReturn(mock(org.amalitech.user.dto.UserDto.class));

           PostResponse response = resolver.post(2);

           assertThat(response.getComments()).isEmpty();
       }

       @Test
       @DisplayName("propagates ResourceNotFoundException when postService can't find the post")
       void post_serviceThrowsNotFound_propagates() {
           when(postService.findPostById(99)).thenThrow(new ResourceNotFoundException("Post not found with ID: 99"));

           assertThatThrownBy(() -> resolver.post(99))
                   .isInstanceOf(ResourceNotFoundException.class)
                   .hasMessageContaining("99");
       }

       @Test
       @DisplayName("maps all comments through commentMapper")
       void post_multipleComments_allAreMapped() {
           Post post = buildPost(1L);
           Comment c1 = new Comment();
           Comment c2 = new Comment();
           CommentDto cd1 = mock(CommentDto.class);
           CommentDto cd2 = mock(CommentDto.class);

           when(postService.findPostById(3)).thenReturn(post);
           when(commentService.getCommentsByPostId(3)).thenReturn(List.of(c1, c2));
           when(postMapper.toDto(post)).thenReturn(mock(PostDto.class));
           when(commentMapper.toDto(c1)).thenReturn(cd1);
           when(commentMapper.toDto(c2)).thenReturn(cd2);
           when(userService.findByUserId(1L)).thenReturn(mock(org.amalitech.user.User.class));
           when(userMapper.toDto(any())).thenReturn(mock(org.amalitech.user.dto.UserDto.class));

           PostResponse response = resolver.post(3);

           assertThat(response.getComments()).containsExactly(cd1, cd2);
       }

       @Test
       @DisplayName("looks up user using the post's user id")
       void post_fetchesUserByPostUserId() {
           Post post = buildPost(77L);

           when(postService.findPostById(4)).thenReturn(post);
           when(commentService.getCommentsByPostId(4)).thenReturn(Collections.emptyList());
           when(postMapper.toDto(post)).thenReturn(mock(PostDto.class));
           when(userService.findByUserId(77L)).thenReturn(mock(org.amalitech.user.User.class));
           when(userMapper.toDto(any())).thenReturn(mock(org.amalitech.user.dto.UserDto.class));

           resolver.post(4);

           verify(userService).findByUserId(77L);
       }
   }
}
