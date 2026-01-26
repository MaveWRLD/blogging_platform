package org.amalitech.ui.controller;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
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

public class RegisterController {

    @FXML
    private TextField usernameField;

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private PasswordField confirmField;

    private final UserService userService;

    public RegisterController() {
        this.userService = ServiceContainer.getInstance().getUserService();
    }

    @FXML
    protected void onCreate(ActionEvent event) {
        String username = usernameField.getText() == null ? "" : usernameField.getText().trim();
        String email = emailField.getText() == null ? "" : emailField.getText().trim();
        String password = passwordField.getText() == null ? "" : passwordField.getText();
        String confirm = confirmField.getText() == null ? "" : confirmField.getText();

        if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Validation error", "All fields are required");
            return;
        }

        if (!password.equals(confirm)) {
            showAlert(Alert.AlertType.ERROR, "Validation error", "Passwords do not match");
            return;
        }

        DbTask<User> task = new DbTask<>(() -> {
            User u = new User(username, email, password, "READER", "ACTIVE");
            userService.createUser(u);
            return userService.authenticate(username, password);
        });

        task.setOnSucceeded(e -> {
            User created = task.getValue();
            if (created != null) {
                SessionContext.setCurrentUser(created);
                showAlert(Alert.AlertType.INFORMATION, "Account created", "Welcome, " + created.getUsername());
                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                stage.close();
                try {
                    java.net.URL url = getClass().getResource("/fxml/dashboard.fxml");
                    if (url == null) {
                        showAlert(Alert.AlertType.ERROR, "Navigation error", "FXML not found: /fxml/dashboard.fxml");
                        return;
                    }
                    FXMLLoader loader = new FXMLLoader(url);
                    Parent root = loader.load();
                    Stage dash = new Stage();
                    dash.setTitle("Dashboard");
                    dash.setScene(new Scene(root));
                    dash.show();
                } catch (Exception ex) {
                    ex.printStackTrace();
                    java.nio.file.Path p = org.amalitech.ui.util.ErrorLogger.log(ex);
                    showAlert(Alert.AlertType.ERROR, "Navigation error", "Unable to open dashboard. Details logged to: " + p);
                }
            } else {
                showAlert(Alert.AlertType.ERROR, "Registration failed", "Unable to create or authenticate user");
            }
        });

        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            if (ex != null) ErrorLogger.log(ex);
            showAlert(Alert.AlertType.ERROR, "Registration error", ex == null ? "Unknown error" : ex.getMessage());
        });

        AppExecutors.db().submit(task);
    }

    @FXML
    protected void onCancel(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Platform.runLater(() -> {
            Alert a = new Alert(type);
            a.setTitle(title);
            a.setHeaderText(null);
            a.setContentText(message);
            a.initModality(Modality.APPLICATION_MODAL);
            a.showAndWait();
        });
    }
}
