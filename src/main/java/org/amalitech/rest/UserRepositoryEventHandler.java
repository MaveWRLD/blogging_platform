package org.amalitech.rest;

import org.amalitech.entities.User;
import org.amalitech.exception.ValidationException;
import org.amalitech.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.core.annotation.HandleBeforeCreate;
import org.springframework.data.rest.core.annotation.HandleBeforeSave;
import org.springframework.data.rest.core.annotation.RepositoryEventHandler;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Event handler for UserRepository to handle validation and business logic
 * before Spring Data REST operations.
 */
@Component
@RepositoryEventHandler
public class UserRepositoryEventHandler {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserRepositoryEventHandler(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Handles validation before creating a new user via Spring Data REST.
     */
    @HandleBeforeCreate
    public void handleUserCreate(User user) {
        validateUserUniqueness(user);
        encodePassword(user);
    }

    /**
     * Handles validation before updating a user via Spring Data REST.
     */
    @HandleBeforeSave
    public void handleUserUpdate(User user) {
        // For updates, we need to check if email/username is being changed
        User existingUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new ValidationException("User not found with id: " + user.getId()));

        // Check username uniqueness if it's being changed
        if (!existingUser.getUsername().equals(user.getUsername())) {
            if (userRepository.existsByUsername(user.getUsername())) {
                throw new ValidationException("Username already exists");
            }
        }

        // Check email uniqueness if it's being changed
        if (user.getEmail() != null && !user.getEmail().equals(existingUser.getEmail())) {
            if (userRepository.existsByEmail(user.getEmail())) {
                throw new ValidationException("Email already exists");
            }
        }

        // Encode password if it's being updated
        if (user.getPassword() != null && !user.getPassword().isBlank() && 
            !user.getPassword().equals(existingUser.getPassword())) {
            encodePassword(user);
        }
    }

    /**
     * Validates that username and email are unique.
     */
    private void validateUserUniqueness(User user) {
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new ValidationException("Username already exists");
        }
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new ValidationException("Email already exists");
        }
    }

    /**
     * Encodes the user's password if it's not already encoded.
     */
    private void encodePassword(User user) {
        if (user.getPassword() != null && !user.getPassword().isBlank()) {
            // Check if password is already encoded (BCrypt hashes start with $2a$, $2b$, etc.)
            if (!user.getPassword().startsWith("$2")) {
                user.setPassword(passwordEncoder.encode(user.getPassword()));
            }
        }
    }
}
