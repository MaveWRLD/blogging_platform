package org.amalitech.service;

import org.amalitech.entities.User;
import org.amalitech.entities.Role;
import org.amalitech.exception.ResourceNotFoundException;
import org.amalitech.exception.ValidationException;
import org.amalitech.repositories.UserRepository;
import org.amalitech.util.PasswordHasher;
import org.amalitech.util.UserValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.HashSet;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Tests")
class UserServiceTest {

   @Mock
   private UserRepository userRepository;

   @Mock
   private PasswordEncoder passwordEncoder;

   @InjectMocks
   private UserService userService;

   private User sampleUser;
   private Set<Role> readerRoles;

   @BeforeEach
   void setUp() {
       // Create a sample role for testing
       Role readerRole = new Role();
       readerRole.setId(1);
       readerRole.setName("reader");
       readerRoles = new HashSet<>();
       readerRoles.add(readerRole);

       sampleUser = User.registerReader(
               "johndoe",
               "john@example.com",
               "plainpassword",
               "John",
               "Doe",
               readerRoles
       );
       sampleUser.setId(1L);
   }

   private User createTestUser(String username, String email, String password, String firstName, String lastName) {
       return User.registerReader(username, email, password, firstName, lastName, readerRoles);
   }

   @Nested
   @DisplayName("findAllUsers()")
   class FindAllUsers {

       @Test
       @DisplayName("returns all users from repository")
       void returnsAllUsers() {
           List<User> users = List.of(sampleUser);
           when(userRepository.findAll()).thenReturn(users);

           List<User> result = userService.findAllUsers();

           assertThat(result).containsExactlyElementsOf(users);
           verify(userRepository).findAll();
       }

       @Test
       @DisplayName("returns empty list when no users exist")
       void noUsers_returnsEmpty() {
           when(userRepository.findAll()).thenReturn(List.of());

           List<User> result = userService.findAllUsers();

           assertThat(result).isEmpty();
       }
   }

   @Nested
   @DisplayName("findByUserId()")
   class FindByUserId {

       @Test
       @DisplayName("returns user when found")
       void found_returnsUser() {
           when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));

           User result = userService.findByUserId(1L);

