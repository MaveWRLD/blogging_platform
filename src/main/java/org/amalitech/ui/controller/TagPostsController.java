package org.amalitech.ui.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Pagination;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.amalitech.config.ServiceContainer;
import org.amalitech.models.Post;
import org.amalitech.models.Tag;
import org.amalitech.service.PostService;
import org.amalitech.service.TagService;
import org.amalitech.ui.util.AppExecutors;
import org.amalitech.ui.util.AlertHelper;
import org.amalitech.ui.util.DbTask;

import java.util.List;

public class TagPostsController {

    private final PostService postService;
    private final TagService tagService;

    public TagPostsController() {
        ServiceContainer container = ServiceContainer.getInstance();
        this.postService = container.getPostService();
        this.tagService = container.getTagService();
    }

    private int tagId;
    private int pageSize = 10;

    @FXML private Label titleLabel;
    @FXML private Pagination pagination;
    @FXML private ListView<Post> postsTable;

    public void loadTag(int tagId) {
        this.tagId = tagId;

        Tag t = tagService.getTagById(tagId);
        String tagName = t != null ? t.getName() : "#" + tagId;
        titleLabel.setText("Posts tagged: " + tagName);

        int total = postService.countByTag(tagId);
        int pages = Math.max(1, (int) Math.ceil((double) total / pageSize));
        pagination.setPageCount(pages);
        pagination.setCurrentPageIndex(0);

        pagination.setPageFactory(this::createPage);

        postsTable.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Post post, boolean empty) {
                super.updateItem(post, empty);
                if (empty || post == null) {
                    setGraphic(null);
                } else {
                    VBox card = new VBox(5);
                    Label title = new Label(post.getTitle());
                    title.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");
                    String body = post.getBody() != null ? post.getBody() : "";
                    Label snippet = new Label(body.length() > 150 ? body.substring(0, 150) + "..." : body);
                    HBox meta = new HBox(10);
                    Label author = new Label("By User " + post.getUserId());
                    Label status = new Label(post.getStatus());
                    Label date = new Label(post.getCreatedAt() != null ? post.getCreatedAt().toString() : "");
                    meta.getChildren().addAll(author, status, date);
                    card.getChildren().addAll(title, snippet, meta);
                    setGraphic(card);
                }
            }
        });

        postsTable.setOnMouseClicked(this::onTableClick);

    }

    private Node createPage(int pageIndex) {
        loadPage(pageIndex);
        return postsTable;
    }

    private void loadPage(int pageIndex) {
        if (tagId <= 0) {
            postsTable.setItems(FXCollections.emptyObservableList());
            return;
        }

        DbTask<List<Post>> task = new DbTask<>(() ->
                postService.listByTag(tagId, pageIndex, pageSize)
        );

        task.setOnSucceeded(e -> {
            List<Post> posts = task.getValue();
            postsTable.setItems(FXCollections.observableArrayList(posts));
        });

        task.setOnFailed(e -> {
            AlertHelper.showError(task.getException(), "Load Error",
                    "Failed to load posts for tag #" + tagId);
            postsTable.setItems(FXCollections.emptyObservableList());
        });

        AppExecutors.db().submit(task);
    }

    private void onTableClick(MouseEvent event) {
        if (event.getClickCount() == 2) {
            Post selected = postsTable.getSelectionModel().getSelectedItem();
        }
    }

    @FXML
    private void onBack() {
        Stage stage = (Stage) titleLabel.getScene().getWindow();
        if (stage != null) {
            stage.close();
        }
    }
}

