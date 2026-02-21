package org.amalitech.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.amalitech.entities.Role;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Set;

public class Jwt {

    private final Claims claims;
    private final SecretKey securityKey;


    public Jwt(Claims claims, SecretKey securityKey) {
        this.claims = claims;
        this.securityKey = securityKey;
    }

    public boolean isExpired() {
        return claims.getExpiration().before(new Date());
    }

    public Long getUserId() {
        return Long.valueOf(claims.getSubject());
    }

    public Set<Role> getRoles() {
        return claims.get("roles", Set.class);
    }

    public String toString() {
        return Jwts.builder().claims(claims).signWith(securityKey).compact();
    }


}
