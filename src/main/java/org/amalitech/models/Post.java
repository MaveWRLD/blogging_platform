package org.amalitech.models;

import java.time.LocalDateTime;

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

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}

