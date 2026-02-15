package org.amalitech.repositories;

import org.amalitech.dtos.postDtos.PostDto;
import org.amalitech.entities.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Post CRUD operations.
 * Abstracts the data access layer from business logic.
 */
@Repository
public interface PostRepository extends JpaRepository<Post, Integer>, JpaSpecificationExecutor<Post> {

    @Query("""
    SELECT new org.amalitech.dtos.postDtos.PostDto(
        p.id, p.title, p.body, p.createdAt, p.updatedAt,
        p.excerpt, p.publishedAt, p.likeCount, p.viewCount,
        p.commentCount, p.status, u.id, u.username
    )
    FROM Post p
    LEFT JOIN p.user u
    """)
    Page<PostDto> findAllProjected(Pageable pageable);

    @EntityGraph(attributePaths = {"user"})
    Optional<Post> findById(int id);

    @Query("""
    SELECT new org.amalitech.dtos.postDtos.PostDto(
        p.id, p.title, p.body, p.createdAt, p.updatedAt,
        p.excerpt, p.publishedAt, p.likeCount, p.viewCount,
        p.commentCount, p.status, u.id, u.username
    )
    FROM Post p
    LEFT JOIN p.user u
    WHERE u.id = :userId
    """)
    Page<PostDto> findPostsByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query("""
        select p
        from Post p
        where p.status = org.amalitech.enums.PostStatus.published
          and p.createdAt >= :threshold
        order by p.createdAt desc
    """)
    Page<Post> findRecentPublishedPosts(@Param("threshold") Instant threshold, Pageable pageable
    );
}

