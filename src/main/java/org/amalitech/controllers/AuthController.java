package org.amalitech.controllers;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.amalitech.api.doc.AuthApi;
import org.amalitech.dtos.JwtResponse;
import org.amalitech.dtos.LoginRequest;
import org.amalitech.user.User;
import org.amalitech.enums.TokenType;
import org.amalitech.factories.TokenFactory;
import org.amalitech.token.Jwt;
import org.amalitech.token.JwtParser;
import org.amalitech.user.UserService;
import org.amalitech.token.TokenBlacklistService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController implements AuthApi {

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final TokenFactory tokenFactory;
    private final JwtParser jwtParser;
    private final TokenBlacklistService blacklistService;

    public AuthController(AuthenticationManager authenticationManager, UserService userService, TokenFactory tokenFactory, JwtParser jwtParser, TokenBlacklistService blacklistService) {
        this.authenticationManager = authenticationManager;
        this.userService = userService;
        this.tokenFactory = tokenFactory;
        this.jwtParser = jwtParser;
        this.blacklistService = blacklistService;
    }

    @Override
    @PostMapping("/login")
    public ResponseEntity<JwtResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response
    ) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        User user = userService.findByUsername(request.getUsername()).orElseThrow();

        Jwt accessToken = tokenFactory.generateToken(TokenType.ACCESS, user);

        Jwt refreshToken = tokenFactory.generateToken(TokenType.REFRESH, user);

        var cookie = new Cookie("refreshToken", refreshToken.toString());
        cookie.setHttpOnly(true);
        cookie.setPath("/api/auth/refresh");
        cookie.setMaxAge(7 * 24 * 60 * 60);
        cookie.setSecure(true);
        response.addCookie(cookie);

        return ResponseEntity.ok(new JwtResponse(accessToken.toString()));
    }

    @Override
    @PostMapping("/refresh")
    public ResponseEntity<JwtResponse> refreshToken(
            @CookieValue("refreshToken") String refreshToken) {
        var jwt = jwtParser.parseToken(refreshToken);
        if (jwt == null || jwt.isExpired()) {
            return ResponseEntity.status(401).build();
        }

        if (TokenType.ACCESS.equals(jwt.getTokenType())) {
            return ResponseEntity.status(401).build();
        }

        Long userId = jwt.getUserId();
        User user = userService.findByUserId(userId);
        Jwt accessToken = tokenFactory.generateToken(TokenType.ACCESS, user);

        return ResponseEntity.ok(new JwtResponse(accessToken.toString()));
    }

    @Override
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @CookieValue(value = "refreshToken", required = false) String refreshToken,
            HttpServletResponse response
    ) {

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String accessToken = authHeader.substring(7);
            Jwt jwt = jwtParser.parseToken(accessToken);

            if (jwt != null && !jwt.isExpired()) {
                blacklistService.blacklist(accessToken, jwt.getExpiresAt());
            }
        }

        if (refreshToken != null) {
            Jwt jwt = jwtParser.parseToken(refreshToken);

            if (jwt != null && !jwt.isExpired()) {
                blacklistService.blacklist(refreshToken, jwt.getExpiresAt());
            }

            Cookie cookie = new Cookie("refreshToken", null);
            cookie.setHttpOnly(true);
            cookie.setSecure(true);
            cookie.setPath("/api/auth/refresh");
            cookie.setMaxAge(0);
            response.addCookie(cookie);
        }

        return ResponseEntity.noContent().build();
    }
}
