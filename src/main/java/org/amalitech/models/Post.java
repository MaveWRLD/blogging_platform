package org.amalitech.models;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
public class Post {

    private int id;
    private String title;
    private String body;
    private int userId;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Post() {}

    public Post(String title, String body, int userId, String status) {
        this.title = title;
        this.body = body;
        this.userId = userId;
        this.status = status;
    }

    public Post(int id, String title, String body, int userId,
                String status) {
        this.id = id;
        this.title = title;
        this.body = body;
        this.userId = userId;
        this.status = status;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
}

