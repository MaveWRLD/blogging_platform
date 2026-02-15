package org.amalitech.repositories;

import org.amalitech.entities.Comment;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

/**
 * Repository interface for Comment operations.
 * Abstracts the data access layer (MongoDB) from business logic.
 * Provides clear, unambiguous method names for different query types.
 */
public interface CommentRepository extends MongoRepository<Comment, String> {

    List<Comment> findByPostId(int postId);

    void deleteByPostId(int postId);
}