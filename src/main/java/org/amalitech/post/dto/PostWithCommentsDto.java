package org.amalitech.post.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.amalitech.comment.dto.CommentDto;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class PostWithCommentsDto {
    private PostDto postDto;

    private List<CommentDto> comments;
    private long totalComments;
}


