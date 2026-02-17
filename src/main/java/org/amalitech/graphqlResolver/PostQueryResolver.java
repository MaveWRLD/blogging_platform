package org.amalitech.graphqlResolver;

import org.amalitech.dtos.CommentDto;
import org.amalitech.dtos.postDtos.PagedPostsResponse;
import org.amalitech.dtos.postDtos.PostDto;
import org.amalitech.dtos.postDtos.PostFilter;
import org.amalitech.dtos.postDtos.PostResponse;
import org.amalitech.exception.ValidationException;
import org.amalitech.mappers.CommentMapper;
import org.amalitech.mappers.PostMapper;
import org.amalitech.mappers.UserMapper;
import org.amalitech.entities.Comment;
import org.amalitech.entities.Post;
import org.amalitech.service.PostService;
import org.amalitech.service.TagService;
import org.amalitech.service.UserService;
import org.amalitech.exception.ResourceNotFoundException;
import org.apache.coyote.BadRequestException;
import org.springframework.data.domain.Page;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Controller
public class PostQueryResolver {

    private final PostService postService;
    private final UserService userService;
    private final PostMapper postMapper;
    private final CommentMapper commentMapper;
    private final UserMapper userMapper;

    public PostQueryResolver(PostService postService, PostMapper postMapper, UserService userService, CommentMapper commentMapper, UserMapper userMapper) {
        this.postService = postService;
        this.postMapper = postMapper;
        this.userService = userService;

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
        Map<Post, List<Comment>> postMap = postService.findPostById(id);

        if (postMap == null || postMap.isEmpty()) {
            throw new ResourceNotFoundException("Post not found with id: " + id);
        }

        Post post = postMap.keySet().stream().findFirst().orElseThrow(() -> new ResourceNotFoundException("Post not found with id: " + id));
        List<Comment> comments = postMap.getOrDefault(post, List.of());

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
