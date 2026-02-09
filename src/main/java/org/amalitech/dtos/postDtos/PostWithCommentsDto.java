package org.amalitech.dtos.postDtos;

import lombok.Builder;
import lombok.Data;
import org.amalitech.dtos.CommentDto;

import java.time.LocalDateTime;
import java.util.List;

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


