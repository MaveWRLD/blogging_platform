package org.amalitech.token;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.amalitech.enums.TokenType;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.HashSet;
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
        try {
            return claims.get("roles", Set.class);
        } catch (Exception e) {
            var rolesList = claims.get("roles", java.util.List.class);
            if (rolesList != null) {
                return new HashSet<>(rolesList);
            }
            return java.util.Collections.emptySet();
        }
    }
}
