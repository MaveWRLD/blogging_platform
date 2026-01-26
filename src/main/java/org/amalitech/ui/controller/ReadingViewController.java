package org.amalitech.ui.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import org.amalitech.config.ServiceContainer;
import org.amalitech.service.CommentService;
import org.amalitech.service.PostService;
import org.amalitech.service.PostTagService;
import org.amalitech.service.TagService;
import org.amalitech.ui.session.SessionContext;
import org.amalitech.models.Comment;
import org.amalitech.models.Post;
import org.amalitech.models.Tag;
import org.amalitech.ui.util.AppExecutors;
import org.amalitech.ui.util.ControllerUtils;
import org.amalitech.ui.util.DbTask;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Controller for the reading/post detail view.
 * Uses ServiceContainer for dependency injection.
 * Delegates business logic to services instead of DAOs.
 */
public class ReadingViewController {

    @FXML private ScrollPane contentScroll;
    @FXML private Label postTitle;
    @FXML private Label authorName;
    @FXML private Label publishDate;
    @FXML private Label readingTime;
    @FXML private HBox tagsContainer;
    @FXML private TextFlow articleBody;
    @FXML private Label commentsHeader;
    @FXML private TextArea commentInput;
    @FXML private VBox commentsList;
    @FXML private Button loadMoreComments;
    @FXML private HBox authorActions;

    private final PostService postService;
    private final CommentService commentService;
    private final PostTagService postTagService;
    private final TagService tagService;

    public ReadingViewController() {
        ServiceContainer container = ServiceContainer.getInstance();
        this.postService = container.getPostService();
        this.commentService = container.getCommentService();
        this.postTagService = container.getPostTagService();
        this.tagService = container.getTagService();
    }

    private Post currentPost;
    private boolean hasMoreComments = true;

    @FXML
    public void initialize() {

    }

    public void loadPost(int postId) {
        DbTask<Post> task = new DbTask<>(() -> postService.getPostById(postId));

        task.setOnSucceeded(event -> {
            currentPost = task.getValue();
            displayPost(currentPost);
            loadComments();
        });

        task.setOnFailed(event -> {
            System.err.println("Failed to load post: " + task.getException().getMessage());
            showError("Failed to load post");
        });

        AppExecutors.getDbExecutor().execute(task);
    }

