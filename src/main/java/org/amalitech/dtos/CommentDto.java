package org.amalitech.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class CommentDto {
    private String id;
    private String username;
    private String body;
    private Instant createdAt;
}
