package org.amalitech.models;

import java.time.LocalDateTime;

public class Comment {

    private String id;
    private int postId;
    private String userName;
    private Integer parentCommentId;
    private String body;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Comment() {}

    public Comment(String id, int postId, String userName, String body) {
        this.id = id;

        this.postId = postId;
        this.userName = userName;
        this.body = body;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public int getPostId() { return postId; }
    public void setPostId(int postId) { this.postId = postId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public Integer getParentCommentId() {
        return parentCommentId;
    }
    public void setParentCommentId(Integer parentCommentId) {
        this.parentCommentId = parentCommentId;
    }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }


    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        String bodyPreview = (body == null) ? "" : (body.length() > 80 ? body.substring(0, 77) + "..." : body);
        String time = (createdAt == null) ? "" : createdAt.toString();
        return (time.isEmpty() ? "" : (time + " - ")) + bodyPreview;
    }
}
