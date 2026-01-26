package org.amalitech.ui.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.FlowPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.amalitech.dao.TagDao;
import org.amalitech.models.Tag;
import org.amalitech.service.TagService;

import java.io.IOException;

public class TagsBrowserController {

    @FXML
    private FlowPane tagsPane;

    private final TagService tagService;

    public TagsBrowserController() {
        this.tagService = new TagService(new TagDao());
    }

    @FXML
    private void initialize() {
        for (Tag t : tagService.getAllTags()) {
            Button pill = new Button("#" + t.getName());
            pill.getStyleClass().add("tag-pill");
            pill.setOnAction(e -> openTagPosts(t.getId()));
            tagsPane.getChildren().add(pill);
        }
    }

    private void openTagPosts(int tagId) {
        try {
            Tag tag = tagService.getTagById(tagId);
            if (tag == null) return;

            MainLayoutController mainController = getMainController();
            if (mainController != null) {
                if (tagsPane.getScene() != null && tagsPane.getScene().getWindow() instanceof Stage stage) {
                    if (stage.getModality() == Modality.WINDOW_MODAL || stage.getModality() == Modality.APPLICATION_MODAL) {
                        stage.close();
                    }
                }

                mainController.onTagClick(tag);
            } else {
                showLegacyTagPosts(tagId);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void showLegacyTagPosts(int tagId) {
        try {
            var fxmlUrl = getClass().getResource("/fxml/tag_posts.fxml");
            if (fxmlUrl == null) {
                throw new IOException("FXML file not found: /fxml/tag_posts.fxml");
            }
            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();

            TagPostsController ctrl = loader.getController();
            ctrl.loadTag(tagId);

            Scene scene = new Scene(root);
            var cssUrl = getClass().getResource("/css/app.css");
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
            }

            Stage stage = new Stage();
            stage.initModality(Modality.WINDOW_MODAL);
            stage.setTitle("Posts tagged #" + tagId);
            stage.setScene(scene);
            stage.showAndWait();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private MainLayoutController getMainController() {
        try {
            if (tagsPane.getScene() != null && tagsPane.getScene().getRoot() != null) {
                Object userData = tagsPane.getScene().getRoot().getUserData();
                if (userData instanceof MainLayoutController) {
                    return (MainLayoutController) userData;
                }
                
                if (tagsPane.getScene().getWindow() instanceof Stage stage) {
                    if (stage.getOwner() != null && stage.getOwner().getScene() != null) {
                        userData = stage.getOwner().getScene().getRoot().getUserData();
                        if (userData instanceof MainLayoutController) {
                            return (MainLayoutController) userData;
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
