package org.amalitech.factories;

import org.amalitech.user.User;
import org.amalitech.enums.TokenType;
import org.amalitech.strategy.AccessTokenStrategy;
import org.amalitech.strategy.RefreshTokenStrategy;
import org.amalitech.token.Jwt;
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
