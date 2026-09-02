package org.amalitech.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtParser {
    @Value("${spring.jwt.secret}")
    private String secretKey;

    public Jwt parseToken(String token) {
        try {
            var claims = getClaims(token);
            return new Jwt(claims, Keys.hmacShaKeyFor(secretKey.getBytes()));
        } catch (JwtException e) {
            return null;
        }
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(secretKey.getBytes()))
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
