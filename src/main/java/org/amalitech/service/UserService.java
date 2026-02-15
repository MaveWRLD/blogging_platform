package org.amalitech.service;

import org.amalitech.entities.Role;
import org.amalitech.entities.User;
import org.amalitech.repositories.PostRepository;
import org.amalitech.repositories.RoleRepository;
import org.amalitech.repositories.UserRepository;
import org.amalitech.util.PasswordHasher;
import org.amalitech.util.UserValidator;
import org.amalitech.exception.ResourceNotFoundException;
import org.amalitech.exception.ValidationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PostRepository postRepository;

    public UserService(UserRepository userRepository, RoleRepository roleRepository, PostRepository postRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.postRepository = postRepository;
    }


    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public List<User> findAllUsers() {
        return userRepository.findAll();
    }

    @Transactional(readOnly = true)
    public User findByUserId(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }


    @Transactional
    public User createUser(User user) {
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new ValidationException("Username already exists");
        }
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new ValidationException("Email already exists");
        }

        if (user.getPassword() != null && !user.getPassword().isBlank()) {
            user.setPassword(PasswordHasher.hash(user.getPassword()));
        }

        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new ResourceNotFoundException("Default role not found: ROLE_USER"));

        user.setRoles(Set.of(userRole));

        return userRepository.save(user);
    }

    @Transactional
    public User updateUser(int id, User updatedUser) {
        User existing = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        if (updatedUser.getUsername() != null) {
            existing.setUsername(updatedUser.getUsername());
        }
        if (updatedUser.getEmail() != null && !updatedUser.getEmail().equals(existing.getEmail())) {
            if (userRepository.existsByEmail(updatedUser.getEmail())) {
                throw new ValidationException("Email already in use");
            }
            existing.setEmail(updatedUser.getEmail());
        }

        if (updatedUser.getPassword() != null && !updatedUser.getPassword().isBlank()) {
            existing.setPassword(PasswordHasher.hash(updatedUser.getPassword()));
        }

        return userRepository.save(existing);
    }

    @Transactional
    public void deleteUser(int id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User not found with id: " + id);
        }
        userRepository.deleteById(id);
    }

    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public User authenticate(String username, String password) {
        UserValidator.validateCredentials(username, password);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));

        if (!PasswordHasher.check(password, user.getPassword())) {
            throw new ValidationException("Invalid credentials");
        }

        return user;
    }

    @Transactional
    public User assignRole(Long id, String roleName) {
        User user = findByUserId(id);
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + roleName));

        user.setRoles(Set.of(role));
        return userRepository.save(user);
    }
}