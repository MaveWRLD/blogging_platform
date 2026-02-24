package org.amalitech.api.doc;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.amalitech.dtos.CustomApiResponse;
import org.amalitech.dtos.userDtos.UserDto;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/api/users")
@Tag(name = "User Controller", description = "Endpoints for managing users")
@SecurityRequirement(name = "bearerAuth")
public interface UserApi {

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('admin') or @authorizationService.canAccessUser(#id)")
    @Operation(
            summary = "Get user by ID",
            description = "Returns a single user with their role information based on the provided user ID. Requires writer role or user access authorization."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "User retrieved successfully",
                    content = @io.swagger.v3.oas.annotations.media.Content(schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = UserDto.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Bad request - Invalid user ID",
                    content = @io.swagger.v3.oas.annotations.media.Content(schema = @io.swagger.v3.oas.annotations.media.Schema(example = "{\"timestamp\":\"2024-01-01T12:00:00\",\"message\":\"Invalid user ID\",\"path\":\"/api/users/invalid\"}"))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Authentication required",
                    content = @io.swagger.v3.oas.annotations.media.Content(schema = @io.swagger.v3.oas.annotations.media.Schema(example = "{\"error\":\"Unauthorized\",\"message\":\"Full authentication is required to access this resource\",\"path\":\"/api/users/1\"}"))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - Insufficient permissions to access this user",
                    content = @io.swagger.v3.oas.annotations.media.Content(schema = @io.swagger.v3.oas.annotations.media.Schema(example = "{\"error\":\"Forbidden\",\"message\":\"You can only access your own profile\",\"path\":\"/api/users/1\"}"))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "User not found",
                    content = @io.swagger.v3.oas.annotations.media.Content(schema = @io.swagger.v3.oas.annotations.media.Schema(example = "{\"timestamp\":\"2024-01-01T12:00:00\",\"message\":\"User not found with id: 1\",\"path\":\"/api/users/1\"}"))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "Internal server error - Database or system failure",
                    content = @io.swagger.v3.oas.annotations.media.Content(schema = @io.swagger.v3.oas.annotations.media.Schema(example = "{\"timestamp\":\"2024-01-01T12:00:00\",\"message\":\"Failed to retrieve user\",\"path\":\"/api/users/1\"}"))
            )
    })
    ResponseEntity<UserDto> getUser(
            @Parameter(description = "User ID", required = true, example = "1")
            @PathVariable Long id
    );

    @PutMapping("/promote/{username}")
    @PreAuthorize("@authorizationService.canPromoteUser()")
    @Operation(
            summary = "Promote user to writer",
            description = "Set a user's role to WRITER based on their username. Requires admin privileges."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "User promoted successfully",
                    content = @io.swagger.v3.oas.annotations.media.Content(schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = CustomApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Bad request - Invalid username format",
                    content = @io.swagger.v3.oas.annotations.media.Content(schema = @io.swagger.v3.oas.annotations.media.Schema(example = "{\"timestamp\":\"2024-01-01T12:00:00\",\"message\":\"Invalid username format\",\"path\":\"/api/users/promote/invalid\"}"))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Authentication required",
                    content = @io.swagger.v3.oas.annotations.media.Content(schema = @io.swagger.v3.oas.annotations.media.Schema(example = "{\"error\":\"Unauthorized\",\"message\":\"Full authentication is required to access this resource\",\"path\":\"/api/users/promote/username\"}"))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - Admin role required",
                    content = @io.swagger.v3.oas.annotations.media.Content(schema = @io.swagger.v3.oas.annotations.media.Schema(example = "{\"error\":\"Forbidden\",\"message\":\"Admin role required to promote users\",\"path\":\"/api/users/promote/username\"}"))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "User not found",
                    content = @io.swagger.v3.oas.annotations.media.Content(schema = @io.swagger.v3.oas.annotations.media.Schema(example = "{\"timestamp\":\"2024-01-01T12:00:00\",\"message\":\"User not found\",\"path\":\"/api/users/promote/nonexistent\"}"))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "Conflict - User is already a writer",
                    content = @io.swagger.v3.oas.annotations.media.Content(schema = @io.swagger.v3.oas.annotations.media.Schema(example = "{\"timestamp\":\"2024-01-01T12:00:00\",\"message\":\"User is already a writer\",\"path\":\"/api/users/promote/username\"}"))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "Internal server error - Database or system failure",
                    content = @io.swagger.v3.oas.annotations.media.Content(schema = @io.swagger.v3.oas.annotations.media.Schema(example = "{\"timestamp\":\"2024-01-01T12:00:00\",\"message\":\"Failed to promote user\",\"path\":\"/api/users/promote/username\"}"))
            )
    })
    ResponseEntity<CustomApiResponse<UserDto>> promoteToWriter(
            @Parameter(description = "Username of the user to promote", required = true, example = "john_doe")
            @PathVariable String username
    );
}
