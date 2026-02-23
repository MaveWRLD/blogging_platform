package org.amalitech.controllers;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.amalitech.dtos.JwtResponse;
import org.amalitech.dtos.LoginRequest;
import org.amalitech.entities.User;
import org.amalitech.enums.TokenType;
import org.amalitech.factories.TokenFactory;
import org.amalitech.token.Jwt;
import org.amalitech.token.JwtParser;
import org.amalitech.service.UserService;
import org.amalitech.token.TokenBlacklistService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Operations for user authentication, token management, and session handling")
public class AuthController {

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

    @PostMapping("/login")
    @Operation(
            summary = "User login",
            description = "Authenticates a user with username and password, returns JWT access token and sets refresh token cookie"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Login successful - JWT access token returned",
                    content = @Content(schema = @Schema(implementation = JwtResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Bad request - Invalid credentials or missing required fields",
                    content = @Content(schema = @Schema(example = "{\"timestamp\":\"2024-01-01T12:00:00\",\"message\":\"Authentication failed\",\"path\":\"/api/auth/login\"}"))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Invalid username or password",
                    content = @Content(schema = @Schema(example = "{\"timestamp\":\"2024-01-01T12:00:00\",\"message\":\"Bad credentials\",\"path\":\"/api/auth/login\"}"))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "422",
                    description = "Unprocessable Entity - Validation errors in request body",
                    content = @Content(schema = @Schema(example = "{\"timestamp\":\"2024-01-01T12:00:00\",\"errors\":{\"username\":\"Username is required\",\"password\":\"Password must not be blank\"},\"message\":\"Validation failed\"}"))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "Internal server error - Database or system failure",
                    content = @Content(schema = @Schema(example = "{\"timestamp\":\"2024-01-01T12:00:00\",\"message\":\"Database connection failed\",\"path\":\"/api/auth/login\"}"))
            )
    })
    public ResponseEntity<JwtResponse> login(
            @Parameter(description = "Login credentials", required = true)
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

    @PostMapping("/refresh")
    @Operation(
            summary = "Refresh access token",
            description = "Generates a new access token using a valid refresh token from cookie"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Token refreshed successfully - New JWT access token returned",
                    content = @Content(schema = @Schema(implementation = JwtResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Bad request - Missing or invalid refresh token cookie",
                    content = @Content(schema = @Schema(example = "{\"timestamp\":\"2024-01-01T12:00:00\",\"message\":\"Missing refreshToken cookie\",\"path\":\"/api/auth/refresh\"}"))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Refresh token expired, invalid, or access token provided instead of refresh token",
                    content = @Content(schema = @Schema(example = "{\"timestamp\":\"2024-01-01T12:00:00\",\"message\":\"Token expired\",\"path\":\"/api/auth/refresh\"}"))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "User not found - User associated with refresh token no longer exists",
                    content = @Content(schema = @Schema(example = "{\"timestamp\":\"2024-01-01T12:00:00\",\"message\":\"User not found\",\"path\":\"/api/auth/refresh\"}"))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "Internal server error - Token generation or database failure",
                    content = @Content(schema = @Schema(example = "{\"timestamp\":\"2024-01-01T12:00:00\",\"message\":\"Token generation failed\",\"path\":\"/api/auth/refresh\"}"))
            )
    })
    public ResponseEntity<JwtResponse> refreshToken(
            @Parameter(description = "Refresh token from HTTP-only cookie", required = true)
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

    @PostMapping("/logout")
    @Operation(
            summary = "User logout",
            description = "Invalidates access token and refresh token, clears refresh token cookie"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "204",
                    description = "Logout successful - Tokens invalidated and cookie cleared"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Bad request - Invalid token format or malformed request",
                    content = @Content(schema = @Schema(example = "{\"timestamp\":\"2024-01-01T12:00:00\",\"message\":\"Invalid token format\",\"path\":\"/api/auth/logout\"}"))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Invalid or expired tokens",
                    content = @Content(schema = @Schema(example = "{\"timestamp\":\"2024-01-01T12:00:00\",\"message\":\"Token expired\",\"path\":\"/api/auth/logout\"}"))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "Internal server error - Token blacklisting or database failure",
                    content = @Content(schema = @Schema(example = "{\"timestamp\":\"2024-01-01T12:00:00\",\"message\":\"Failed to blacklist token\",\"path\":\"/api/auth/logout\"}"))
            )
    })
    public ResponseEntity<Void> logout(
            @Parameter(description = "Authorization header with Bearer token (optional)")
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @Parameter(description = "Refresh token from HTTP-only cookie (optional)")
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
