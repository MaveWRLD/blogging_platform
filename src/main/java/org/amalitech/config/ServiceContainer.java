package org.amalitech.config;

import lombok.Getter;
import lombok.Setter;
import org.amalitech.dao.*;
import org.amalitech.interfaces.*;
import org.amalitech.service.*;
import org.amalitech.util.PasswordHasher;
import org.amalitech.util.PostValidator;
import org.amalitech.util.UserValidator;

/**
 * Centralized service container for dependency injection.
 * Manages creation and initialization of all services and repositories.
 * This follows the Service Locator pattern to provide a single point
 * for configuring all application dependencies.
 */
@Getter
@Setter
public class ServiceContainer {

    private static ServiceContainer instance;


    //private final UserRepository userRepository;
  //  private final TagRepository tagRepository;
    //private final PostTagRepository postTagRepository;

    //private final UserService userService;
    //private final PostService postService;
    //private final PostTagService postTagService;
   // private final TagService tagService;

    private final AppConfig appConfig;


    private ServiceContainer() {
        this.appConfig = new PropertiesConfig();

        //UserDao userDao = new UserDao();
        //TagDao tagDao = new TagDao();
        //PostTagDao postTagDao = new PostTagDao();

        //this.userRepository = userDao;
        //this.tagRepository = tagDao;
        //this.postTagRepository = postTagDao;
        

        //this.postTagService = new PostTagService(postTagDao);
        //this.tagService = new TagService(tagDao);

        //this.userService = new UserService(userDao);

        //this.postService = new PostService(postRepository, postTagService);

    }

    public static synchronized ServiceContainer getInstance() {
        if (instance == null) {
            instance = new ServiceContainer();
        }
        return instance;
    }
}