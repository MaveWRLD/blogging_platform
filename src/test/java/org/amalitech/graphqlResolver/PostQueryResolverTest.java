package org.amalitech.graphqlResolver;

import org.amalitech.dtos.postDtos.PagedPostsResponse;
import org.amalitech.dtos.postDtos.PostFilter;
import org.amalitech.mappers.CommentMapper;
import org.amalitech.mappers.PostMapper;
import org.amalitech.mappers.UserMapper;
import org.amalitech.models.Tag;
import org.amalitech.service.PostService;
import org.amalitech.service.TagService;
import org.amalitech.service.UserService;
import org.apache.coyote.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PostQueryResolverTest {
    @Mock
    private PostService postService;
    @Mock
    private PostMapper postMapper;
    @Mock
    private UserService userService;
    @Mock
    private TagService tagService;
    @Mock
    private CommentMapper commentMapper;
    @Mock
    private UserMapper userMapper;
    private PostQueryResolver resolver;
    @BeforeEach
    void setUp() {
        resolver = new PostQueryResolver(postService, postMapper, userService, tagService, commentMapper, userMapper);
    }
    @Test
    void posts_returnsPagedResponse_withValidFilter() throws Exception {
        PostFilter filter = new PostFilter();
        filter.setPage(0);
        filter.setSize(12);

        when(postService.findPosts(filter)).thenReturn(List.of());
        when(postService.postCount(filter)).thenReturn(0);

        PagedPostsResponse resp = resolver.posts(filter);

        assertThat(resp).isNotNull();
        assertThat(resp.getPosts()).isEmpty();
        assertThat(resp.getPage()).isEqualTo(0);
        assertThat(resp.getSize()).isEqualTo(12);
        assertThat(resp.getTotal()).isEqualTo(0L);
    }
    @Test
    void posts_withInvalidPagination_throwsBadRequest() {
        PostFilter filter = new PostFilter();
        filter.setPage(-1);
        assertThatThrownBy(() -> resolver.posts(filter))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Invalid pagination parameters");
    }

    @Test
    void tags_returnsTagsForPost() {
        org.amalitech.dtos.postDtos.PostDto postDto = new org.amalitech.dtos.postDtos.PostDto();
        postDto.setId(1);
        List<Tag> tags = List.of(new Tag());
        when(tagService.findTagsByPostId(1)).thenReturn(tags);
        List<Tag> result = resolver.tags(postDto);
        assertThat(result).isEqualTo(tags);
    }
}