package org.amalitech.controllers;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.amalitech.dtos.ApiResponse;
import org.amalitech.dtos.userDtos.CreateUserRequest;
import org.amalitech.dtos.userDtos.UpdateUserRequest;
import org.amalitech.dtos.userDtos.UserDto;
import org.amalitech.dtos.userDtos.UserWithRoleDto;
import org.amalitech.mappers.UserMapper;
import org.amalitech.models.User;
import org.amalitech.service.UserService;
import org.amalitech.util.exception.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag( name = "User Controller", description = "Endpoints for managing users")
public class UserController{

    private final UserService userService;
    private final UserMapper userMapper;

    @GetMapping
    @Operation(
            summary = "Get all users",
            description = "Returns a list of all users in the system"
    )
    public ResponseEntity<List<UserDto>> getAllUsers() {
        List<User> users = userService.findAllUsers();
        List<UserDto> userDtos = users.stream()
                .map(userMapper::toUserDto)
                .toList();
        return ResponseEntity.ok(userDtos);
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get user by ID",
            description = "Returns a single user with their role information based on the provided user ID"
    )
    public ResponseEntity<UserWithRoleDto> getUser(@PathVariable int id) {
        User user = userService.findByUserId(id);
        if (user == null)
            return ResponseEntity.badRequest().build();
        return ResponseEntity.ok(userMapper.toDto(user));
    }

    @PostMapping
    @Operation(
            summary = "Create a new user",
            description = "Creates a new user in the system based on the provided user information"
    )
    public ResponseEntity<ApiResponse<UserDto>> createUser(
            @Valid @RequestBody CreateUserRequest request) {

        User user = userMapper.toEntity(request);

        userService.createUser(user);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(userMapper.toUserDto(user), "User created successfully"));
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Update an existing user",
            description = "Updates an existing user's information based on the provided user ID and updated information"
    )
    public ResponseEntity<ApiResponse<UserDto>> updateUser(
            @PathVariable int id,
            @RequestBody UpdateUserRequest request) {
            User existing = userService.findByUserId(id);
            userMapper.updateEntity(request, existing);
            userService.updateUser(existing);
            UserDto dto = userMapper.toUserDto(existing);
            return ResponseEntity.ok(ApiResponse.success(dto, "User updated successfully"));
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Delete a user",
            description = "Deletes an existing user from the system based on the provided user ID"
    )
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable int id) {
        try {
            User existing = userService.findByUserId(id);
            if (existing == null) {
                throw new ResourceNotFoundException("User not found");
            }
            userService.deleteUser(id);
            return ResponseEntity.ok(ApiResponse.success(null, "User deleted successfully"));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
