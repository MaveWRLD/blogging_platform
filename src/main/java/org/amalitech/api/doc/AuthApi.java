package org.amalitech.api.doc;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.amalitech.dtos.JwtResponse;
import org.amalitech.dtos.LoginRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Operations for user authentication, token management, and session handling")
public interface AuthApi {

    @PostMapping("/login")
    @Operation(
            summary = "User login",
            description = "Authenticates a user with username and password, returns JWT access token and sets refresh token cookie"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Login successful - JWT access token returned",
                    content = @io.swagger.v3.oas.annotations.media.Content(schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = JwtResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Bad request - Invalid credentials or missing required fields",
                    content = @io.swagger.v3.oas.annotations.media.Content(schema = @io.swagger.v3.oas.annotations.media.Schema(example = "{\"timestamp\":\"2024-01-01T12:00:00\",\"message\":\"Authentication failed\",\"path\":\"/api/auth/login\"}"))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Invalid username or password",
                    content = @io.swagger.v3.oas.annotations.media.Content(schema = @io.swagger.v3.oas.annotations.media.Schema(example = "{\"timestamp\":\"2024-01-01T12:00:00\",\"message\":\"Bad credentials\",\"path\":\"/api/auth/login\"}"))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "422",
                    description = "Unprocessable Entity - Validation errors in request body",
                    content = @io.swagger.v3.oas.annotations.media.Content(schema = @io.swagger.v3.oas.annotations.media.Schema(example = "{\"timestamp\":\"2024-01-01T12:00:00\",\"errors\":{\"username\":\"Username is required\",\"password\":\"Password must not be blank\"},\"message\":\"Validation failed\"}"))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "Internal server error - Database or system failure",
                    content = @io.swagger.v3.oas.annotations.media.Content(schema = @io.swagger.v3.oas.annotations.media.Schema(example = "{\"timestamp\":\"2024-01-01T12:00:00\",\"message\":\"Database connection failed\",\"path\":\"/api/auth/login\"}"))
            )
    })
    ResponseEntity<JwtResponse> login(
            @Parameter(description = "Login credentials", required = true)
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response
    );

    @PostMapping("/refresh")
    @Operation(
            summary = "Refresh access token",
            description = "Generates a new access token using a valid refresh token from cookie"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Token refreshed successfully - New JWT access token returned",
                    content = @io.swagger.v3.oas.annotations.media.Content(schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = JwtResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Bad request - Missing or invalid refresh token cookie",
                    content = @io.swagger.v3.oas.annotations.media.Content(schema = @io.swagger.v3.oas.annotations.media.Schema(example = "{\"timestamp\":\"2024-01-01T12:00:00\",\"message\":\"Missing refreshToken cookie\",\"path\":\"/api/auth/refresh\"}"))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Refresh token expired, invalid, or access token provided instead of refresh token",
                    content = @io.swagger.v3.oas.annotations.media.Content(schema = @io.swagger.v3.oas.annotations.media.Schema(example = "{\"timestamp\":\"2024-01-01T12:00:00\",\"message\":\"Token expired\",\"path\":\"/api/auth/refresh\"}"))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "User not found - User associated with refresh token no longer exists",
                    content = @io.swagger.v3.oas.annotations.media.Content(schema = @io.swagger.v3.oas.annotations.media.Schema(example = "{\"timestamp\":\"2024-01-01T12:00:00\",\"message\":\"User not found\",\"path\":\"/api/auth/refresh\"}"))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "Internal server error - Token generation or database failure",
                    content = @io.swagger.v3.oas.annotations.media.Content(schema = @io.swagger.v3.oas.annotations.media.Schema(example = "{\"timestamp\":\"2024-01-01T12:00:00\",\"message\":\"Token generation failed\",\"path\":\"/api/auth/refresh\"}"))
            )
    })
    ResponseEntity<JwtResponse> refreshToken(
            @Parameter(description = "Refresh token from HTTP-only cookie", required = true)
            @CookieValue("refreshToken") String refreshToken
    );

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
                    content = @io.swagger.v3.oas.annotations.media.Content(schema = @io.swagger.v3.oas.annotations.media.Schema(example = "{\"timestamp\":\"2024-01-01T12:00:00\",\"message\":\"Invalid token format\",\"path\":\"/api/auth/logout\"}"))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Invalid or expired tokens",
                    content = @io.swagger.v3.oas.annotations.media.Content(schema = @io.swagger.v3.oas.annotations.media.Schema(example = "{\"timestamp\":\"2024-01-01T12:00:00\",\"message\":\"Token expired\",\"path\":\"/api/auth/logout\"}"))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "Internal server error - Token blacklisting or database failure",
                    content = @io.swagger.v3.oas.annotations.media.Content(schema = @io.swagger.v3.oas.annotations.media.Schema(example = "{\"timestamp\":\"2024-01-01T12:00:00\",\"message\":\"Failed to blacklist token\",\"path\":\"/api/auth/logout\"}"))
            )
    })
    ResponseEntity<Void> logout(
            @Parameter(description = "Authorization header with Bearer token (optional)")
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @Parameter(description = "Refresh token from HTTP-only cookie (optional)")
            @CookieValue(value = "refreshToken", required = false) String refreshToken,
            HttpServletResponse response
    );
}
