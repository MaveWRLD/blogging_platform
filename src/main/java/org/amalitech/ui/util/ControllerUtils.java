package org.amalitech.ui.util;

import javafx.scene.Scene;
import javafx.scene.control.Alert;
import org.amalitech.ui.controller.MainLayoutController;

public class ControllerUtils {

    public static void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static MainLayoutController getMainController(Scene scene) {
        try {
            if (scene != null && scene.getRoot() != null) {
                return (MainLayoutController) scene.getRoot().getUserData();
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    public static int estimateReadTime(String content) {
        if (content == null || content.isEmpty()) return 0;
        int wordCount = content.trim().split("\\s+").length;
        return Math.max(1, wordCount / 200);
    }
}
