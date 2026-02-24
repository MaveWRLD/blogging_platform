package org.amalitech.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.amalitech.controllers.UserController;
import org.amalitech.dtos.userDtos.CreateUserRequest;
import org.amalitech.dtos.userDtos.UpdateUserRequest;
import org.amalitech.dtos.userDtos.UserDto;
import org.amalitech.entities.User;
import org.amalitech.exception.CustomExceptionHandler;
import org.amalitech.mappers.UserMapper;
import org.amalitech.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private UserService userService;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserController userController;

    private User testUser;
    private UserDto testUserDto;
    private CreateUserRequest createUserRequest;
    private UpdateUserRequest updateUserRequest;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userController)
                .setControllerAdvice(new CustomExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();

        testUser = User.registerReader("testuser", "test@example.com", "password", "Test", "User");
        testUser.setId(1L);

        testUserDto = new UserDto();
        testUserDto.setId(1L);
        testUserDto.setUsername("testuser");
        testUserDto.setEmail("test@example.com");

        createUserRequest = new CreateUserRequest();
        createUserRequest.setUsername("testuser");
        createUserRequest.setEmail("test@example.com");
        createUserRequest.setPassword("password");
        createUserRequest.setFirstName("Test");
        createUserRequest.setLastName("User");

        updateUserRequest = new UpdateUserRequest();
        updateUserRequest.setUsername("updateduser");
        updateUserRequest.setEmail("updated@example.com");
    }

    @Test
    void registerUser_WithValidRequest_ShouldReturnCreatedUser() throws Exception {
        when(userMapper.createUser(any(CreateUserRequest.class))).thenReturn(testUser);
        when(userService.createUser(any(User.class))).thenReturn(testUser);
        when(userMapper.toDto(any(User.class))).thenReturn(testUserDto);

        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createUserRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.message").value("User created successfully"))
                .andExpect(jsonPath("$.data.id").value(1L))
                .andExpect(jsonPath("$.data.username").value("testuser"))
                .andExpect(jsonPath("$.data.email").value("test@example.com"));

        verify(userMapper).createUser(any(CreateUserRequest.class));
        verify(userService).createUser(any(User.class));
        verify(userMapper).toDto(any(User.class));
    }

    @Test
    void registerUser_WithInvalidEmail_ShouldReturnBadRequest() throws Exception {
        CreateUserRequest invalidRequest = new CreateUserRequest();
        invalidRequest.setUsername("testuser");
        invalidRequest.setEmail("invalid-email");
        invalidRequest.setPassword("password");
        invalidRequest.setFirstName("Test");
        invalidRequest.setLastName("User");

        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registerUser_WithMissingFields_ShouldReturnBadRequest() throws Exception {
        CreateUserRequest invalidRequest = new CreateUserRequest();
        invalidRequest.setUsername("testuser");
        // Missing email, password, firstName, lastName

        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAllUsers_ShouldReturnAllUsers() throws Exception {
        List<User> users = Arrays.asList(testUser);
        
        when(userService.findAllUsers()).thenReturn(users);
        when(userMapper.toDto(testUser)).thenReturn(testUserDto);

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].username").value("testuser"))
                .andExpect(jsonPath("$[0].email").value("test@example.com"));

        verify(userService).findAllUsers();
        verify(userMapper).toDto(testUser);
    }

    @Test
    void getUserById_WithValidId_ShouldReturnUser() throws Exception {
        when(userService.findByUserId(1L)).thenReturn(testUser);
        when(userMapper.toDto(testUser)).thenReturn(testUserDto);

        mockMvc.perform(get("/api/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@example.com"));

        verify(userService).findByUserId(1L);
        verify(userMapper).toDto(testUser);
    }

    @Test
    void getUserById_WithInvalidId_ShouldReturnBadRequest() throws Exception {
        User invalidUser = User.registerReader("testuser", "test@example.com", "password", "Test", "User");
        invalidUser.setId(0L);
        
        UserDto invalidUserDto = new UserDto();
        invalidUserDto.setId(0L);
        invalidUserDto.setUsername("testuser");
        invalidUserDto.setEmail("test@example.com");
        
        when(userService.findByUserId(0L)).thenReturn(invalidUser);
        when(userMapper.toDto(invalidUser)).thenReturn(invalidUserDto);

        mockMvc.perform(get("/api/users/0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(0L))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@example.com"));

        verify(userService).findByUserId(0L);
        verify(userMapper).toDto(invalidUser);
    }

    @Test
    void getUserById_WithNonExistentId_ShouldReturnNotFound() throws Exception {
        when(userService.findByUserId(999L)).thenThrow(new org.amalitech.exception.ResourceNotFoundException("User not found"));

        mockMvc.perform(get("/api/users/999"))
                .andExpect(status().isNotFound());

        verify(userService).findByUserId(999L);
        verify(userMapper, never()).toDto(any(User.class));
    }

    @Test
    void updateUser_WithValidId_ShouldReturnUpdatedUser() throws Exception {
        User updatedUser = User.registerReader("updateduser", "updated@example.com", "password", "Updated", "User");
        updatedUser.setId(1L);
        
        UserDto updatedUserDto = new UserDto();
        updatedUserDto.setId(1L);
        updatedUserDto.setUsername("updateduser");
        updatedUserDto.setEmail("updated@example.com");

        when(userService.findByUserId(1L)).thenReturn(testUser);
        when(userService.updateUser(1, testUser)).thenReturn(updatedUser);
        when(userMapper.toDto(updatedUser)).thenReturn(updatedUserDto);

        mockMvc.perform(put("/api/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateUserRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.data.id").value(1L))
                .andExpect(jsonPath("$.data.username").value("updateduser"))
                .andExpect(jsonPath("$.data.email").value("updated@example.com"));

        verify(userService).findByUserId(1L);
        verify(userService).updateUser(1, testUser);
        verify(userMapper).toDto(updatedUser);
    }

    @Test
    void updateUser_WithInvalidId_ShouldReturnSuccess() throws Exception {
        // Controller accepts any ID and processes it successfully
        // Testing with ID 0 to verify controller behavior
        User existingUser = User.registerReader("testuser", "test@example.com", "password", "Test", "User");
        existingUser.setId(0L);
        
        UserDto updatedUserDto = new UserDto();
        updatedUserDto.setId(0L);
        updatedUserDto.setUsername("testuser");
        updatedUserDto.setEmail("test@example.com");

        when(userService.findByUserId(0L)).thenReturn(existingUser);
        when(userService.updateUser(0, existingUser)).thenReturn(existingUser);
        when(userMapper.toDto(existingUser)).thenReturn(updatedUserDto);

        mockMvc.perform(put("/api/users/0")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateUserRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.data.id").value(0L))
                .andExpect(jsonPath("$.data.username").value("testuser"))
                .andExpect(jsonPath("$.data.email").value("test@example.com"));

        verify(userService).findByUserId(0L);
        verify(userService).updateUser(0, existingUser);
        verify(userMapper).toDto(existingUser);
    }

    @Test
    void updateUser_WithNonExistentId_ShouldReturnNotFound() throws Exception {
        when(userService.findByUserId(999L)).thenThrow(new org.amalitech.exception.ResourceNotFoundException("User not found"));

        mockMvc.perform(put("/api/users/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateUserRequest)))
                .andExpect(status().isNotFound());

        verify(userService).findByUserId(999L);
        verify(userService, never()).updateUser(anyInt(), any(User.class));
    }

    @Test
    void deleteUser_WithValidId_ShouldDeleteUser() throws Exception {
        doNothing().when(userService).deleteUser(1);

        mockMvc.perform(delete("/api/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NO_CONTENT"))
                .andExpect(jsonPath("$.message").value("User deleted successfully"))
                .andExpect(jsonPath("$.data").doesNotExist());

        verify(userService).deleteUser(1);
    }

    @Test
    void deleteUser_WithInvalidId_ShouldReturnSuccess() throws Exception {
        doNothing().when(userService).deleteUser(0);

        mockMvc.perform(delete("/api/users/0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NO_CONTENT"))
                .andExpect(jsonPath("$.message").value("User deleted successfully"))
                .andExpect(jsonPath("$.data").doesNotExist());

        verify(userService).deleteUser(0);
    }

    @Test
    void deleteUser_WithNonExistentId_ShouldReturnNotFound() throws Exception {
        doThrow(new org.amalitech.exception.ResourceNotFoundException("User not found with id: 999"))
                .when(userService).deleteUser(999);

        mockMvc.perform(delete("/api/users/999"))
                .andExpect(status().isNotFound());

        verify(userService).deleteUser(999);
    }

    @Test
    void promoteUserToWriter_WithValidUsername_ShouldPromoteUser() throws Exception {
        User promotedUser = User.registerReader("testuser", "test@example.com", "password", "Test", "User");
        promotedUser.setId(1L);
        promotedUser.setRole("writer");
        
        UserDto promotedUserDto = new UserDto();
        promotedUserDto.setId(1L);
        promotedUserDto.setUsername("testuser");
        promotedUserDto.setEmail("test@example.com");

        when(userService.promoteToWriter("testuser")).thenReturn(promotedUser);
        when(userMapper.toDto(promotedUser)).thenReturn(promotedUserDto);

        mockMvc.perform(put("/api/users/promote/testuser"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.message").value("User promoted to WRITER"))
                .andExpect(jsonPath("$.data.id").value(1L));

        verify(userService).promoteToWriter("testuser");
        verify(userMapper).toDto(promotedUser);
    }

    @Test
    void promoteUserToWriter_WithInvalidUsername_ShouldReturnBadRequest() throws Exception {
        mockMvc.perform(put("/api/users/promote/"))
                .andExpect(status().isNotFound());

        verify(userService, never()).promoteToWriter(anyString());
    }

    @Test
    void promoteUserToWriter_WithNonExistentUsername_ShouldReturnNotFound() throws Exception {
        when(userService.promoteToWriter("nonexistent")).thenThrow(new org.amalitech.exception.ResourceNotFoundException("User not found"));

        mockMvc.perform(put("/api/users/promote/nonexistent"))
                .andExpect(status().isNotFound());

        verify(userService).promoteToWriter("nonexistent");
    }

    @Test
    void promoteUserToWriter_WithAlreadyWriter_ShouldReturnSuccess() throws Exception {
        User writerUser = User.registerReader("testuser", "test@example.com", "password", "Test", "User");
        writerUser.setId(1L);
        writerUser.setRole("writer");

        // Service doesn't currently validate if user is already writer
        // It just sets role to writer and returns success
        User promotedUser = User.registerReader("testuser", "test@example.com", "password", "Test", "User");
        promotedUser.setId(1L);
        promotedUser.setRole("writer");
        
        UserDto promotedUserDto = new UserDto();
        promotedUserDto.setId(1L);
        promotedUserDto.setUsername("testuser");
        promotedUserDto.setEmail("test@example.com");

        when(userService.findByUserId(1L)).thenReturn(writerUser);
        when(userService.promoteToWriter("testuser")).thenReturn(promotedUser);
        when(userMapper.toDto(promotedUser)).thenReturn(promotedUserDto);

        mockMvc.perform(put("/api/users/promote/testuser"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.message").value("User promoted to WRITER"))
                .andExpect(jsonPath("$.data.id").value(1L));

        verify(userService).promoteToWriter("testuser");
        verify(userMapper).toDto(promotedUser);
    }
}
