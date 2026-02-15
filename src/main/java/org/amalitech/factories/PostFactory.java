package org.amalitech.factories;

import org.amalitech.dtos.postDtos.CreatePostRequest;
import org.amalitech.entities.Post;
import org.amalitech.enums.PostStatus;
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
