package org.amalitech.graphqlResolver;

import org.amalitech.dtos.CreateCommentRequest;
import org.amalitech.dtos.postDtos.*;
import org.amalitech.entities.Comment;
import org.amalitech.entities.Post;
import org.amalitech.enums.PostStatus;
import org.amalitech.service.CommentService;
import org.amalitech.service.PostService;
import org.amalitech.exception.ResourceNotFoundException;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.stereotype.Controller;

import java.time.Instant;

@Controller
public class PostMutationResolver {

    private final PostService postService;
    private final CommentService commentService;

    public PostMutationResolver(
            PostService postService,
            CommentService commentService
    ) {
        this.postService = postService;
        this.commentService = commentService;
    }

    @MutationMapping
    public Post createPost(@Argument CreatePostRequest input) {

        Post post = new Post();
        post.setTitle(input.getTitle());
        post.setBody(input.getBody());
        post.setStatus(PostStatus.valueOf(input.getStatus() != null ? input.getStatus() : "draft"));

        postService.createPost(post, null);

        return post;
    }

    @MutationMapping
    public Post updatePost(@Argument int id, @Argument UpdatePostRequest input) {

        Post existing = postService.findPostById(id).keySet().stream().findFirst().orElseThrow(
                () -> new ResourceNotFoundException("Post not found")
        );

        if (input.getTitle() != null) {
            existing.setTitle(input.getTitle());
        }
        if (input.getBody() != null) {
            existing.setBody(input.getBody());
        }
        if (input.getStatus() != null) {
            existing.setStatus(PostStatus.valueOf(input.getStatus()));
            if (existing.getPublishedAt() == null && "published".equals(existing.getStatus())) {
                existing.setPublishedAt(Instant.now());
            }
        }

        postService.updatePost(existing, null);

        return existing;
    }

    @MutationMapping
    public Boolean deletePost(@Argument String id) {
        int postId = Integer.parseInt(id);
        postService.deletePost(postId);
        return true;
    }

    @MutationMapping
    public Post publishPost(@Argument String id) {
        int postId = Integer.parseInt(id);

        Post post = postService.findPostById(postId).keySet().stream().findFirst().orElseThrow(
                () -> new ResourceNotFoundException("Post not found")
        );

        if (PostStatus.published != post.getStatus()) {
            post.setStatus(PostStatus.valueOf("published"));
            post.setPublishedAt(Instant.now());
            postService.updatePost(post, null);
        }

        return post;
    }

    @MutationMapping
    public Post likePost(@Argument String id) {
        int postId = Integer.parseInt(id);

        Post post = postService.findPostById(postId).keySet().stream().findFirst().orElseThrow(
                () -> new ResourceNotFoundException("Post not found")
        );

        post.setLikeCount(post.getLikeCount() + 1);
        postService.updatePost(post, null);

        return post;
    }

    @MutationMapping
    public Comment createComment(@Argument CreateCommentRequest input) {

        Comment comment = new Comment();
        comment.setPostId(input.getPostId());
        comment.setBody(input.getBody());
        comment.setUsername("current_user");

        commentService.save(comment);

        return comment;
    }
}