package org.amalitech.repositories;

import org.amalitech.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repository interface for User CRUD operations.
 * Abstracts the data access layer from business logic.
 */
/**
 * Repository interface for User CRUD operations.
 * Abstracts the data access layer from business logic.
 */
public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findById(Long id);
    Optional<User> findByUsername(String email);

    void deleteById(Long id);

    boolean existsByEmail(String email);
    boolean existsByUsername(String username);
}