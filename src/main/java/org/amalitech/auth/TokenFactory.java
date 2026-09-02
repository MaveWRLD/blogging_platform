package org.amalitech.auth;

import org.amalitech.user.User;
import org.amalitech.auth.TokenType;
import org.amalitech.auth.AccessTokenStrategy;
import org.amalitech.auth.RefreshTokenStrategy;
import org.amalitech.auth.Jwt;
import org.springframework.stereotype.Service;

@Service
public class TokenFactory {

    private final AccessTokenStrategy accessTokenStrategy;
    private final RefreshTokenStrategy refreshTokenStrategy;

    public TokenFactory(AccessTokenStrategy a, RefreshTokenStrategy r) {
        this.accessTokenStrategy = a;
        this.refreshTokenStrategy = r;
    }

    public Jwt generateToken(TokenType type, User user) {
        return switch (type) {
            case ACCESS -> accessTokenStrategy.generateToken(user);
            case REFRESH -> refreshTokenStrategy.generateToken(user);
        };
    }
}
