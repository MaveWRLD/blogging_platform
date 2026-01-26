package org.amalitech.ui.controller;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.amalitech.config.ServiceContainer;
import org.amalitech.models.User;
import org.amalitech.service.UserService;
import org.amalitech.ui.session.SessionContext;
import org.amalitech.ui.util.AppExecutors;
import org.amalitech.ui.util.DbTask;
import org.amalitech.ui.util.ErrorLogger;

public class LoginController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Button loginButton;

    private final UserService userService;

    public LoginController() {
        this.userService = ServiceContainer.getInstance().getUserService();
    }

    @FXML
    private void initialize() {
    }

    @FXML
    protected void onLogin() {
        final String username = usernameField.getText() == null
                ? ""
                : usernameField.getText().trim();

        if (username.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Validation error", "Username is required");
            return;
        }

        setControlsDisabled(true);

        DbTask<User> task = new DbTask<>(() ->
                userService.authenticate(username, passwordField.getText())
        );

        task.setOnSucceeded(e -> {
            User found = task.getValue();

            if (found == null) {
                showAlert(Alert.AlertType.ERROR, "Login failed", "Invalid username or password");
                setControlsDisabled(false);
                return;
            }

            SessionContext.setCurrentUser(found);

            try {
                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/fxml/main_layout.fxml")
                );
                Parent root = loader.load();

                Stage stage = new Stage();
                stage.setTitle("BlogSpace");
                stage.setScene(new Scene(root));

                Stage currentStage = (Stage) usernameField.getScene().getWindow();
                currentStage.close();

                stage.show();

            } catch (Exception ex) {
                ex.printStackTrace();
                ErrorLogger.log(ex);
                showAlert(Alert.AlertType.ERROR,
                        "Navigation error",
                        "Unable to open main layout");
            } finally {
                setControlsDisabled(false);
            }
        });

        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            if (ex != null) ErrorLogger.log(ex);

            showAlert(Alert.AlertType.ERROR,
                    "Login error",
                    ex == null ? "Unknown error" : ex.getMessage());

            setControlsDisabled(false);
        });

        AppExecutors.db().submit(task);
    }

    @FXML
    protected void onOpenRegister() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/register.fxml")
            );
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Register");
            stage.setScene(new Scene(root));
            stage.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR,
                    "Error",
                    "Unable to open registration window");
        }
    }

    private void setControlsDisabled(boolean disabled) {
        Platform.runLater(() -> {
            usernameField.setDisable(disabled);
            passwordField.setDisable(disabled);
            loginButton.setDisable(disabled);
        });
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(type);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
}

