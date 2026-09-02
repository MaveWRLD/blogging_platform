package org.amalitech.comment.dto;

import lombok.Data;

@Data
public class CreateCommentRequest {
    private String body;
    private String parentCommentId;
}
