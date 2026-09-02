package org.amalitech.user;

import org.amalitech.exception.ValidationException;

public class UserValidator {

    public static void validateCredentials(String username, String password) throws ValidationException{
        if (username == null || username.isEmpty()) {
            throw new ValidationException("Username cannot be empty");
        }
        if (password == null || password.isEmpty()) {
            throw new ValidationException("Password cannot be empty");
        }
    }
}
