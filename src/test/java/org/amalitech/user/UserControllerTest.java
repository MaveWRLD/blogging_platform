package org.amalitech.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.amalitech.user.dto.CreateUserRequest;
import org.amalitech.user.dto.UpdateUserRequest;
import org.amalitech.user.dto.UserDto;
import org.amalitech.user.Role;
import org.amalitech.user.User;
import org.amalitech.common.exception.CustomExceptionHandler;
import org.amalitech.user.UserMapper;
import org.amalitech.user.UserService;
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

import java.util.HashSet;
import java.util.Set;

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
    private Set<Role> readerRoles;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userController)
                .setControllerAdvice(new CustomExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();

        // Create default READER role
        Role readerRole = new Role();
        readerRole.setId(1);
        readerRole.setName("READER");
        readerRoles = new HashSet<>();
        readerRoles.add(readerRole);

        testUser = User.registerReader("testuser", "test@example.com", "password", "Test", "User", readerRoles);
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
        User invalidUser = User.registerReader("testuser", "test@example.com", "password", "Test", "User", readerRoles);
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
        when(userService.findByUserId(999L)).thenThrow(new org.amalitech.common.exception.ResourceNotFoundException("User not found"));

        mockMvc.perform(get("/api/users/999"))
                .andExpect(status().isNotFound());

        verify(userService).findByUserId(999L);
        verify(userMapper, never()).toDto(any(User.class));
    }

    @Test
    void promoteUserToWriter_WithValidUsername_ShouldPromoteUser() throws Exception {
        User promotedUser = User.registerReader("testuser", "test@example.com", "password", "Test", "User", readerRoles);
        promotedUser.setId(1L);
        
        Set<Role> writerRoles = new HashSet<>();
        Role writerRole = new Role();
        writerRole.setId(2);
        writerRole.setName("writer");
        writerRoles.add(writerRole);
        promotedUser.setRoles(writerRoles);
        
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
        when(userService.promoteToWriter("nonexistent")).thenThrow(new org.amalitech.common.exception.ResourceNotFoundException("User not found"));

        mockMvc.perform(put("/api/users/promote/nonexistent"))
                .andExpect(status().isNotFound());

        verify(userService).promoteToWriter("nonexistent");
    }

    @Test
    void promoteUserToWriter_WithAlreadyWriter_ShouldReturnSuccess() throws Exception {
        User writerUser = User.registerReader("testuser", "test@example.com", "password", "Test", "User", readerRoles);
        writerUser.setId(1L);
        
        Set<Role> writerRoles = new HashSet<>();
        Role writerRole = new Role();
        writerRole.setId(2);
        writerRole.setName("writer");
        writerRoles.add(writerRole);
        writerUser.setRoles(writerRoles);

        User promotedUser = User.registerReader("testuser", "test@example.com", "password", "Test", "User", readerRoles);
        promotedUser.setId(1L);
        
        Set<Role> promotedWriterRoles = new HashSet<>();
        Role promotedWriterRole = new Role();
        promotedWriterRole.setId(2);
        promotedWriterRole.setName("writer");
        promotedWriterRoles.add(promotedWriterRole);
        promotedUser.setRoles(promotedWriterRoles);
        
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
