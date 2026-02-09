package org.amalitech.models;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class Comment {

    private String id;
    private int postId;
    private String username;
    private Integer parentCommentId;
    private String body;
    private LocalDateTime createdAt;

    public Comment() {}

    public Comment(String id, int postId, String userName, String body, LocalDateTime createdAt) {
        this.id = id;
        this.postId = postId;
        this.username = userName;
        this.body = body;
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        String bodyPreview = (body == null) ? "" : (body.length() > 80 ? body.substring(0, 77) + "..." : body);
        String time = (createdAt == null) ? "" : createdAt.toString();
        return (time.isEmpty() ? "" : (time + " - ")) + bodyPreview;
    }
}
