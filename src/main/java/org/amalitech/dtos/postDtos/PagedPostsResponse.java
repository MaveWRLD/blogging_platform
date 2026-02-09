package org.amalitech.dtos.postDtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.amalitech.models.Post;

import java.util.List;

@Data
@AllArgsConstructor
public class PagedPostsResponse {
    private List<PostDto> posts;
    private int page;
    private int size;
    private long total;
    private int totalPages;
    private boolean hasPrevious;
    private boolean hasNext;
}
