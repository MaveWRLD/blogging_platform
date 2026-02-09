package org.amalitech.dtos.postDtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.amalitech.dtos.CommentDto;
import org.amalitech.dtos.userDtos.UserDto;

import java.util.List;

@Data
@AllArgsConstructor
public class PostResponse {
    private PostDto post;
    private UserDto author;
    private List<CommentDto> comments;
}
