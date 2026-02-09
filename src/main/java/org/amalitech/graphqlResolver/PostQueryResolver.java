package org.amalitech.graphqlResolver;

import org.amalitech.dtos.CommentDto;
import org.amalitech.dtos.postDtos.PagedPostsResponse;
import org.amalitech.dtos.postDtos.PostDto;
import org.amalitech.dtos.postDtos.PostFilter;
import org.amalitech.dtos.postDtos.PostResponse;
import org.amalitech.mappers.CommentMapper;
import org.amalitech.mappers.PostMapper;
import org.amalitech.mappers.UserMapper;
import org.amalitech.models.Comment;
import org.amalitech.models.Post;
import org.amalitech.models.Tag;
import org.amalitech.service.PostService;
import org.amalitech.service.TagService;
import org.amalitech.service.UserService;
import org.amalitech.util.exception.ResourceNotFoundException;
import org.apache.coyote.BadRequestException;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Controller
public class PostQueryResolver {

    private final TagService tagService;
    private final PostService postService;
    private final UserService userService;
    private final PostMapper postMapper;
    private final CommentMapper commentMapper;
    private final UserMapper userMapper;


    public PostQueryResolver(PostService postService, PostMapper postMapper, UserService userService, TagService tagService, CommentMapper commentMapper, UserMapper userMapper) {
        this.postService = postService;
        this.postMapper = postMapper;
        this.userService = userService;
        this.tagService = tagService;
        this.commentMapper = commentMapper;
        this.userMapper = userMapper;
    }

    @QueryMapping
    public PagedPostsResponse posts(@Argument PostFilter filter) throws BadRequestException {

        int page = (filter != null && filter.getPage() >= 0) ? filter.getPage() : 0;
        int size = (filter != null && filter.getSize() >= 1 && filter.getSize() <= 100)
                ? filter.getSize()
                : 12;

        if (filter != null && (filter.getPage() < 0 || filter.getSize() < 1 || filter.getSize() > 100)) {
            throw new BadRequestException("Invalid pagination parameters");
        }

        List<PostDto> posts = postService.findPosts(filter).stream().map(postMapper::toDto).collect(Collectors.toList());

        for (PostDto post : posts) {
            System.out.println(post.getCommentCount());
        }
        ;
        long total = postService.postCount(filter);
        if (total < 0) total = 0;

        boolean hasNext = (long) (page + 1) * size < total;
        boolean hasPrevious = page > 0;

        int totalPages = (int) Math.ceil((double) total / size);

        return new PagedPostsResponse(
                posts,
                page,
                size,
                total,
                totalPages,
                hasPrevious,
                hasNext
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

        var user = userService.findByUserId(post.getUserId());
        var userDto = userMapper.toUserDto(user);

        return new PostResponse(postDto, userDto, commentDtos);
    }

    @SchemaMapping(typeName = "Post", field = "tags")
    public List<Tag> tags(PostDto postDto) {
        return tagService.findTagsByPostId(postDto.getId());
    }
}
