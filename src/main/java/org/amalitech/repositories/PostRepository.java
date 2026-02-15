package org.amalitech.repositories;

import org.amalitech.entities.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

/**
 * Repository interface for Post CRUD operations.
 * Abstracts the data access layer from business logic.
 */
public interface PostRepository extends JpaRepository<Post, Integer>, JpaSpecificationExecutor<Post> {

    @Query("SELECT p FROM Post p LEFT JOIN FETCH p.user WHERE p.user.id = :userId")
    Page<Post> findPostsByUserId(Long userId,
                                 Pageable pageable);

    @Query("SELECT p FROM Post p JOIN fetch p.user " +
            "WHERE p.status = 'published' AND p.createdAt >= :threshold " +
            "ORDER BY p.createdAt DESC")
    List<Post> findRecentPublishedPosts(@Param("threshold") Instant threshold);
}

