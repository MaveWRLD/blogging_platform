package org.amalitech.post.graphqlResolver;

import org.amalitech.comment.dto.CreateCommentRequest;
import org.amalitech.post.dto.*;
import org.amalitech.comment.Comment;
import org.amalitech.post.Post;
import org.amalitech.post.PostStatus;
import org.amalitech.comment.CommentService;
import org.amalitech.post.PostService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.stereotype.Controller;


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
        return postService.updatePost(id, input);
    }


    @MutationMapping
    public Boolean deletePost(@Argument String id) {
        int postId = Integer.parseInt(id);
        postService.deletePost(postId);
        return true;
    }

   @MutationMapping
   public void likePost(@Argument String id) {
       int postId = Integer.parseInt(id);

       postService.incrementLikeCount(postId);
   }

    @MutationMapping
    public Comment createComment(@Argument Long postId, @Argument CreateCommentRequest input) {

        Comment comment = new Comment();
        comment.setPostId(postId);
        comment.setBody(input.getBody());
        comment.setUsername("current_user");

        commentService.save(postId, comment);

        return comment;
    }
}