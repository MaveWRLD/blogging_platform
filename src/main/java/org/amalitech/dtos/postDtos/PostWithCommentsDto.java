package org.amalitech.dtos.postDtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.amalitech.dtos.CommentDto;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class PostWithCommentsDto {
    private PostDto postDto;

    private List<CommentDto> comments;
    private long totalComments;
    private int page;
    private int size;
    private int totalPages;
}


