package org.amalitech.entities;

import jakarta.persistence.Id;
import lombok.*;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@AllArgsConstructor
@NoArgsConstructor
@ToString
@Getter
@Setter
@Document("comments")
public class Comment {

    @Id
    private String id;

    private int postId;

    private String username;
    private Integer parentCommentId;
    private String body;
    private Instant createdAt;
}
