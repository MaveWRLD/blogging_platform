package org.amalitech.dtos.postDtos;

import lombok.Data;

import java.time.Instant;


@Data
public class PostDto {
    private int id;
    private String title;
    private String body;
    private String excerpt;
    private String status;
    private Instant createdAt;
    private Instant publishedAt;
    private Instant updatedAt;
    private int viewCount;
    private int likeCount;
    private int commentCount;
    private int userId;
    private String author;
}