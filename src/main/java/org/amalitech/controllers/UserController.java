package org.amalitech.controllers;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.amalitech.dtos.ApiResponse;
import org.amalitech.dtos.userDtos.CreateUserRequest;
import org.amalitech.dtos.userDtos.UpdateUserRequest;
import org.amalitech.dtos.userDtos.UserDto;
import org.amalitech.mappers.PostMapper;
import org.amalitech.mappers.UserMapper;
import org.amalitech.entities.User;
import org.amalitech.service.UserService;
import org.amalitech.exception.ResourceNotFoundException;
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
                .body(ApiResponse.success("User created successfully", userMapper.toDto(user)));
    }

    @GetMapping
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
    @Operation(
            summary = "Get user by ID",
            description = "Returns a single user with their role information based on the provided user ID"
    )
    public ResponseEntity<UserDto> getUser(@PathVariable Long id) {
        User user = userService.findByUserId(id);
        if (user == null)
            return ResponseEntity.badRequest().build();
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
        try {
            User existing = userService.findByUserId(Long.parseLong(String.valueOf(id)));
            if (existing == null) {
                throw new ResourceNotFoundException("User not found");
            }
            userService.deleteUser(id);
            return ResponseEntity.ok(ApiResponse.success(HttpStatus.NO_CONTENT,"User deleted successfully", null));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
