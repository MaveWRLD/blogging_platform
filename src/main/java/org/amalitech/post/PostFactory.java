package org.amalitech.post;

import org.amalitech.post.dto.CreatePostRequest;
import org.amalitech.post.Post;
import org.amalitech.post.PostStatus;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class PostFactory {

    public static Post fromRequest(CreatePostRequest request) {
        if (request.getStatus() != null &&
                request.getStatus().equalsIgnoreCase("PUBLISHED")) {
            return createPublished(request);
        }
        return createDraft(request);
    }

    private static Post createDraft(CreatePostRequest request) {
        return new Post(request.getTitle(), request.getBody(), PostStatus.draft, Instant.now(), null);
    }

    private static Post createPublished(CreatePostRequest request) {
        return new Post(request.getTitle(), request.getBody(), PostStatus.published, Instant.now(), Instant.now());
    }
}
