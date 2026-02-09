package org.amalitech.dtos;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;


@Data
@Builder
public class CommentDto {
    private String id;
    private String username;
    private String body;
    private LocalDateTime createdAt;
}
