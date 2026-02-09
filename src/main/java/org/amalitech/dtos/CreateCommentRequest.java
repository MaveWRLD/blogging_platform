package org.amalitech.dtos;

import lombok.Data;

@Data
public class CreateCommentRequest {
    private int postId;
    private String username;
    private String body;
    private String parentCommentId;
}
