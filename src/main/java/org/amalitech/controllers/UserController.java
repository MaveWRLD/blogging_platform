package org.amalitech.controllers;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.amalitech.dtos.ApiResponse;
import org.amalitech.dtos.userDtos.CreateUserRequest;
import org.amalitech.dtos.userDtos.UpdateUserRequest;
import org.amalitech.dtos.userDtos.UserDto;
import org.amalitech.mappers.UserMapper;
import org.amalitech.entities.User;
import org.amalitech.service.AuthorizationService;
import org.amalitech.service.UserService;
import org.amalitech.exception.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import java.util.List;


@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag( name = "User Controller", description = "Endpoints for managing users")
@SecurityRequirement(name = "bearerAuth")
public class UserController{

    private final AuthorizationService authorizationService;
    private final UserService userService;
    private final UserMapper userMapper;


    @PostMapping("/register")
    @Operation(
            summary = "Register a new user",
            description = "Register a new user in the system based on the provided user information. No authentication required."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "User registered successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Bad request - Invalid user data or missing required fields",
                    content = @Content(schema = @Schema(example = "{\"timestamp\":\"2024-01-01T12:00:00\",\"errors\":{\"username\":\"Username is required\",\"email\":\"Invalid email format\"},\"message\":\"Validation failed\"}"))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "Conflict - Username or email already exists",
                    content = @Content(schema = @Schema(example = "{\"timestamp\":\"2024-01-01T12:00:00\",\"message\":\"Username already exists\",\"path\":\"/api/users/register\"}"))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "422",
                    description = "Unprocessable Entity - Validation errors in request body",
                    content = @Content(schema = @Schema(example = "{\"timestamp\":\"2024-01-01T12:00:00\",\"errors\":{\"password\":\"Password must be at least 8 characters\"},\"message\":\"Validation failed\"}"))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "Internal server error - Database or system failure",
                    content = @Content(schema = @Schema(example = "{\"timestamp\":\"2024-01-01T12:00:00\",\"message\":\"Failed to create user\",\"path\":\"/api/users/register\"}"))
            )
    })
    public ResponseEntity<ApiResponse<UserDto>> register(
            @Parameter(description = "User registration request", required = true)
            @Valid @RequestBody CreateUserRequest request) {

        User user = userMapper.createUser(request);

        userService.createUser(user);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("User created successfully", userMapper.toDto(user)));
    }

    @GetMapping
    @PreAuthorize("hasRole('admin')")
    @Operation(
            summary = "Get all users",
            description = "Returns a list of all users in the system. Requires admin role."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Users retrieved successfully",
                    content = @Content(schema = @Schema(implementation = List.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Authentication required",
                    content = @Content(schema = @Schema(example = "{\"error\":\"Unauthorized\",\"message\":\"Full authentication is required to access this resource\",\"path\":\"/api/users\"}"))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - Admin role required",
                    content = @Content(schema = @Schema(example = "{\"error\":\"Forbidden\",\"message\":\"Access is denied\",\"path\":\"/api/users\"}"))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "Internal server error - Database or system failure",
                    content = @Content(schema = @Schema(example = "{\"timestamp\":\"2024-01-01T12:00:00\",\"message\":\"Failed to retrieve users\",\"path\":\"/api/users\"}"))
            )
    })
    public ResponseEntity<List<UserDto>> getAllUsers() {
        List<User> users = userService.findAllUsers();
        List<UserDto> userDtos = users.stream()
                .map(userMapper::toDto)
                .toList();
        return ResponseEntity.ok(userDtos);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('writer') or @authorizationService.canAccessUser(#id)")
    @Operation(
            summary = "Get user by ID",
            description = "Returns a single user with their role information based on the provided user ID. Requires writer role or user access authorization."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "User retrieved successfully",
                    content = @Content(schema = @Schema(implementation = UserDto.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Bad request - Invalid user ID",
                    content = @Content(schema = @Schema(example = "{\"timestamp\":\"2024-01-01T12:00:00\",\"message\":\"Invalid user ID\",\"path\":\"/api/users/invalid\"}"))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Authentication required",
                    content = @Content(schema = @Schema(example = "{\"error\":\"Unauthorized\",\"message\":\"Full authentication is required to access this resource\",\"path\":\"/api/users/1\"}"))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - Insufficient permissions to access this user",
                    content = @Content(schema = @Schema(example = "{\"error\":\"Forbidden\",\"message\":\"You can only access your own profile\",\"path\":\"/api/users/1\"}"))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "User not found",
                    content = @Content(schema = @Schema(example = "{\"timestamp\":\"2024-01-01T12:00:00\",\"message\":\"User not found with id: 1\",\"path\":\"/api/users/1\"}"))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "Internal server error - Database or system failure",
                    content = @Content(schema = @Schema(example = "{\"timestamp\":\"2024-01-01T12:00:00\",\"message\":\"Failed to retrieve user\",\"path\":\"/api/users/1\"}"))
            )
    })
    public ResponseEntity<UserDto> getUser(
            @Parameter(description = "User ID", required = true, example = "1")
            @PathVariable Long id) {
        User user = userService.findByUserId(id);
        return ResponseEntity.ok(userMapper.toDto(user));
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Update an existing user",
            description = "Updates an existing user's information based on the provided user ID and updated information. Requires authentication and proper authorization."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "User updated successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Bad request - Invalid user ID or update data",
                    content = @Content(schema = @Schema(example = "{\"timestamp\":\"2024-01-01T12:00:00\",\"message\":\"Invalid user ID\",\"path\":\"/api/users/1\"}"))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Authentication required",
                    content = @Content(schema = @Schema(example = "{\"error\":\"Unauthorized\",\"message\":\"Full authentication is required to access this resource\",\"path\":\"/api/users/1\"}"))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - User does not have permission to update this profile",
                    content = @Content(schema = @Schema(example = "{\"error\":\"Forbidden\",\"message\":\"You can only update your own profile\",\"path\":\"/api/users/1\"}"))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "User not found",
                    content = @Content(schema = @Schema(example = "{\"timestamp\":\"2024-01-01T12:00:00\",\"message\":\"User not found with id: 1\",\"path\":\"/api/users/1\"}"))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "422",
                    description = "Unprocessable Entity - Validation errors in request body",
                    content = @Content(schema = @Schema(example = "{\"timestamp\":\"2024-01-01T12:00:00\",\"errors\":{\"email\":\"Invalid email format\"},\"message\":\"Validation failed\"}"))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "Internal server error - Database or system failure",
                    content = @Content(schema = @Schema(example = "{\"timestamp\":\"2024-01-01T12:00:00\",\"message\":\"Failed to update user\",\"path\":\"/api/users/1\"}"))
            )
    })
    public ResponseEntity<ApiResponse<UserDto>> updateUser(
            @Parameter(description = "User ID", required = true, example = "1")
            @PathVariable int id,
            @Parameter(description = "User update request", required = true)
            @RequestBody UpdateUserRequest request) {
            User existing = userService.findByUserId(Long.parseLong(String.valueOf(id)));
            userMapper.updateEntity(request, existing);
            var updatedUser = userService.updateUser(id, existing);
            UserDto dto = userMapper.toDto(updatedUser);
            return ResponseEntity.ok(ApiResponse.success("User updated successfully", dto));
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Delete a user",
            description = "Deletes an existing user from the system based on the provided user ID. Requires authentication and proper authorization. This action is irreversible."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "User deleted successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Bad request - Invalid user ID",
                    content = @Content(schema = @Schema(example = "{\"timestamp\":\"2024-01-01T12:00:00\",\"message\":\"Invalid user ID\",\"path\":\"/api/users/1\"}"))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Authentication required",
                    content = @Content(schema = @Schema(example = "{\"error\":\"Unauthorized\",\"message\":\"Full authentication is required to access this resource\",\"path\":\"/api/users/1\"}"))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - User does not have permission to delete this account",
                    content = @Content(schema = @Schema(example = "{\"error\":\"Forbidden\",\"message\":\"You can only delete your own account\",\"path\":\"/api/users/1\"}"))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "User not found",
                    content = @Content(schema = @Schema(example = "{\"timestamp\":\"2024-01-01T12:00:00\",\"message\":\"User not found with id: 1\",\"path\":\"/api/users/1\"}"))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "Internal server error - Database or system failure",
                    content = @Content(schema = @Schema(example = "{\"timestamp\":\"2024-01-01T12:00:00\",\"message\":\"Failed to delete user\",\"path\":\"/api/users/1\"}"))
            )
    })
    public ResponseEntity<ApiResponse<Void>> deleteUser(
            @Parameter(description = "User ID", required = true, example = "1")
            @PathVariable int id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.NO_CONTENT,"User deleted successfully", null));

    }

    @PutMapping("/promote/{username}")
    @Operation(
            summary = "Promote user to writer",
            description = "Set a user's role to WRITER based on their username. Requires admin privileges."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "User promoted successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Bad request - Invalid username format",
                    content = @Content(schema = @Schema(example = "{\"timestamp\":\"2024-01-01T12:00:00\",\"message\":\"Invalid username format\",\"path\":\"/api/users/promote/invalid\"}"))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Authentication required",
                    content = @Content(schema = @Schema(example = "{\"error\":\"Unauthorized\",\"message\":\"Full authentication is required to access this resource\",\"path\":\"/api/users/promote/username\"}"))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - Admin role required",
                    content = @Content(schema = @Schema(example = "{\"error\":\"Forbidden\",\"message\":\"Admin role required to promote users\",\"path\":\"/api/users/promote/username\"}"))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "User not found",
                    content = @Content(schema = @Schema(example = "{\"timestamp\":\"2024-01-01T12:00:00\",\"message\":\"User not found\",\"path\":\"/api/users/promote/nonexistent\"}"))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "Conflict - User is already a writer",
                    content = @Content(schema = @Schema(example = "{\"timestamp\":\"2024-01-01T12:00:00\",\"message\":\"User is already a writer\",\"path\":\"/api/users/promote/username\"}"))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "Internal server error - Database or system failure",
                    content = @Content(schema = @Schema(example = "{\"timestamp\":\"2024-01-01T12:00:00\",\"message\":\"Failed to promote user\",\"path\":\"/api/users/promote/username\"}"))
            )
    })
    public ResponseEntity<ApiResponse<UserDto>> promoteToWriter(
            @Parameter(description = "Username of the user to promote", required = true, example = "john_doe")
            @PathVariable String username) {
        try {
            User promoted = userService.promoteToWriter(username);
            return ResponseEntity.ok(ApiResponse.success("User promoted to WRITER", userMapper.toDto(promoted)));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("User not found"));
        }
    }
}
