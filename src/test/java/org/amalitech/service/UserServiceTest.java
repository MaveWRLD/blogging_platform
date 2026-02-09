package org.amalitech.service;

import org.amalitech.interfaces.RoleRepository;
import org.amalitech.interfaces.UserRepository;
import org.amalitech.interfaces.UserRoleRepository;
import org.amalitech.models.User;
import org.amalitech.util.PasswordHasher;
import org.amalitech.util.exception.ResourceNotFoundException;
import org.amalitech.util.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserRoleRepository userRoleRepository;
    @Mock
    private RoleRepository roleRepository;
    private UserService userService;
    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, userRoleRepository, roleRepository);
    }
    @Test
    void authenticate_withValidCredentials_returnsTrue() {
        User u = new User();
        u.setUsername("test");
        u.setPassword(PasswordHasher.hash("secret"));
        when(userRepository.findByUsername("test")).thenReturn(Optional.of(u));
        boolean ok = userService.authenticate("test", "secret");
        assertThat(ok).isTrue();
    }
    @Test
    void authenticate_withInvalidCredentials_throwsValidationException() {
        User u = new User();
        u.setUsername("test");
        u.setPassword(PasswordHasher.hash("secret"));
        when(userRepository.findByUsername("test")).thenReturn(Optional.of(u));
        assertThatThrownBy(() -> userService.authenticate("test", "wrong"))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Invalid credentials");
    }
    @Test
    void createUser_savesUserAndAssignsRole() {
        User user = new User();
        user.setUsername("newuser");
        user.setPassword("pass");
        when(userRepository.save(any(User.class))).thenReturn(1);
        org.amalitech.models.Role role = new org.amalitech.models.Role();
        role.setId(1);
        when(roleRepository.findByName("reader")).thenReturn(role);
        doNothing().when(userRoleRepository).assignRoleToUser(anyInt(), anyInt());
        userService.createUser(user);
        verify(userRepository).save(user);
        verify(userRoleRepository).assignRoleToUser(1, 1);
    }
    @Test
    void findByUserId_returnsUser() {
        User user = new User();
        user.setId(1);
        when(userRepository.findByUserId(1)).thenReturn(Optional.of(user));
        User found = userService.findByUserId(1);
        assertThat(found).isEqualTo(user);
    }
    @Test
    void findByUserId_throwsNotFoundWhenMissing() {
        when(userRepository.findByUserId(1)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> userService.findByUserId(1))
                .isInstanceOf(ResourceNotFoundException.class);
    }
    @Test
    void updateUser_updatesUser() {
        User user = new User();
        user.setId(1);
        user.setPassword("newpass");
        doNothing().when(userRepository).update(user);
        userService.updateUser(user);
        verify(userRepository).update(user);
        assertThat(user.getPassword()).isEqualTo(PasswordHasher.hash("newpass"));
    }
    @Test
    void deleteUser_deletesUser() {
        User user = new User();
        user.setId(1);
        when(userRepository.findByUserId(1)).thenReturn(Optional.of(user));
        doNothing().when(userRepository).delete(1);
        userService.deleteUser(1);
        verify(userRepository).delete(1);
    }
    @Test
    void findAllUsers_returnsUsers() {
        List<User> users = List.of(new User());
        when(userRepository.findAll()).thenReturn(users);
        List<User> found = userService.findAllUsers();
        assertThat(found).isEqualTo(users);
    }
}