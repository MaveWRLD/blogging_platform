package org.amalitech.controllers;

import org.amalitech.dtos.ApiResponse;
import org.amalitech.dtos.userDtos.CreateUserRequest;
import org.amalitech.dtos.userDtos.UpdateUserRequest;
import org.amalitech.dtos.userDtos.UserDto;
import org.amalitech.dtos.userDtos.UserWithRoleDto;
import org.amalitech.mappers.UserMapper;
import org.amalitech.models.User;
import org.amalitech.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class UserControllerTest {
    @Mock
    private UserService userService;
    @Mock
    private UserMapper userMapper;
    private UserController controller;
    @BeforeEach
    void setUp() {
        controller = new UserController(userService, userMapper);
    }
    @Test
    void getAllUsers_returnsUsers() {
        List<User> users = List.of(new User());
        when(userService.findAllUsers()).thenReturn(users);
        when(userMapper.toUserDto(any(User.class))).thenReturn(new UserDto());
        ResponseEntity<List<UserDto>> response = controller.getAllUsers();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
    }
    @Test
    void getUser_returnsUser() {
        User user = new User();
        when(userService.findByUserId(1)).thenReturn(user);
        when(userMapper.toDto(user)).thenReturn(new UserWithRoleDto());
        ResponseEntity<UserWithRoleDto> response = controller.getUser(1);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
    @Test
    void createUser_createsUser() {
        CreateUserRequest request = new CreateUserRequest();
        User user = new User();
        when(userMapper.toEntity(request)).thenReturn(user);
        doNothing().when(userService).createUser(user);
        when(userMapper.toUserDto(user)).thenReturn(new UserDto());
        ResponseEntity<ApiResponse<UserDto>> response = controller.createUser(request);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }
    @Test
    void updateUser_updatesUser() {
        UpdateUserRequest request = new UpdateUserRequest();
        User user = new User();
        when(userService.findByUserId(1)).thenReturn(user);
        doNothing().when(userMapper).updateEntity(request, user);
        doNothing().when(userService).updateUser(user);
        when(userMapper.toUserDto(user)).thenReturn(new UserDto());
        ResponseEntity<ApiResponse<UserDto>> response = controller.updateUser(1, request);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
    @Test
    void deleteUser_deletesUser() {
        when(userService.findByUserId(1)).thenReturn(new User());
        doNothing().when(userService).deleteUser(1);
        ResponseEntity<ApiResponse<Void>> response = controller.deleteUser(1);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}