package org.amalitech.post.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.amalitech.comment.dto.CommentDto;
import org.amalitech.user.dto.UserDto;

import java.util.List;

@Data
@AllArgsConstructor
public class PostResponse {
    private PostDto post;
    private UserDto author;
    private List<CommentDto> comments;
}
