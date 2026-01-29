package org.amalitech.ui.controller;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.amalitech.config.ServiceContainer;
import org.amalitech.models.Post;
import org.amalitech.models.Tag;
import org.amalitech.service.PostService;
import org.amalitech.service.PostTagService;
import org.amalitech.service.TagService;
import org.amalitech.ui.util.AppExecutors;
import org.amalitech.ui.util.DbTask;

import java.util.ArrayList;
import java.util.List;

public class PostEditorController {

    @FXML
    private TextField titleField;

    @FXML
    private TextArea bodyField;

    @FXML
    private ListView<Tag> tagsList;

    @FXML
    private Button saveButton;

    @FXML
    private Button cancelButton;

    private final PostService postService;
    private final PostTagService postTagService;
    private final TagService tagService;

    public PostEditorController() {
        ServiceContainer container = ServiceContainer.getInstance();
        this.postService = container.getPostService();
        this.postTagService = container.getPostTagService();
        this.tagService = container.getTagService();
    }

    private Integer editingPostId = null;

    @FXML
    private void initialize() {
        loadTags();
    }

    public void loadPost(Integer postId) {
        this.editingPostId = postId;
        if (postId == null) return;
        DbTask<Post> task = new DbTask<>(() -> postService.getPostById(postId));
        task.setOnSucceeded(e -> {
            Post p = task.getValue();
            titleField.setText(p.getTitle());
            bodyField.setText(p.getBody());
            try {
                List<Integer> tagIds = postTagService.getTagsForPost(p.getId());
                selectTagsWhenReady(tagIds);
            } catch (Exception ignore) {}
        });
        AppExecutors.db().submit(task);
    }

    private void loadTags() {
        DbTask<List<Tag>> task = new DbTask<>(tagService::getAllTags);
        task.setOnSucceeded(e -> {
            List<Tag> tags = task.getValue();
            ObservableList<Tag> obs = FXCollections.observableArrayList(tags);
            tagsList.setItems(obs);
            tagsList.getSelectionModel().setSelectionMode(javafx.scene.control.SelectionMode.MULTIPLE);
        });
        AppExecutors.db().submit(task);
    }

    private void selectTagsWhenReady(List<Integer> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) return;
        ObservableList<Tag> items = tagsList.getItems();
        if (items != null && !items.isEmpty()) {
            for (int i = 0; i < items.size(); i++) {
                if (tagIds.contains(items.get(i).getId())) tagsList.getSelectionModel().select(i);
            }
            return;
        }

        tagsList.getItems().addListener((javafx.collections.ListChangeListener<Tag>) change -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    for (Tag t : change.getAddedSubList()) {
                        if (tagIds.contains(t.getId())) tagsList.getSelectionModel().select(t);
                    }
                }
            }
        });
    }

    @FXML
    protected void onSave(ActionEvent event) {
        String title = titleField.getText() == null ? "" : titleField.getText().trim();
        String body = bodyField.getText() == null ? "" : bodyField.getText().trim();
        List<Tag> selectedTags = new ArrayList<>(tagsList.getSelectionModel().getSelectedItems());

        if (title.isEmpty() || body.isEmpty()) {
            javafx.scene.control.Alert a = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
            a.setTitle("Validation error");
            a.setHeaderText(null);
            a.setContentText("Title and body are required");
            a.showAndWait();
            return;
        }

        DbTask<Void> task = new DbTask<>(() -> {
            List<Integer> tagIds = new ArrayList<>();
            for (Tag t : selectedTags) tagIds.add(t.getId());

            if (editingPostId == null) {
                int userId = 1;
                try {
                    org.amalitech.models.User cu = org.amalitech.ui.session.SessionContext.getCurrentUser();
                    if (cu != null) userId = cu.getId();
                } catch (Exception ignore) {}

                Post p = new Post(title, body, userId, "DRAFT");
                postService.createPost(p, tagIds);  // Now returns the created post with ID
            } else {
                Post p = postService.getPostById(editingPostId);
                p.setTitle(title);
                p.setBody(body);
                postService.updatePost(p, tagIds);
            }
            return null;
        });

        task.setOnSucceeded(e -> {
            Platform.runLater(() -> {
                Stage stage = (Stage) saveButton.getScene().getWindow();
                stage.close();
            });
        });

        AppExecutors.db().submit(task);
    }

    @FXML
    protected void onCancel(ActionEvent event) {
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }
}
