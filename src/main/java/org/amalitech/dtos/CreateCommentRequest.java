package org.amalitech.dtos;

import lombok.Data;

@Data
public class CreateCommentRequest {
    private String body;
    private String parentCommentId;
}
