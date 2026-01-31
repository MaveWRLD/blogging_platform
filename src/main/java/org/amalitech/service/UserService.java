package org.amalitech.service;

import org.amalitech.dao.UserDao;
import org.amalitech.models.User;
import org.amalitech.util.UserValidator;
import org.amalitech.util.PasswordHasher;
import org.springframework.stereotype.Service;

public class UserService {

    private final UserDao userDao;


    public UserService(UserDao userDao) {
        this.userDao = userDao;
    }

    public void createUser(User user) {
        UserValidator.validate(user);
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            user.setPassword(PasswordHasher.hash(user.getPassword()));
        }
        userDao.save(user);
    }

    public User findByUserId(int id) {
        return userDao.findByUserId(id);
    }

    public User authenticate(String username, String password) {
        UserValidator.validateCredentials(username, password);

        User user = userDao.findByUsername(username);
        if (user == null || user.getPassword() == null) return null;

        boolean matches = PasswordHasher.check(password, user.getPassword());
        return matches ? user : null;
    }
}
