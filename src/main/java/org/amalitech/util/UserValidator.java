package org.amalitech.util;

import org.amalitech.util.exception.ValidationException;
import org.amalitech.models.User;

public class UserValidator {

    public void validate(User user) {
        if (user.getUsername() == null || user.getUsername().isEmpty()) {
            throw new ValidationException("Username cannot be empty");
        }
        if (user.getEmail() == null || user.getEmail().isEmpty()) {
            throw new ValidationException("Email cannot be empty");
        }
    }

    public void validateCredentials(String username, String password) {
        if (username == null || username.isEmpty()) {
            throw new ValidationException("Username cannot be empty");
        }
        if (password == null || password.isEmpty()) {
            throw new ValidationException("Password cannot be empty");
        }
    }
}
