package org.amalitech.ui.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.amalitech.config.ServiceContainer;
import org.amalitech.models.Post;
import org.amalitech.models.SortOrder;
import org.amalitech.service.PostService;
import org.amalitech.service.UserService;
import org.amalitech.ui.util.AppExecutors;
import org.amalitech.ui.util.ControllerUtils;
import org.amalitech.ui.util.DbTask;

import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public class FeedViewController {

    @FXML private ComboBox<String> sortFilter;
    @FXML private ComboBox<String> timeFilter;
    @FXML private VBox postFeed;
    @FXML private VBox loadingIndicator;
    @FXML private VBox endOfFeed;
    @FXML private Label feedTitle;


    private final UserService userService;
    private final PostService postService;

    public FeedViewController() {
        ServiceContainer container = ServiceContainer.getInstance();
        this.userService = container.getUserService();
        this.postService = container.getPostService();
    }

    private int currentPage = 0;
    private final int pageSize = 10;
    private boolean isLoading = false;
    private boolean hasMorePosts = true;

    private Integer selectedTagId = null;
    private SortOrder currentSortOrder = SortOrder.NEWEST;
    private String currentTimeFilter = "All time";




    @FXML
    public void initialize() {

        sortFilter.getItems().addAll("Latest", "Oldest", "Most commented");
        sortFilter.setValue("Latest");
        sortFilter.setOnAction(e -> onSortChanged());

        timeFilter.getItems().addAll("All time", "Today", "This week", "This month");
        timeFilter.setValue("All time");
        timeFilter.setOnAction(e -> onTimeFilterChanged());

        AppExecutors.getDbExecutor().execute(() -> {
            Platform.runLater(() -> {
            });
        });

        loadPosts();

        setupInfiniteScroll();
    }

    @FXML
    private void onFilterAny() {
        refreshFeed();
    }

    @FXML
    private void onFilterShort() {
        refreshFeed();
    }

    @FXML
    private void onFilterMedium() {
        refreshFeed();
    }

    private void onSortChanged() {
        String selectedSort = sortFilter.getValue();
        if ("Latest".equals(selectedSort)) {
            currentSortOrder = SortOrder.NEWEST;
        } else if ("Oldest".equals(selectedSort)) {
            currentSortOrder = SortOrder.OLDEST;
        } else if ("Most commented".equals(selectedSort)) {
            currentSortOrder = SortOrder.MOST_COMMENTED;
        }
        refreshFeed();
    }

    private void onTimeFilterChanged() {
        String selectedTime = timeFilter.getValue();
        if (selectedTime != null) {
            currentTimeFilter = selectedTime;
        }
        refreshFeed();
    }

    private void refreshFeed() {
        currentPage = 0;
        hasMorePosts = true;
        postFeed.getChildren().clear();
        loadPosts();
    }

    private void loadPosts() {
        if (isLoading || !hasMorePosts) return;

        isLoading = true;
        showLoading();

        final int pageToLoad = currentPage;
        final LocalDateTime cutoffDate = getTimeFilterCutoffDate();
        final SortOrder sortOrder = currentSortOrder;
        final Integer tagId = selectedTagId;

        DbTask<List<Post>> task = new DbTask<>(() -> {
            List<Post> posts;
            if (tagId != null) {
                posts = postService.listByTag(tagId, pageToLoad, pageSize, sortOrder, cutoffDate);
            } else {
                posts = postService.getAllPosts(pageToLoad, pageSize, sortOrder, cutoffDate);
            }

            return posts;
        });

        task.setOnSucceeded(event -> {
            List<Post> posts = task.getValue();

            if (posts.isEmpty() || posts.size() < pageSize) {
                hasMorePosts = false;
                showEndOfFeed();
            }

            if (!posts.isEmpty()) {
                renderPosts(posts);
                currentPage++;
            }

            hideLoading();
            isLoading = false;
        });

        task.setOnFailed(event -> {
            hideLoading();
            isLoading = false;
        });

        AppExecutors.getDbExecutor().execute(task);
    }

    private LocalDateTime getTimeFilterCutoffDate() {
        LocalDateTime now = LocalDateTime.now();
        return switch (currentTimeFilter) {
            case "Today" -> now.minusHours(24);
            case "This week" -> now.minusWeeks(1);
            case "This month" -> now.minusMonths(1);
            default -> null; // "All time"
        };
    }

    private void renderPosts(List<Post> posts) {
        for (Post post : posts) {
            VBox postCard = createPostCard(post);
            postFeed.getChildren().add(postCard);
        }
    }

    private VBox createPostCard(Post post) {
        VBox card = new VBox(12);
        card.getStyleClass().add("post-card");
        card.setCursor(javafx.scene.Cursor.HAND);
        card.setOnMouseClicked(e -> onPostClick(post));

        Label title = new Label(post.getTitle());
        title.getStyleClass().add("post-card-title");
        title.setWrapText(true);

        String body = post.getBody() != null ? post.getBody() : "";
        String excerpt = body.length() > 150
            ? body.substring(0, 150) + "..."
            : body;

        Label excerptLabel = new Label(excerpt);
        excerptLabel.getStyleClass().add("post-card-excerpt");
        excerptLabel.setWrapText(true);
        excerptLabel.setMaxWidth(Double.MAX_VALUE);

        HBox meta = new HBox(8);
        meta.setAlignment(Pos.CENTER_LEFT);

        Label author = new Label("User " + userService.findByUserId(post.getUserId()).getUsername());
        author.getStyleClass().add("post-card-meta");

        Label separator1 = new Label("•");
        separator1.getStyleClass().add("post-card-meta");

        Label date = new Label(post.getCreatedAt() != null
            ? post.getCreatedAt().format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))
            : "Today");
        date.getStyleClass().add("post-card-meta");

        Label separator2 = new Label("•");
        separator2.getStyleClass().add("post-card-meta");

        int readTime = ControllerUtils.estimateReadTime(body);
        Label readTimeLabel = new Label(readTime + " min read");
        readTimeLabel.getStyleClass().add("post-card-meta");

        meta.getChildren().addAll(author, separator1, date, separator2, readTimeLabel);

        card.getChildren().addAll(title, excerptLabel, meta);

        return card;
    }

    public void performSearch(String query) {
        if (query == null || query.trim().isEmpty()) {
            clearFilters();
            return;
        }


        if (feedTitle != null) {
            feedTitle.setText("Search results for: \"" + query + "\"");
        }

        currentPage = 0;
        hasMorePosts = true;
        postFeed.getChildren().clear();

        DbTask<List<Post>> task = new DbTask<>(() -> postService.search(
                query.trim(),
                Set.of(),
                Set.of(),
                null,
                SortOrder.NEWEST,
                currentPage,
                pageSize
        ));

        task.setOnSucceeded(event -> {
            List<Post> results = task.getValue();
            if (results.isEmpty()) {
                showNoResultsMessage();
            } else {
                renderPosts(results);
                currentPage++;
            }
            hideLoading();
            isLoading = false;
        });

        task.setOnFailed(event -> {
            System.err.println("Search failed: " + task.getException().getMessage());
            hideLoading();
            isLoading = false;
        });

        showLoading();
        isLoading = true;
        AppExecutors.getDbExecutor().execute(task);
    }

    private void showNoResultsMessage() {
        Label msg = new Label("No posts found for your search");
        msg.getStyleClass().add("hint");
        postFeed.getChildren().add(msg);
    }

    private void onPostClick(Post post) {
        MainLayoutController mainController = ControllerUtils.getMainController(postFeed.getScene());
        if (mainController != null) {
            mainController.loadReadingView(post.getId());
        }
    }

    private void setupInfiniteScroll() {
        postFeed.heightProperty().addListener((obs, oldVal, newVal) -> {
            if (postFeed.getParent() instanceof javafx.scene.control.ScrollPane scrollPane) {

                scrollPane.vvalueProperty().addListener((observable, oldValue, newValue) -> {
                    if (newValue.doubleValue() > 0.8 && !isLoading && hasMorePosts) {
                        loadPosts();
                    }
                });
            }
        });
    }

    private void showLoading() {
        Platform.runLater(() -> {
            loadingIndicator.setManaged(true);
            loadingIndicator.setVisible(true);
        });
    }

    private void hideLoading() {
        Platform.runLater(() -> {
            loadingIndicator.setManaged(false);
            loadingIndicator.setVisible(false);
        });
    }

    private void showEndOfFeed() {
        Platform.runLater(() -> {
            endOfFeed.setManaged(true);
            endOfFeed.setVisible(true);
        });
    }

    public void filterByTag(int tagId, String tagName) {
        this.selectedTagId = tagId;
        if (feedTitle != null) {
            feedTitle.setText("Posts tagged: #" + tagName);
        }
        refreshFeed();
    }

    public void clearFilters() {
        this.selectedTagId = null;
        if (feedTitle != null) {
            feedTitle.setText("Latest Posts");
        }
        refreshFeed();
    }
}