           assertThat(result).isEqualTo(sampleUser);
       }

       @Test
       @DisplayName("throws ResourceNotFoundException when user does not exist")
       void notFound_throwsResourceNotFoundException() {
           when(userRepository.findById(99L)).thenReturn(Optional.empty());

           assertThatThrownBy(() -> userService.findByUserId(99L))
                   .isInstanceOf(ResourceNotFoundException.class)
                   .hasMessageContaining("User not found with id: 99");
       }
   }

   @Nested
   @DisplayName("findByUsername()")
   class FindByUsername {

       @Test
       @DisplayName("returns Optional with user when username exists")
       void found_returnsOptionalUser() {
           when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(sampleUser));

           Optional<User> result = userService.findByUsername("johndoe");

           assertThat(result).isPresent().contains(sampleUser);
       }

       @Test
       @DisplayName("returns empty Optional when username does not exist")
       void notFound_returnsEmptyOptional() {
           when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

           Optional<User> result = userService.findByUsername("ghost");

           assertThat(result).isEmpty();
       }
   }

   @Nested
   @DisplayName("createUser()")
   class CreateUser {

       @Test
       @DisplayName("creates user with hashed password")
       void validUser_createsSuccessfully() {
           when(userRepository.existsByUsername("johndoe")).thenReturn(false);
           when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
           when(userRepository.save(any(User.class))).thenReturn(sampleUser);
           when(passwordEncoder.encode("plainpassword")).thenReturn("hashedpassword");

           User result = userService.createUser(sampleUser);

           assertThat(sampleUser.getPassword()).isEqualTo("hashedpassword");
           assertThat(result).isEqualTo(sampleUser);
           verify(userRepository).save(sampleUser);
       }

       @Test
       @DisplayName("throws ValidationException when username already exists")
       void duplicateUsername_throwsValidationException() {
           when(userRepository.existsByUsername("johndoe")).thenReturn(true);

           assertThatThrownBy(() -> userService.createUser(sampleUser))
                   .isInstanceOf(ValidationException.class)
                   .hasMessageContaining("Username already exists");

           verify(userRepository, never()).save(any());
       }

       @Test
       @DisplayName("throws ValidationException when email already exists")
       void duplicateEmail_throwsValidationException() {
           when(userRepository.existsByUsername("johndoe")).thenReturn(false);
           when(userRepository.existsByEmail("john@example.com")).thenReturn(true);

           assertThatThrownBy(() -> userService.createUser(sampleUser))
                   .isInstanceOf(ValidationException.class)
                   .hasMessageContaining("Email already exists");

           verify(userRepository, never()).save(any());
       }

       @Test
       @DisplayName("does not hash password when password is blank")
       void blankPassword_skipsHashing() {
           sampleUser.setPassword("   ");
           when(userRepository.existsByUsername("johndoe")).thenReturn(false);
           when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
           when(userRepository.save(any(User.class))).thenReturn(sampleUser);

           try (MockedStatic<PasswordHasher> hasherMock = mockStatic(PasswordHasher.class)) {
               userService.createUser(sampleUser);
               hasherMock.verifyNoInteractions();
           }
       }

       @Test
       @DisplayName("does not hash password when password is null")
       void nullPassword_skipsHashing() {
           sampleUser.setPassword(null);
           when(userRepository.existsByUsername("johndoe")).thenReturn(false);
           when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
           when(userRepository.save(any(User.class))).thenReturn(sampleUser);

           try (MockedStatic<PasswordHasher> hasherMock = mockStatic(PasswordHasher.class)) {
               userService.createUser(sampleUser);
               hasherMock.verifyNoInteractions();
           }
       }
   }


   @Nested
   @DisplayName("updateUser()")
   class UpdateUser {

       @Test
       @DisplayName("updates username when provided")
       void updatesUsername() {
           User updatedData = createTestUser(
                   "newname",
                   "john@example.com",
                   "plainpassword",
                   "John",
                   "Doe"
           );
           updatedData.setUsername("newname");

           when(userRepository.findById(1)).thenReturn(Optional.of(sampleUser));
           when(userRepository.save(any(User.class))).thenReturn(sampleUser);

           userService.updateUser(1, updatedData);

           assertThat(sampleUser.getUsername()).isEqualTo("newname");
       }

       @Test
       @DisplayName("updates email when new email is different and not already taken")
       void updatesEmail_whenNewAndAvailable() {
           User updatedData = createTestUser(
                   "johndoe",
                   "newemail@example.com",
                   "plainpassword",
                   "John",
                   "Doe"
           );
           updatedData.setEmail("newemail@example.com");

           when(userRepository.findById(1)).thenReturn(Optional.of(sampleUser));
           when(userRepository.existsByEmail("newemail@example.com")).thenReturn(false);
           when(userRepository.save(any(User.class))).thenReturn(sampleUser);

           userService.updateUser(1, updatedData);

           assertThat(sampleUser.getEmail()).isEqualTo("newemail@example.com");
       }

       @Test
       @DisplayName("throws ValidationException when new email is already in use")
       void emailInUse_throwsValidationException() {
           User updatedData = createTestUser(
                   "johndoe",
                   "taken@example.com",
                   "plainpassword",
                   "John",
                   "Doe"
           );
           updatedData.setEmail("taken@example.com");

           when(userRepository.findById(1)).thenReturn(Optional.of(sampleUser));
           when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

           assertThatThrownBy(() -> userService.updateUser(1, updatedData))
                   .isInstanceOf(ValidationException.class)
                   .hasMessageContaining("Email already in use");
       }

       @Test
       @DisplayName("does not update email when it is the same as existing")
       void sameEmail_notUpdated() {
           User updatedData = User.registerReader(
                   "johndoe",
                   "john@example.com",
                   "plainpassword",
                   "John",
                   "Doe",
                   readerRoles
           );

           when(userRepository.findById(1)).thenReturn(Optional.of(sampleUser));
           when(userRepository.save(any(User.class))).thenReturn(sampleUser);

           userService.updateUser(1, updatedData);

           verify(userRepository, never()).existsByEmail(anyString());
           assertThat(sampleUser.getEmail()).isEqualTo("john@example.com");
       }
       @Test
       @DisplayName("hashes and updates password when provided")
       void updatesHashedPassword() {
           User updatedData = createTestUser(
                   "johndoe",
                   "john@example.com",
                   "newpassword",
                   "John",
                   "Doe"
           );
           updatedData.setPassword("newpassword");

           when(userRepository.findById(1)).thenReturn(Optional.of(sampleUser));
           when(userRepository.save(any(User.class))).thenReturn(sampleUser);
           when(passwordEncoder.encode("newpassword")).thenReturn("newhashedpw");

           userService.updateUser(1, updatedData);

           assertThat(sampleUser.getPassword()).isEqualTo("newhashedpw");
       }

       @Test
       @DisplayName("throws ResourceNotFoundException when user does not exist")
       void userNotFound_throwsResourceNotFoundException() {
           when(userRepository.findById(99)).thenReturn(Optional.empty());

           User updatedData = createTestUser(
                   "ghost",
                   "ghost@example.com",
                   "password",
                   "Ghost",
                   "User"
           );

           assertThatThrownBy(() -> userService.updateUser(99, updatedData))
                   .isInstanceOf(ResourceNotFoundException.class)
                   .hasMessageContaining("User not found with id: 99");
       }

       @Test
       @DisplayName("does not update username when updatedUser username is null")
       void nullUsername_retainsExistingUsername() {
           User updatedData = createTestUser(
                   "johndoe",
                   "john@example.com",
                   "plainpassword",
                   "John",
                   "Doe"
           );
           updatedData.setUsername(null);

           when(userRepository.findById(1)).thenReturn(Optional.of(sampleUser));
           when(userRepository.save(any(User.class))).thenReturn(sampleUser);

           userService.updateUser(1, updatedData);

           assertThat(sampleUser.getUsername()).isEqualTo("johndoe");
       }
   }

   @Nested
   @DisplayName("deleteUser()")
   class DeleteUser {

       @Test
       @DisplayName("deletes user when found")
       void found_deletesSuccessfully() {
           when(userRepository.existsById(1)).thenReturn(true);

           userService.deleteUser(1);

           verify(userRepository).deleteById(1);
       }

       @Test
       @DisplayName("throws ResourceNotFoundException when user does not exist")
       void notFound_throwsResourceNotFoundException() {
           when(userRepository.existsById(99)).thenReturn(false);

           assertThatThrownBy(() -> userService.deleteUser(99))
                   .isInstanceOf(ResourceNotFoundException.class)
                   .hasMessageContaining("User not found with id: 99");

           verify(userRepository, never()).deleteById(anyInt());
       }
   }

   @Nested
   @DisplayName("authenticate()")
   class Authenticate {

       @Test
       @DisplayName("returns user when credentials are valid")
       void validCredentials_returnsUser() {
           sampleUser.setPassword("hashedpassword");
           when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(sampleUser));
           when(passwordEncoder.matches("rawpassword", "hashedpassword")).thenReturn(true);

           try (MockedStatic<UserValidator> validatorMock = mockStatic(UserValidator.class)) {
               validatorMock.when(() -> UserValidator.validateCredentials("johndoe", "rawpassword"))
                       .thenAnswer(inv -> null);

               User result = userService.authenticate("johndoe", "rawpassword");

               assertThat(result).isEqualTo(sampleUser);
           }
       }

       @Test
       @DisplayName("throws ValidationException when password is wrong")
       void wrongPassword_throwsValidationException() {
           sampleUser.setPassword("hashedpassword");
           when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(sampleUser));
           when(passwordEncoder.matches("wrongpw", "hashedpassword")).thenReturn(false);

           try (MockedStatic<UserValidator> validatorMock = mockStatic(UserValidator.class)) {
               validatorMock.when(() -> UserValidator.validateCredentials("johndoe", "wrongpw"))
                       .thenAnswer(inv -> null);

               assertThatThrownBy(() -> userService.authenticate("johndoe", "wrongpw"))
                       .isInstanceOf(ValidationException.class)
                       .hasMessageContaining("Invalid credentials");
           }
       }

       @Test
       @DisplayName("throws ResourceNotFoundException when username does not exist")
       void unknownUser_throwsResourceNotFoundException() {
           when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

           try (MockedStatic<UserValidator> validatorMock = mockStatic(UserValidator.class)) {
               validatorMock.when(() -> UserValidator.validateCredentials("ghost", "pass"))
                       .thenAnswer(inv -> null);

               assertThatThrownBy(() -> userService.authenticate("ghost", "pass"))
                       .isInstanceOf(ResourceNotFoundException.class)
                       .hasMessageContaining("User not found with username: ghost");
           }
       }
   }
}