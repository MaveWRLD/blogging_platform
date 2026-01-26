package org.amalitech.service;

import org.amalitech.dao.UserDao;
import org.amalitech.models.User;
import org.amalitech.util.UserValidator;
import org.amalitech.util.PasswordHasher;

public class UserService {

    private final UserDao userDao;
    private final UserValidator userValidator;
    private final PasswordHasher passwordHasher;



    public UserService(UserDao userDao, UserValidator userValidator, PasswordHasher passwordHasher) {
        this.userDao = userDao;
        this.userValidator = userValidator;
        this.passwordHasher = passwordHasher;
    }


    public void createUser(User user) {
        userValidator.validate(user);
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            user.setPassword(passwordHasher.hash(user.getPassword()));
        }
        userDao.save(user);
    }

    public User findByUserId(int id) {
        return userDao.findByUserId(id);
    }

    public User authenticate(String username, String password) {
        userValidator.validateCredentials(username, password);

        User user = userDao.findByUsername(username);
        if (user == null || user.getPassword() == null) return null;

        boolean matches = passwordHasher.check(password, user.getPassword());
        return matches ? user : null;
    }
}
