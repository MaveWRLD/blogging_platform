package org.amalitech.strategy;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.amalitech.user.Role;
import org.amalitech.user.User;
import org.amalitech.enums.TokenType;
import org.amalitech.token.Jwt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.stream.Collectors;

@Service
public class RefreshTokenStrategy implements TokenStrategy {

    @Value("${spring.jwt.secret}")
    private String secretKey;

    @Override
    public Jwt generateToken(User user) {

        var userRoles = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        long REFRESH_EXPIRATION = 604800;
        var claims = Jwts.claims()
                .subject(user.getId().toString())
                .add("token_type", TokenType.REFRESH)
                .add("email", user.getEmail())
                .add("username", user.getUsername())
                .add("roles", userRoles)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 1000 * REFRESH_EXPIRATION))
                .build();

        return new Jwt(claims, Keys.hmacShaKeyFor(secretKey.getBytes()));
    }
}
