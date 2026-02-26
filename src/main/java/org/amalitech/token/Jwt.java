package org.amalitech.token;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.amalitech.enums.TokenType;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.Set;

public class Jwt {

    private final Claims claims;
    private final SecretKey securityKey;


    public Jwt(Claims claims, SecretKey securityKey) {
        this.claims = claims;
        this.securityKey = securityKey;
    }

    public TokenType getTokenType() {
        String type = claims.get("token_type", String.class);
        return TokenType.from(type);
    }

    public boolean isExpired() {
        return claims.getExpiration().before(new Date());
    }

    public Long getUserId() {
        return Long.valueOf(claims.getSubject());
    }

    public String toString() {
        return Jwts
                .builder()
                .claims(claims)
                .signWith(securityKey)
                .compact();
    }

    public Instant getExpiresAt() {
        return Instant.ofEpochMilli(claims.getExpiration().getTime());
    }

    public Set<String> getRoles() {
        return claims.get("roles", Set.class);
    }

    public String getRole() {
        Set<String> roles = getRoles();
        return roles != null && !roles.isEmpty() ? roles.iterator().next() : null;
    }
}
