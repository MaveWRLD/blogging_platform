package org.amalitech.config;

import org.amalitech.dao.*;
import org.amalitech.interfaces.*;
import org.amalitech.service.*;
import org.amalitech.util.PasswordHasher;
import org.amalitech.util.UserValidator;

/**
 * Centralized service container for dependency injection.
 * Manages creation and initialization of all services and repositories.
 *
 * This follows the Service Locator pattern to provide a single point
 * for configuring all application dependencies.
 */
public class ServiceContainer {

    private static ServiceContainer instance;

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final TagRepository tagRepository;
    private final PostTagRepository postTagRepository;

    private final UserService userService;
    private final PostService postService;
    private final PostSearchService postSearchService;
    private final PostTagService postTagService;
    private final TagService tagService;
    private final CommentService commentService;
    private final PostCacheService postCacheService;

    private final UserValidator userValidator;
    private final PasswordHasher passwordHasher;
    private final AppConfig appConfig;


    private ServiceContainer() {
        this.appConfig = new PropertiesConfig();

        this.userValidator = new UserValidator();
        this.passwordHasher = new PasswordHasher();

        PostDao postDao = new PostDao();
        UserDao userDao = new UserDao();
        TagDao tagDao = new TagDao();
        PostTagDao postTagDao = new PostTagDao();

        this.postRepository = postDao;
        this.userRepository = userDao;
        this.tagRepository = tagDao;
        this.postTagRepository = postTagDao;
        
        this.commentRepository = new MongoCommentDao();

        this.postCacheService = new PostCacheService(appConfig);
        this.postTagService = new PostTagService(postTagDao);
        this.tagService = new TagService(tagDao);

        this.userService = new UserService(userDao, userValidator, passwordHasher);

        this.postSearchService = new PostSearchService(postRepository, new SearchCache());
        this.postService = new PostService(postRepository, new PostValidator(), postTagService);

        this.commentService = new CommentService(commentRepository);
    }

    public static synchronized ServiceContainer getInstance() {
        if (instance == null) {
            instance = new ServiceContainer();
        }
        return instance;
    }




    public UserService getUserService() {
        return userService;
    }

    public PostService getPostService() {
        return postService;
    }

    public PostSearchService getPostSearchService() {
        return postSearchService;
    }

    public PostTagService getPostTagService() {
        return postTagService;
    }

    public TagService getTagService() {
        return tagService;
    }

    public CommentService getCommentService() {
        return commentService;
    }

    public PostRepository getPostRepository() {
        return postRepository;
    }

    public UserRepository getUserRepository() {
        return userRepository;
    }

    public TagRepository getTagRepository() {
        return tagRepository;
    }

    public PostTagRepository getPostTagRepository() {
        return postTagRepository;
    }

    public CommentRepository getCommentRepository() {
        return commentRepository;
    }
}

