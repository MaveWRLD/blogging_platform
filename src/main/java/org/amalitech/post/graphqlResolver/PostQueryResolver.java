package org.amalitech.post.graphqlResolver;

import org.amalitech.comment.dto.CommentDto;
import org.amalitech.post.dto.PagedPostsResponse;
import org.amalitech.post.dto.PostDto;
import org.amalitech.post.dto.PostFilter;
import org.amalitech.post.dto.PostResponse;
import org.amalitech.exception.ValidationException;
import org.amalitech.comment.CommentMapper;
import org.amalitech.comment.CommentService;
import org.amalitech.post.PostMapper;
import org.amalitech.mappers.UserMapper;
import org.amalitech.comment.Comment;
import org.amalitech.post.Post;
import org.amalitech.post.PostService;
// TODO(feature-migration): TagService/UserService still live in the old layered
// org.amalitech.service package until Tag/User get their own package-by-feature pass.
import org.amalitech.post.tag.TagService;
import org.amalitech.service.UserService;
import org.apache.coyote.BadRequestException;
import org.springframework.data.domain.Page;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.stream.Collectors;


@Controller
public class PostQueryResolver {

    private final TagService tagService;
    private final PostService postService;
    private final CommentService commentService;
    private final UserService userService;
    private final PostMapper postMapper;
    private final CommentMapper commentMapper;
    private final UserMapper userMapper;

    public PostQueryResolver(PostService postService, CommentService commentService, PostMapper postMapper, UserService userService, TagService tagService, CommentMapper commentMapper, UserMapper userMapper) {
        this.postService = postService;
        this.commentService = commentService;
        this.postMapper = postMapper;
        this.userService = userService;
        this.tagService = tagService;
        this.commentMapper = commentMapper;
        this.userMapper = userMapper;
    }

    @QueryMapping
    public PagedPostsResponse posts(@Argument PostFilter filter) throws BadRequestException {

        Pagination getPagination = getPagination(filter);

        List<PostDto> posts = getPagination.pagedPosts().getContent().stream()
                .map(postMapper::toDto)
                .collect(Collectors.toList());

        return new PagedPostsResponse(
                posts,
                getPagination.page(),
                getPagination.size(),
                getPagination.total(),
                getPagination.totalPages(),
                getPagination.hasPrevious(),
                getPagination.hasNext()
        );
    }

    @QueryMapping
    public PostResponse post(@Argument int id) {
        Post post = postService.findPostById(id);
        List<Comment> comments = commentService.getCommentsByPostId(id);

        PostDto postDto = postMapper.toDto(post);
        List<CommentDto> commentDtos = comments.stream().map(commentMapper::toDto).collect(Collectors.toList());

        var user = userService.findByUserId(post.getUser().getId());
        var userDto = userMapper.toDto(user);

        return new PostResponse(postDto, userDto, commentDtos);
    }

    private Pagination getPagination(PostFilter filter) throws ValidationException {
        int page = (filter != null && filter.getPage() >= 0) ? filter.getPage() : 0;
        int size = (filter != null && filter.getSize() >= 1 && filter.getSize() <= 100)
                ? filter.getSize()
                : 12;

        if (filter != null && (filter.getPage() < 0 || filter.getSize() < 1 || filter.getSize() > 100)) {
            throw new ValidationException("Invalid pagination parameters");
        }

        Page<Post> pagedPosts = postService.findPosts(filter);

        long total = pagedPosts.getTotalElements();
        if (total < 0) total = 0;

        boolean hasNext = (long) (page + 1) * size < total;
        boolean hasPrevious = page > 0;

        int totalPages = (int) Math.ceil((double) total / size);
        return new Pagination(page, size, pagedPosts, total, hasNext, hasPrevious, totalPages);
    }

    private record Pagination(int page, int size, Page<Post> pagedPosts, long total, boolean hasNext, boolean hasPrevious, int totalPages) {
    }
}
