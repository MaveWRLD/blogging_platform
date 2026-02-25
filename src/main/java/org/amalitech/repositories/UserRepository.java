package org.amalitech.repositories;

import org.amalitech.entities.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.Optional;

/**
 * Repository interface for User CRUD operations.
 * Abstracts the data access layer from business logic.
 * Spring Data REST will automatically expose REST endpoints for this repository.
 */
@RepositoryRestResource(collectionResourceRel = "users", path = "users")
public interface UserRepository extends JpaRepository<User, Integer> {
    
    @PreAuthorize("hasRole('admin') or @authorizationService.canAccessUser(#id)")
    Optional<User> findById(Long id);

    Optional<User> findByEmail(String email);
    
    @PreAuthorize("permitAll()")
    Optional<User> findByUsername(String username);
    
    @PreAuthorize("hasRole('admin')")
    Page<User> findAll(Pageable pageable);
    
    @PreAuthorize("hasRole('admin') or @authorizationService.canAccessUser(#id)")
    void deleteById(Long id);

    @PreAuthorize("permitAll()")
    boolean existsByEmail(String email);
    
    @PreAuthorize("permitAll()")
    boolean existsByUsername(String username);
    
    @Query("SELECT u FROM User u WHERE u.username = :username")
    @PreAuthorize("permitAll()")
    Optional<User> findByUsernameForSearch(@Param("username") String username);
}