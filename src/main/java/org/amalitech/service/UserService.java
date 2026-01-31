package org.amalitech.service;

import org.amalitech.interfaces.UserRepository;
import org.amalitech.models.User;
import org.amalitech.util.UserValidator;
import org.amalitech.util.PasswordHasher;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;


    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void createUser(User user) {
        UserValidator.validate(user);
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            user.setPassword(PasswordHasher.hash(user.getPassword()));
        }
        userRepository.save(user);
    }

    public List<User> findByUserId(int id) {
        return userRepository.findByUserId(id);
    }

//    public User authenticate(String username, String password) {
//        UserValidator.validateCredentials(username, password);
//
//        User user = userRepository.findByUsername(username);
//        if (user == null || user.getPassword() == null) return null;
//
//        boolean matches = PasswordHasher.check(password, user.getPassword());
//        return matches ? user : null;
//    }

}
