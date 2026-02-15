package org.amalitech.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.amalitech.dtos.userDtos.CreateUserRequest;
import org.amalitech.dtos.userDtos.UpdateUserRequest;
import org.amalitech.dtos.userDtos.UserDto;
import org.amalitech.entities.User;
import org.amalitech.exception.ResourceNotFoundException;
import org.amalitech.exception.ValidationException;
import org.amalitech.mappers.PostMapper;
import org.amalitech.mappers.UserMapper;
import org.amalitech.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@DisplayName("UserController Tests")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private UserMapper userMapper;

    @MockitoBean
    private PostMapper postMapper;

    private User sampleUser;
    private UserDto sampleUserDto;

    @BeforeEach
    void setUp() {
        sampleUser = new User();
        sampleUser.setId(1L);
        sampleUser.setUsername("johndoe");
        sampleUser.setEmail("john@example.com");

        sampleUserDto = new UserDto();
        sampleUserDto.setId(1L);
        sampleUserDto.setUsername("johndoe");
        sampleUserDto.setEmail("john@example.com");
    }

    @Nested
    @DisplayName("POST /api/users")
    class CreateUser {

        @Test
        @DisplayName("returns 400 when request body is missing")
        void missingBody_returns400() throws Exception {
            mockMvc.perform(post("/api/users")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("returns 400 when username is already taken")
        void duplicateUsername_returns400() throws Exception {
            CreateUserRequest request = new CreateUserRequest();
            request.setUsername("johndoe");
            request.setEmail("john@example.com");
            request.setPassword("secret123");

            when(userMapper.toEntity(any(CreateUserRequest.class))).thenReturn(sampleUser);
            when(userService.createUser(any(User.class)))
                    .thenThrow(new ValidationException("Username already exists"));

            mockMvc.perform(post("/api/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/users")
    class GetAllUsers {

        @Test
        @DisplayName("returns 200 with list of user DTOs")
        void usersExist_returns200WithList() throws Exception {
            when(userService.findAllUsers()).thenReturn(List.of(sampleUser));
            when(userMapper.toDto(sampleUser)).thenReturn(sampleUserDto);

            mockMvc.perform(get("/api/users"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].id").value(1))
                    .andExpect(jsonPath("$[0].username").value("johndoe"));
        }

        @Test
        @DisplayName("returns 200 with empty list when no users exist")
        void noUsers_returns200WithEmptyList() throws Exception {
            when(userService.findAllUsers()).thenReturn(List.of());

            mockMvc.perform(get("/api/users"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        @DisplayName("maps each user through the mapper")
        void multipleUsers_mapsAll() throws Exception {
            User second = new User();
            second.setId(2L);
            UserDto secondDto = new UserDto();
            secondDto.setId(2L);

            when(userService.findAllUsers()).thenReturn(List.of(sampleUser, second));
            when(userMapper.toDto(sampleUser)).thenReturn(sampleUserDto);
            when(userMapper.toDto(second)).thenReturn(secondDto);

            mockMvc.perform(get("/api/users"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)));

            verify(userMapper, times(2)).toDto(any(User.class));
        }
    }

    @Nested
    @DisplayName("GET /api/users/{id}")
    class GetUser {

        @Test
        @DisplayName("returns 200 with user DTO when found")
        void found_returns200WithDto() throws Exception {
            when(userService.findByUserId(1L)).thenReturn(sampleUser);
            when(userMapper.toDto(sampleUser)).thenReturn(sampleUserDto);

            mockMvc.perform(get("/api/users/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.username").value("johndoe"))
                    .andExpect(jsonPath("$.email").value("john@example.com"));
        }

        @Test
        @DisplayName("returns 404 when user does not exist")
        void notFound_returns404() throws Exception {
            when(userService.findByUserId(99L))
                    .thenThrow(new ResourceNotFoundException("User not found with id: 99"));

            mockMvc.perform(get("/api/users/99"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("returns 400 when service returns null user")
        void serviceReturnsNull_returns400() throws Exception {
            when(userService.findByUserId(1L)).thenReturn(null);

            mockMvc.perform(get("/api/users/1"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("PUT /api/users/{id}")
    class UpdateUser {

        @Test
        @DisplayName("returns 200 with updated user DTO")
        void validUpdate_returns200WithDto() throws Exception {
            UpdateUserRequest request = new UpdateUserRequest();
            request.setUsername("newname");

            when(userService.findByUserId(1L)).thenReturn(sampleUser);
            doNothing().when(userMapper).updateEntity(any(UpdateUserRequest.class), any(User.class));
            when(userService.updateUser(eq(1), any(User.class))).thenReturn(sampleUser);
            when(userMapper.toDto(sampleUser)).thenReturn(sampleUserDto);

            mockMvc.perform(put("/api/users/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("User updated successfully"))
                    .andExpect(jsonPath("$.data.id").value(1));

            verify(userMapper).updateEntity(any(UpdateUserRequest.class), eq(sampleUser));
            verify(userService).updateUser(eq(1), eq(sampleUser));
        }

        @Test
        @DisplayName("returns 404 when user to update does not exist")
        void userNotFound_returns404() throws Exception {
            UpdateUserRequest request = new UpdateUserRequest();
            request.setUsername("newname");

            when(userService.findByUserId(99L))
                    .thenThrow(new ResourceNotFoundException("User not found with id: 99"));

            mockMvc.perform(put("/api/users/99")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /api/users/{id}")
    class DeleteUser {

        @Test
        @DisplayName("returns 200 with success message on deletion")
        void existingUser_returns200() throws Exception {
            when(userService.findByUserId(1L)).thenReturn(sampleUser);
            doNothing().when(userService).deleteUser(1);

            mockMvc.perform(delete("/api/users/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("User deleted successfully"));

            verify(userService).deleteUser(1);
        }

        @Test
        @DisplayName("returns 404 when user is not found")
        void userNotFound_returns404() throws Exception {
            when(userService.findByUserId(99L))
                    .thenThrow(new ResourceNotFoundException("User not found with id: 99"));

            mockMvc.perform(delete("/api/users/99"))
                    .andExpect(status().isNotFound());

            verify(userService, never()).deleteUser(anyInt());
        }

        @Test
        @DisplayName("returns 404 when findByUserId returns null (defensive check)")
        void findReturnsNull_returns404() throws Exception {
            when(userService.findByUserId(1L)).thenReturn(null);

            mockMvc.perform(delete("/api/users/1"))
                    .andExpect(status().isNotFound());

            verify(userService, never()).deleteUser(anyInt());
        }
    }
}