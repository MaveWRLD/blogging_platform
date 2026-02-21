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

import java.util.List;


@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag( name = "User Controller", description = "Endpoints for managing users")
public class UserController{

    private final AuthorizationService authorizationService;
    private final UserService userService;
    private final UserMapper userMapper;


    @PostMapping("/register")
    @Operation(
            summary = "Register a new user",
            description = "Register a new user in the system based on the provided user information"
    )
    public ResponseEntity<ApiResponse<UserDto>> register(
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
            description = "Returns a list of all users in the system"
    )
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
            description = "Returns a single user with their role information based on the provided user ID"
    )
    public ResponseEntity<UserDto> getUser(@PathVariable Long id) {
        User user = userService.findByUserId(id);
        return ResponseEntity.ok(userMapper.toDto(user));
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Update an existing user",
            description = "Updates an existing user's information based on the provided user ID and updated information"
    )
    public ResponseEntity<ApiResponse<UserDto>> updateUser(
            @PathVariable int id,
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
            description = "Deletes an existing user from the system based on the provided user ID"
    )
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable int id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.NO_CONTENT,"User deleted successfully", null));

    }

    @PutMapping("/promote/{username}")
    @Operation(
            summary = "Promote user to writer",
            description = "Set a user's role to WRITER based on their username"
    )
    public ResponseEntity<ApiResponse<UserDto>> promoteToWriter(@PathVariable String username) {
        try {
            User promoted = userService.promoteToWriter(username);
            return ResponseEntity.ok(ApiResponse.success("User promoted to WRITER", userMapper.toDto(promoted)));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("User not found"));
        }
    }
}
