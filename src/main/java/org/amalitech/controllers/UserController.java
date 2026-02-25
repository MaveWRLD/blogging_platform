package org.amalitech.controllers;

import lombok.RequiredArgsConstructor;
import org.amalitech.api.doc.UserApi;
import org.amalitech.dtos.CustomApiResponse;
import org.amalitech.dtos.userDtos.CreateUserRequest;
import org.amalitech.dtos.userDtos.UserDto;
import org.amalitech.mappers.UserMapper;
import org.amalitech.entities.User;
import org.amalitech.service.AuthorizationService;
import org.amalitech.service.UserService;
import org.amalitech.exception.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController implements UserApi {

    private final UserService userService;
    private final UserMapper userMapper;

    @Override
    public ResponseEntity<UserDto> getUser(Long id) {
        User user = userService.findByUserId(id);
        return ResponseEntity.ok(userMapper.toDto(user));
    }

    private final AuthorizationService authorizationService;


    @Override
    public ResponseEntity<CustomApiResponse<UserDto>> register(CreateUserRequest request) {

        User user = userMapper.createUser(request);

        userService.createUser(user);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(CustomApiResponse.success("User created successfully", userMapper.toDto(user)));
    }

    @Override
    public ResponseEntity<CustomApiResponse<UserDto>> promoteToWriter(String username) {
        try {
            User promoted = userService.promoteToWriter(username);
            return ResponseEntity.ok(CustomApiResponse.success("User promoted to WRITER", userMapper.toDto(promoted)));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(CustomApiResponse.error("User not found"));
        }
    }
}
