package org.amalitech.dtos.postDtos;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class PostDto {
    private int id;
    private String title;
    private String body;
    private int userId;
    private String author;
    @JsonIgnore
    private List<String> tags;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String status;
    private String excerpt;
    private LocalDateTime publishedAt;
    private int viewCount;
    private int likeCount;
    private int commentCount;
}