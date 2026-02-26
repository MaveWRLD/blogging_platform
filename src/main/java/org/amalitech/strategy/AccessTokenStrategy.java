package org.amalitech.strategy;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.amalitech.entities.Role;
import org.amalitech.entities.User;
import org.amalitech.enums.TokenType;
import org.amalitech.token.Jwt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.stream.Collectors;

@Service
public class AccessTokenStrategy implements TokenStrategy {

    @Value("${spring.jwt.secret}")
    private String secretKey;

    @Override
    public Jwt generateToken(User user) {

        var userRoles = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        long ACCESS_EXPIRATION = 3600;
        var claims = Jwts.claims()
                .subject(user.getId().toString())
                .add("token_type", TokenType.ACCESS)
                .add("email", user.getEmail())
                .add("username", user.getUsername())
                .add("roles", userRoles)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 1000 * ACCESS_EXPIRATION))
                .build();

        return new Jwt(claims, Keys.hmacShaKeyFor(secretKey.getBytes()));
    }
}
