package org.amalitech.strategy;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.amalitech.entities.User;
import org.amalitech.enums.TokenType;
import org.amalitech.token.Jwt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class RefreshTokenStrategy implements TokenStrategy {

    @Value("${spring.jwt.secret}")
    private String secretKey;

    @Override
    public Jwt generateToken(User user) {

        long REFRESH_EXPIRATION = 604800;
        var claims = Jwts.claims()
                .subject(user.getId().toString())
                .add("token_type", TokenType.REFRESH)
                .add("email", user.getEmail())
                .add("username", user.getUsername())
                .add("role", user.getRole())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 1000 * REFRESH_EXPIRATION))
                .build();

        return new Jwt(claims, Keys.hmacShaKeyFor(secretKey.getBytes()));
    }
}
