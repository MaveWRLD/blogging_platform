package org.amalitech.service;

import org.amalitech.interfaces.RoleRepository;
import org.amalitech.interfaces.UserRepository;
import org.amalitech.interfaces.UserRoleRepository;
import org.amalitech.models.User;
import org.amalitech.util.UserValidator;
import org.amalitech.util.PasswordHasher;
import org.amalitech.util.exception.ResourceNotFoundException;
import org.amalitech.util.exception.ValidationException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;


@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;

    public UserService(UserRepository userRepository, UserRoleRepository userRoleRepository, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
        this.roleRepository = roleRepository;
    }

    public void createUser(User user) {
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            user.setPassword(PasswordHasher.hash(user.getPassword()));
        }
        int userId = userRepository.save(user);
        int roleId = roleRepository.findByName("reader").getId();
        if (userId != 0)
            userRoleRepository.assignRoleToUser(userId, roleId);
    }

    public List<User> findAllUsers() {
        return userRepository.findAll();

    }

    public User findByUserId(int id) {
        return userRepository.findByUserId(id).orElseThrow(
                () -> new ResourceNotFoundException("User not found with id: " + id)
        );
    }

    public void updateUser(User user) {
        if (user.getPassword() != null && !user.getPassword().isBlank()) {
            user.setPassword(PasswordHasher.hash(user.getPassword()));
        }

        userRepository.update(user);
    }

    public void deleteUser(int userId) {
        Optional<User> user = userRepository.findByUserId(userId);
        if (user.isEmpty()) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }
        userRepository.delete(userId);
    }

    public boolean authenticate(String username, String password) {
        UserValidator.validateCredentials(username, password);

        User user = userRepository.findByUsername(username).orElseThrow(
                () -> new ResourceNotFoundException("User not found with username: " + username)
        );

        boolean matches = PasswordHasher.check(password, user.getPassword());

        if (!matches) {
            throw new ValidationException("Invalid credentials");
        }

        return true;
    }
}
