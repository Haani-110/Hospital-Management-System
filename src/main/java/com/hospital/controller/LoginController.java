package com.hospital.controller;

import com.hospital.exception.AuthenticationException;
import com.hospital.model.User;
import com.hospital.service.AuthService;
import com.hospital.util.SceneManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

/**
 * Builds and handles events on the login screen.
 *
 * <p>This controller does <b>not</b> hold a direct reference to another
 * controller. After a successful login it invokes an {@link #onLoginSuccess}
 * callback (set by {@link Main}), which keeps controllers decoupled and
 * avoids circular constructor dependencies.
 */
public class LoginController {

    private final AuthService authService;
    private final SceneManager sceneManager;

    /** Called with the authenticated user once login succeeds. */
    private Consumer<User> onLoginSuccess;

    private Label errorLabel;

    public LoginController(AuthService authService, SceneManager sceneManager) {
        this.authService = authService;
        this.sceneManager = sceneManager;
    }

    /**
     * Register a callback to run after successful authentication.
     * Must be called before {@link #buildScene()}.
     */
    public void setOnLoginSuccess(Consumer<User> onLoginSuccess) {
        this.onLoginSuccess = onLoginSuccess;
    }

    public Scene buildScene() {
        VBox root = new VBox(20);
        root.setPadding(new Insets(40));
        root.setAlignment(Pos.CENTER);
        root.getStyleClass().add("root");

        Label title = new Label("Hospital Management System");
        title.getStyleClass().add("title");

        Label subtitle = new Label("Please sign in to continue");
        subtitle.getStyleClass().add("subtitle");

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(12);
        form.setAlignment(Pos.CENTER);

        Label userLabel = new Label("Username:");
        TextField usernameField = new TextField();
        usernameField.setPromptText("e.g. admin");

        Label passLabel = new Label("Password:");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");

        form.add(userLabel, 0, 0);
        form.add(usernameField, 1, 0);
        form.add(passLabel, 0, 1);
        form.add(passwordField, 1, 1);

        Button loginBtn = new Button("Login");
        loginBtn.getStyleClass().add("primary-button");
        loginBtn.setDefaultButton(true);
        loginBtn.setMinWidth(100);

        HBox buttonBox = new HBox(loginBtn);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);

        errorLabel = new Label();
        errorLabel.getStyleClass().add("error");
        errorLabel.setWrapText(true);
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        loginBtn.setOnAction(e -> {
            errorLabel.setVisible(false);
            errorLabel.setManaged(false);
            try {
                User user = authService.login(usernameField.getText(), passwordField.getText());
                if (onLoginSuccess != null) {
                    onLoginSuccess.accept(user);
                }
            } catch (AuthenticationException ex) {
                errorLabel.setText(ex.getMessage());
                errorLabel.setVisible(true);
                errorLabel.setManaged(true);
            }
        });

        root.getChildren().addAll(title, subtitle, form, errorLabel, buttonBox);

        Scene scene = new Scene(root, 600, 450);
        try {
            var cssUrl = getClass().getResource("/com/hospital/css/styles.css");
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
            }
        } catch (Exception ignored) {
            // CSS is optional; UI still works if stylesheet is missing.
        }
        return scene;
    }
}
