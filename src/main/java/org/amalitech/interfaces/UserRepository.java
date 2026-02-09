package org.amalitech.interfaces;

import org.amalitech.models.User;
import org.amalitech.util.exception.ResourceNotFoundException;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for User CRUD operations.
 * Abstracts the data access layer from business logic.
 */
public interface UserRepository {

    /**
     * Save a new user to the database.
     * @param user the user to save
     * @return the generated user ID
     */
    int save(User user);

    List<User> findAll();

    /**
     * Find a user by their ID.
     *
     * @param id the user ID
     * @return the User object
     * @throws ResourceNotFoundException if user doesn't exist
     */
    Optional<User> findByUserId(int id);

    /**
     * Find a user by their username.
     *
     * @param username the username
     * @return the User object
     * @throws ResourceNotFoundException if user doesn't exist
     */
    Optional<User> findByUsername(String username);

    /**
     * Update an existing user.
     * @param user the user with updated data
     */
    void update(User user);

    void delete(int id);
}


