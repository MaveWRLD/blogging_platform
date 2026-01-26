package org.amalitech.ui.controller;


import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.amalitech.config.ServiceContainer;
import org.amalitech.models.Tag;
import org.amalitech.service.TagService;
import org.amalitech.ui.util.AppExecutors;
import org.amalitech.ui.util.DbTask;
import org.amalitech.ui.session.SessionContext;

import java.io.IOException;
import java.util.List;


public class MainLayoutController {

    @FXML private TextField searchField;
    @FXML private VBox tagsList;
    @FXML private StackPane contentArea;

    private final TagService tagService;
    private String currentView = "feed";
    private FeedViewController feedViewController;

    public MainLayoutController() {
        this.tagService = ServiceContainer.getInstance().getTagService();
    }

    @FXML
    public void initialize() {
        loadTags();

        loadFeedView();

        searchField.setOnKeyPressed(event -> {
            if (event.isControlDown() && event.getCode().toString().equals("K")) {
                searchField.requestFocus();
            }
        });

        Platform.runLater(() -> contentArea.getScene().getRoot().setUserData(this));
    }

    public void loadPerformanceMetrics() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/performance_test.fxml")
            );
            Parent root = loader.load();

            PerformanceTestController controller = loader.getController();
            controller.initialize();

            contentArea.getChildren().setAll(root);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onPerformanceMetrics() {
        loadPerformanceMetrics();
    }

    @FXML
    private void onSearch() {
        String query = searchField.getText().trim();
        if (query.isEmpty()) {
            return;
        }

        if (!"feed".equals(currentView) || feedViewController == null) {
            loadFeedView();
            currentView = "feed";
        }

        if (feedViewController != null) {
            feedViewController.performSearch(query);
        } else {
            System.err.println("Cannot search — feed controller not loaded");
        }
    }

    @FXML
    private void onWrite() {
        onEdit(-1);
    }

    public void onEdit(int postId) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/editor_view.fxml"));
            if (loader.getLocation() == null) {
                throw new IOException("FXML file not found: /fxml/editor_view.fxml");
            }

            Parent editorRoot = loader.load();

            EditorViewController controller = loader.getController();
            if (postId > 0) {
                DbTask<org.amalitech.models.Post> task = new DbTask<>(() -> new org.amalitech.dao.PostDao().findById(postId));
                task.setOnSucceeded(e -> controller.loadPost(task.getValue()));
                AppExecutors.getDbExecutor().execute(task);
            } else {
                controller.loadPost(null);
            }

            contentArea.getChildren().setAll(editorRoot);
            currentView = "editor";
        } catch (IOException e) {
            e.printStackTrace();
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Navigation Error");
                alert.setHeaderText("Cannot open editor");
                alert.setContentText("Failed to load editor view:\n" + e.getMessage());
                alert.showAndWait();
            });
        }
    }


    @FXML
    void onHome() {
        if (feedViewController != null) {
            feedViewController.clearFilters();
        }
        loadFeedView();
        currentView = "feed";
    }

    @FXML
    private void onLogout() {
        SessionContext.clear();
        loadLoginView();
    }

    private void loadTags() {
        DbTask<List<Tag>> task = new DbTask<>(tagService::getAllTags);

        task.setOnSucceeded(event -> {
            List<Tag> tags = task.getValue();
            tagsList.getChildren().clear();

            for (Tag tag : tags) {
                Button tagButton = new Button("# " + tag.getName());
                tagButton.getStyleClass().add("sidebar-button-subtle");
                tagButton.setMaxWidth(Double.MAX_VALUE);
                tagButton.setOnAction(e -> onTagClick(tag));
                tagsList.getChildren().add(tagButton);
            }
        });

        task.setOnFailed(event -> System.err.println("Failed to load tags: " + task.getException().getMessage()));

        AppExecutors.getDbExecutor().execute(task);
    }

    public void onTagClick(Tag tag) {
        if (!"feed".equals(currentView) || feedViewController == null) {
            loadFeedView();
            currentView = "feed";
        }

        if (feedViewController != null) {
            feedViewController.filterByTag(tag.getId(), tag.getName());
        }
    }

    private void loadFeedView() {
        loadView("/fxml/feed_view.fxml");
    }

    private void loadView(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent view = loader.load();
            
            Object controller = loader.getController();
            if (controller instanceof FeedViewController) {
                this.feedViewController = (FeedViewController) controller;
            }

            contentArea.getChildren().clear();
            contentArea.getChildren().add(view);
        } catch (IOException e) {
            System.err.println("Failed to load view: " + fxmlPath);
            e.printStackTrace();
        }
    }

    private void loadLoginView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Parent loginView = loader.load();
            contentArea.getScene().setRoot(loginView);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadReadingView(int postId) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/reading_view.fxml"));
            Parent view = loader.load();
            
            ReadingViewController controller = loader.getController();
            controller.loadPost(postId);
            
            contentArea.getChildren().clear();
            contentArea.getChildren().add(view);
            currentView = "reading";
        } catch (IOException e) {
            System.err.println("Failed to load reading view");
            e.printStackTrace();
        }
    }
}