    private void displayPost(Post post) {
        postTitle.setText(post.getTitle());

        // Load author information
        DbTask<String> authorTask = new DbTask<>(() -> {
            try {
                org.amalitech.models.User user = postService.getUserById(post.getUserId());
                return user != null ? user.getUsername() : "Unknown User";
            } catch (Exception e) {
                return "User " + post.getUserId();
            }
        });

        authorTask.setOnSucceeded(e -> {
            String authorNameValue = authorTask.getValue();
            authorName.setText("By " + authorNameValue);
        });
        AppExecutors.getDbExecutor().execute(authorTask);

        publishDate.setText(post.getCreatedAt().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")));
        int readTime = ControllerUtils.estimateReadTime(post.getBody());
        readingTime.setText(readTime + " min read");

        if (SessionContext.isAuthenticated() && SessionContext.getCurrentUser() != null &&
            SessionContext.getCurrentUser().getId() == post.getUserId()) {
            authorActions.setVisible(true);
            authorActions.setManaged(true);
        } else {
            authorActions.setVisible(false);
            authorActions.setManaged(false);
        }

        tagsContainer.getChildren().clear();
        DbTask<List<Tag>> tagsTask = new DbTask<>(() -> {
            List<Integer> tagIds = postTagService.getTagsForPost(post.getId());

            java.util.List<Tag> tags = new java.util.ArrayList<>();
            for (Integer tagId : tagIds) {
                Tag tag = tagService.getTagById(tagId);
                if (tag != null) {
                    tags.add(tag);
                }
            }
            return tags;
        });
        tagsTask.setOnSucceeded(e -> {
            List<Tag> tags = tagsTask.getValue();
            if (tags != null && !tags.isEmpty()) {
                for (Tag tag : tags) {
                    Button tagBtn = new Button("#" + tag.getName());
                    tagBtn.getStyleClass().add("tag-pill");
                    tagBtn.setOnAction(event -> {
                        MainLayoutController main = ControllerUtils.getMainController(tagsContainer.getScene());
                        if (main != null) {
                            main.onTagClick(tag);
                        }
                    });
                    tagsContainer.getChildren().add(tagBtn);
                }
                tagsContainer.setManaged(true);
                tagsContainer.setVisible(true);
            } else {
                tagsContainer.setManaged(false);
                tagsContainer.setVisible(false);
            }
        });
        AppExecutors.getDbExecutor().execute(tagsTask);

        articleBody.getChildren().clear();

        String[] paragraphs = post.getBody().split("\n\n");
        for (String paragraph : paragraphs) {
            Text text = new Text(paragraph + "\n\n");
            text.getStyleClass().add("body-large");
            articleBody.getChildren().add(text);
        }

    }

    @FXML
    private void onBack() {
        MainLayoutController mainController = ControllerUtils.getMainController(contentScroll.getScene());
        if (mainController != null) {
            mainController.onHome();
        }
    }

    @FXML
    private void onEditPost() {
        MainLayoutController main = ControllerUtils.getMainController(contentScroll.getScene());
        if (main != null) {
            main.onEdit(currentPost.getId());
        }
    }

    @FXML
    private void onDeletePost() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Post");
        alert.setHeaderText("Are you sure you want to delete this post?");
        alert.setContentText("This action cannot be undone.");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                DbTask<Void> deleteExtra = new DbTask<>(() -> {
                    postService.deletePost(currentPost.getId());
                    return null;
                });
                deleteExtra.setOnSucceeded(e -> onBack());
                deleteExtra.setOnFailed(e -> {
                    e.getSource().getException().printStackTrace();
                    showError("Failed to delete post: " + e.getSource().getException().getMessage());
                });
                AppExecutors.getDbExecutor().execute(deleteExtra);
            }
        });
    }

    @FXML
    private void onSortTop() {
        commentsList.getChildren().clear();
        loadComments();
    }

    @FXML
    private void onSortNewest() {
        commentsList.getChildren().clear();
        loadComments();
    }

    @FXML
    private void onPostComment() {
        String commentText = commentInput.getText().trim();
        if (commentText.isEmpty()) {
            showError("Comment cannot be empty");
            return;
        }

        Comment comment = new Comment();
        comment.setPostId(currentPost.getId());
        comment.setUserName(SessionContext.getCurrentUser().getUsername());
        comment.setBody(commentText);

        DbTask<Void> task = new DbTask<>(() -> {
            commentService.createComment(comment);
            return null;
        });

        task.setOnSucceeded(event -> {
            commentInput.clear();
            commentsList.getChildren().clear();
            hasMoreComments = true;
            loadComments();
        });

        task.setOnFailed(event -> {
            System.err.println("Failed to post comment: " + task.getException().getMessage());
            showError("Failed to post comment");
        });

        AppExecutors.getDbExecutor().execute(task);
    }

    @FXML
    private void onCancelComment() {
        commentInput.clear();
    }

    @FXML
    private void onLoadMoreComments() {
        loadComments();
    }

    private void loadComments() {
        if (!hasMoreComments) {
            return;
        }

        DbTask<List<Comment>> task = new DbTask<>(() ->
            commentService.getCommentsByPostId(currentPost.getId())
        );

        task.setOnSucceeded(event -> {
            List<Comment> comments = task.getValue();

            commentsHeader.setText(String.format("Comments (%d)", comments.size()));

            if (comments.isEmpty()) {
                hasMoreComments = false;
                loadMoreComments.setManaged(false);
                loadMoreComments.setVisible(false);

                if (commentsList.getChildren().isEmpty()) {
                    Label noComments = new Label("No comments yet. Be the first to comment!");
                    noComments.getStyleClass().add("hint");
                    commentsList.getChildren().add(noComments);
                }
            } else {
                renderComments(comments);
            }
        });

        task.setOnFailed(event -> {
            System.err.println("Failed to load comments: " + task.getException().getMessage());
        });

        AppExecutors.getDbExecutor().execute(task);
    }

    private void renderComments(List<Comment> comments) {
        for (Comment comment : comments) {
            VBox commentCard = createCommentCard(comment);
            commentsList.getChildren().add(commentCard);
        }
    }

    private VBox createCommentCard(Comment comment) {
        VBox card = new VBox(8);
        card.getStyleClass().add("comment-card");

        HBox header = new HBox(8);
        Label author = new Label(comment.getUserName());
        author.getStyleClass().add("comment-author");

        Label separator = new Label("•");
        separator.getStyleClass().add("caption");

        String dateText = "Unknown date";
        if (comment.getCreatedAt() != null) {
            dateText = comment.getCreatedAt().format(
                DateTimeFormatter.ofPattern("MMM dd, yyyy"));
        }
        Label date = new Label(dateText);
        date.getStyleClass().add("caption");

        header.getChildren().addAll(author, separator, date);

        Label commentText = new Label(comment.getBody());
        commentText.getStyleClass().add("comment-text");
        commentText.setWrapText(true);

        card.getChildren().addAll(header, commentText);
        return card;
    }

    private void showError(String message) {
        ControllerUtils.showError(message);
    }
}
