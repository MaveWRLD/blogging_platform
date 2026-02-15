package org.amalitech.dtos.postDtos;

import org.amalitech.enums.PostStatus;

import java.time.Instant;

public record PostDto(
        Integer id,
        String title,
        String body,
        Instant createdAt,
        Instant updatedAt,
        String excerpt,
        Instant publishedAt,
        Integer likeCount,
        Integer viewCount,
        Integer commentCount,
        PostStatus status,
        Long userId,
        String author
) {}
