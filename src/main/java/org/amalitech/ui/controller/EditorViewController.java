package org.amalitech.ui.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import org.amalitech.config.ServiceContainer;
import org.amalitech.models.Tag;
import org.amalitech.service.PostService;
import org.amalitech.service.PostTagService;
import org.amalitech.service.TagService;
import org.amalitech.ui.session.SessionContext;
import org.amalitech.models.Post;
import org.amalitech.ui.util.AppExecutors;
import org.amalitech.ui.util.ControllerUtils;
import org.amalitech.ui.util.DbTask;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class EditorViewController {

    @FXML private Label saveStatus;
    @FXML private Button editorModeButton;

    @FXML private VBox regularEditorView;
    @FXML private TextField titleField;
    @FXML private TextField tagsField;
    @FXML private ComboBox<String> statusCombo;
    @FXML private TextArea contentEditor;
    @FXML private Label wordCount;
    @FXML private Label charCount;
    @FXML private Label readTimeEstimate;

    @FXML private VBox editorOnlyView;
    @FXML private TextField titleFieldFullscreen;
    @FXML private TextField tagsFieldFullscreen;
    @FXML private ComboBox<String> statusComboFullscreen;
    @FXML private TextArea contentEditorFullscreen;
    @FXML private Label wordCountFullscreen;
    @FXML private Label readTimeFullscreen;

    private Post currentPost;
    private boolean isFullscreenMode = false;
    private boolean hasUnsavedChanges = false;

    private final PostService postService;
    private final TagService tagService;

    public EditorViewController() {
        ServiceContainer container = ServiceContainer.getInstance();
        this.postService = container.getPostService();
        this.tagService = container.getTagService();
    }

    @FXML
    public void initialize() {
        statusCombo.getItems().addAll("Draft", "Published", "Archived");
        statusCombo.setValue("Draft");

        if (statusComboFullscreen != null) {
            statusComboFullscreen.getItems().addAll("Draft", "Published", "Archived");
            statusComboFullscreen.setValue("Draft");
        }

        setupWordCount();

        showRegularEditor();
    }

    @FXML
    private void onBack() {
        if (hasUnsavedChanges) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Unsaved Changes");
            alert.setHeaderText("You have unsaved changes");
            alert.setContentText("Do you want to save before leaving?");

            ButtonType saveButton = new ButtonType("Save");
            ButtonType discardButton = new ButtonType("Discard");
            ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

            alert.getButtonTypes().setAll(saveButton, discardButton, cancelButton);

            alert.showAndWait().ifPresent(response -> {
                if (response == saveButton) {
                    saveDraft();
                    navigateBack();
                } else if (response == discardButton) {
                    navigateBack();
                }
            });
        } else {
            navigateBack();
        }
    }

    private void navigateBack() {
        MainLayoutController mainController = ControllerUtils.getMainController(contentEditor.getScene());
        if (mainController != null) {
            mainController.onHome();
        }
    }

    @FXML
    private void onToggleEditorMode() {
        isFullscreenMode = !isFullscreenMode;

        if (isFullscreenMode) {
            showFullscreenEditor();
        } else {
            showRegularEditor();
        }
    }

    @FXML
    private void onSaveDraft() {
        saveDraft();
    }

    @FXML
    private void onPublish() {
        if (validatePost()) {
            saveAndPublish();
        }
    }

    private void showRegularEditor() {
        if (isFullscreenMode && titleFieldFullscreen != null) {
            titleField.setText(titleFieldFullscreen.getText());
            tagsField.setText(tagsFieldFullscreen.getText());
            statusCombo.setValue(statusComboFullscreen.getValue());
            contentEditor.setText(contentEditorFullscreen.getText());
        }

        regularEditorView.setManaged(true);
        regularEditorView.setVisible(true);
        editorOnlyView.setManaged(false);
        editorOnlyView.setVisible(false);

        editorModeButton.setText("Fullscreen");
        editorModeButton.getStyleClass().remove("primary-button");
        if (!editorModeButton.getStyleClass().contains("ghost-button")) {
            editorModeButton.getStyleClass().add("ghost-button");
        }
    }

    private void showFullscreenEditor() {
        if (titleFieldFullscreen != null && titleField != null) {
            titleFieldFullscreen.setText(titleField.getText());
            tagsFieldFullscreen.setText(tagsField.getText());
            statusComboFullscreen.setValue(statusCombo.getValue());
            contentEditorFullscreen.setText(contentEditor.getText());
        }

        regularEditorView.setManaged(false);
        regularEditorView.setVisible(false);
        editorOnlyView.setManaged(true);
        editorOnlyView.setVisible(true);

        editorModeButton.setText("Exit Fullscreen");
        editorModeButton.getStyleClass().remove("ghost-button");
        if (!editorModeButton.getStyleClass().contains("primary-button")) {
            editorModeButton.getStyleClass().add("primary-button");
        }
    }

    private void setupWordCount() {
        contentEditor.textProperty().addListener((obs, oldVal, newVal) -> {
            updateWordCountRegular(newVal);
            hasUnsavedChanges = true;
        });

        if (contentEditorFullscreen != null) {
            contentEditorFullscreen.textProperty().addListener((obs, oldVal, newVal) -> {
                updateWordCountFullscreen(newVal);
                hasUnsavedChanges = true;
            });
        }

        updateWordCountRegular(contentEditor.getText());
        if (contentEditorFullscreen != null) {
            updateWordCountFullscreen(contentEditorFullscreen.getText());
        }
    }

    private void updateWordCountRegular(String text) {
        int readTime = ControllerUtils.estimateReadTime(text);
        if (text == null || text.trim().isEmpty()) {
            wordCount.setText("0 words");
            charCount.setText("0 characters");
            readTimeEstimate.setText("< 1 min read");
            return;
        }

        String[] words = text.trim().split("\\s+");
        int wordCountValue = words.length;
        int chars = text.length();

        this.wordCount.setText(wordCountValue + " words");
        this.charCount.setText(chars + " characters");
        this.readTimeEstimate.setText(readTime + " min read");
    }

    private void updateWordCountFullscreen(String text) {
        if (wordCountFullscreen == null || readTimeFullscreen == null) {
            return;
        }

        int readTime = ControllerUtils.estimateReadTime(text);
        if (text == null || text.trim().isEmpty()) {
            wordCountFullscreen.setText("0 words");
            readTimeFullscreen.setText("< 1 min read");
            return;
        }

        String[] words = text.trim().split("\\s+");
        int wordCountValue = words.length;

        wordCountFullscreen.setText(wordCountValue + " words");
        readTimeFullscreen.setText(readTime + " min read");
    }

    private void saveDraft() {
        savePost("draft");
    }

    private void saveAndPublish() {
        savePost("published");
    }

    private void savePost(String status) {
        String title = isFullscreenMode && titleFieldFullscreen != null
                ? titleFieldFullscreen.getText().trim()
                : titleField.getText().trim();
        String content = isFullscreenMode && contentEditorFullscreen != null
                ? contentEditorFullscreen.getText().trim()
                : contentEditor.getText().trim();
        String tagsText = isFullscreenMode && tagsFieldFullscreen != null
                ? tagsFieldFullscreen.getText().trim()
                : tagsField.getText().trim();

        if (title.isEmpty()) {
            showError("Title is required");
            return;
        }

        Post post = currentPost != null ? currentPost : new Post();
        post.setTitle(title);
        post.setBody(content);
        post.setStatus(status.toLowerCase());
        post.setUserId(SessionContext.getCurrentUser().getId());
        post.setUpdatedAt(LocalDateTime.now());

        if (currentPost == null) {
            post.setCreatedAt(LocalDateTime.now());
        }

        DbTask<Void> task = new DbTask<>(() -> {
            // Extract tags
            List<String> tagNames = java.util.Arrays.stream(tagsText.split("[,\\s]+"))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(s -> s.startsWith("#") ? s.substring(1) : s)
                    .distinct()
                    .toList();

            List<Integer> tagIds = tagNames.stream().map(name -> {
                Tag tag = tagService.getTagByName(name);
                if (tag == null) {
                    tag = new Tag();
                    tag.setName(name);
                    tagService.createTag(tag);
                    tag = tagService.getTagByName(name);
                }
                return tag.getId();
            }).collect(Collectors.toList());

            if (currentPost == null) {
                postService.createPost(post, tagIds);
            } else {
                postService.updatePost(post, tagIds);
            }
            return null;
        });

        task.setOnSucceeded(event -> {
            hasUnsavedChanges = false;
            currentPost = post;
            if ("published".equalsIgnoreCase(status)) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Published");
                alert.setHeaderText("Post published successfully!");
                alert.setContentText("Your post is now live.");
                alert.showAndWait();
                navigateBack();
            } else {
                updateSaveStatus("All changes saved");
            }
        });

        task.setOnFailed(event -> {
            if ("published".equalsIgnoreCase(status)) {
                showError("Failed to publish: " + task.getException().getMessage());
            } else {
                updateSaveStatus("Save failed");
                showError("Failed to save draft: " + task.getException().getMessage());
            }
        });

        AppExecutors.getDbExecutor().execute(task);
    }

    private boolean validatePost() {
        String title = isFullscreenMode && titleFieldFullscreen != null
                ? titleFieldFullscreen.getText().trim()
                : titleField.getText().trim();
        String content = isFullscreenMode && contentEditorFullscreen != null
                ? contentEditorFullscreen.getText().trim()
                : contentEditor.getText().trim();

        if (title.isEmpty()) {
            showError("Title is required");
            return false;
        }

        if (content.isEmpty()) {
            showError("Content is required");
            return false;
        }

        if (content.split("\\s+").length < 50) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Short Post");
            alert.setHeaderText("Your post is quite short");
            alert.setContentText("Posts under 50 words may not engage readers. Publish anyway?");
            return alert.showAndWait().filter(response -> response == ButtonType.OK).isPresent();
        }

        return true;
    }

    private void updateSaveStatus(String status) {
        Platform.runLater(() -> saveStatus.setText(status));
    }

    private void showError(String message) {
        ControllerUtils.showError(message);
    }

    public void loadPost(Post post) {
        this.currentPost = post;

        if (post != null) {
            titleField.setText(post.getTitle() != null ? post.getTitle() : "");
            contentEditor.setText(post.getBody() != null ? post.getBody() : "");
            statusCombo.setValue(capitalizeFirst(post.getStatus()));

            List<Integer> tagIds = postService.getTagsForPost(post.getId());
            List<Tag> tags = tagService.getAllTags().stream()
                    .filter(t -> tagIds.contains(t.getId()))
                    .toList();
            String tagText = tags.stream()
                    .map(Tag::getName)
                    .map(name -> "#" + name)
                    .collect(Collectors.joining(", "));
            tagsField.setText(tagText);

            hasUnsavedChanges = false;
            updateSaveStatus("All changes saved");
        } else {
            titleField.setText("");
            contentEditor.setText("");
            statusCombo.setValue("Draft");
            tagsField.setText("");

            hasUnsavedChanges = false;
            updateSaveStatus("New post");
        }
    }

    private String capitalizeFirst(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}