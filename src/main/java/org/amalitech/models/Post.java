package org.amalitech.models;

import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@ToString
@Setter
@Getter
public class Post {

    private int id;
    private String title;
    private String body;
    private int userId;
    private String author;
    private List<String> tags;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime publishedAt;
    private String excerpt;
    private int viewCount = 0;
    private int likeCount = 0;
    private int commentCount = 0;

    private Double trendingScore;


    public Post(String title, String body, Integer userId, String status) {
        this.title = title;
        this.body = body;
        this.userId = userId;
        this.status = status != null ? status : "draft";
        this.tags = new ArrayList<>();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.viewCount = 0;
        this.likeCount = 0;
        this.commentCount = 0;
    }

    public Post(Integer id, String title, String body, Integer userId, String author,
                List<String> tags, String status, LocalDateTime createdAt, LocalDateTime updatedAt,
                String excerpt, int viewCount, int likeCount, int commentCount) {
        this.id = id;
        this.title = title;
        this.body = body;
        this.userId = userId;
        this.author = author;
        this.tags = tags != null ? new ArrayList<>(tags) : new ArrayList<>();
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.excerpt = excerpt;
        this.viewCount = viewCount;
        this.likeCount = likeCount;
        this.commentCount = commentCount;
    }
}