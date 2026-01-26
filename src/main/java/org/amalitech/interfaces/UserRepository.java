package org.amalitech.interfaces;

import org.amalitech.models.User;
import org.amalitech.util.exception.NotFoundException;

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

    /**
     * Find a user by their ID.
     * @param id the user ID
     * @return the User object
     * @throws NotFoundException if user doesn't exist
     */
    User findByUserId(int id);

    /**
     * Find a user by their username.
     * @param username the username
     * @return the User object
     * @throws NotFoundException if user doesn't exist
     */
    User findByUsername(String username);

    /**
     * Update an existing user.
     * @param user the user with updated data
     */
    void update(User user);
}


