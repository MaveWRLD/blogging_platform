package org.amalitech.comment;

import org.amalitech.comment.Comment;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for Comment operations.
 * Abstracts the data access layer (MongoDB) from business logic.
 * Provides clear, unambiguous method names for different query types.
 */
@Repository
@RestResource(exported = false)
public interface CommentRepository extends MongoRepository<Comment, String> {

    List<Comment> findByPostId(int postId);

    void deleteByPostId(int postId);

    boolean existsByIdAndUsername(String id, String username);
}