package org.amalitech.strategy;

import org.amalitech.user.User;
import org.amalitech.token.Jwt;

public interface TokenStrategy {
    Jwt generateToken(User user);
}
