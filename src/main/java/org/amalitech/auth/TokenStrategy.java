package org.amalitech.auth;

import org.amalitech.user.User;
import org.amalitech.auth.Jwt;

public interface TokenStrategy {
    Jwt generateToken(User user);
}
