package org.amalitech.strategy;

import org.amalitech.entities.User;
import org.amalitech.token.Jwt;

public interface TokenStrategy {
    Jwt generateToken(User user);
}
