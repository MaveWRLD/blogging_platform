package org.amalitech;


import org.amalitech.dao.UserDao;
import org.amalitech.models.User;
import org.amalitech.service.UserService;
import org.amalitech.util.PasswordHasher;
import org.amalitech.util.UserValidator;

public class App{

    public static void main( String[] args ) {

        PasswordHasher passwordHasher = new PasswordHasher();
        UserValidator userValidator = new UserValidator();
        UserDao userDao = new UserDao();

        UserService userService = new UserService(userDao,  userValidator, passwordHasher);

        User user = new User("testuser", "testuser@patch.com", "password123", "AUTHOR", "ACTIVE");
        userService.createUser(user);


    }
}
