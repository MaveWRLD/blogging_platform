package org.amalitech.repositories;

import org.amalitech.dtos.postDtos.PostDto;
import org.amalitech.entities.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Post CRUD operations.
 * Abstracts the data access layer from business logic.
 */
@Repository
@RepositoryRestResource(exported = false)
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


    Page<Post> findByUserId(@Param("userId") Long userId, Pageable pageable);

    @Modifying
    @Query("UPDATE Post p SET p.viewCount = p.viewCount + :count WHERE p.id = :postId")
    void incrementViewCount(@Param("postId") int postId, @Param("count") int count);

    @Modifying
    @Query("UPDATE Post p SET p.likeCount = p.likeCount + :count WHERE p.id = :postId")
    void incrementLikeCount(@Param("postId") int postId, @Param("count") int count);

    @Query("SELECT p FROM Post p WHERE p.status = 'PUBLISHED' ORDER BY p.createdAt DESC")
    List<Post> findRecentPublishedPostsForTrending();

    boolean existsByIdAndUser_Id(int id, Long user_id);
}

