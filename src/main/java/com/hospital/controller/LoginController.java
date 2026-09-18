package com.hospital.controller;

import com.hospital.exception.AuthenticationException;
import com.hospital.model.User;
import com.hospital.service.AuthService;
import com.hospital.util.SceneManager;
import com.hospital.util.UiMotion;
import com.hospital.util.UiStyles;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
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
        root.setPadding(new Insets(24));
        root.setAlignment(Pos.CENTER);
        root.getStyleClass().addAll("root", "login-root");
        root.setMinHeight(Region.USE_PREF_SIZE);

        VBox card = new VBox(16);
        card.getStyleClass().addAll("card", "login-card");
        card.setMaxWidth(440);
        card.setMaxHeight(Region.USE_PREF_SIZE);
        Label eyebrow = new Label("HOSPITAL  /  STAFF PORTAL");
        eyebrow.getStyleClass().add("login-eyebrow");

        Label title = new Label("Hospital Management System");
        title.getStyleClass().add("title");
        title.setWrapText(true);

        Label subtitle = new Label("Sign in to your hospital workspace");
        subtitle.getStyleClass().add("subtitle");

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(12);
        form.setAlignment(Pos.CENTER);

        Label userLabel = new Label("Username:");
        TextField usernameField = new TextField();
        usernameField.setPromptText("Enter your username");

        Label passLabel = new Label("Password:");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Enter your password");

        form.add(userLabel, 0, 0);
        form.add(usernameField, 0, 1);
        form.add(passLabel, 0, 2);
        form.add(passwordField, 0, 3);
        UiStyles.form(form);

        Button loginBtn = new Button("Sign in");
        loginBtn.getStyleClass().add("primary-button");
        loginBtn.setDefaultButton(true);
        loginBtn.setMaxWidth(Double.MAX_VALUE);

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

        Label footer = new Label("Authorized staff access");
        footer.getStyleClass().add("login-footer");
        card.getChildren().addAll(UiStyles.medicalMark(), eyebrow, title, subtitle, form, errorLabel, loginBtn, footer);
        UiMotion.elevate(card, false);
        UiMotion.enter(card, 0, false);
        root.getChildren().add(card);

        ScrollPane viewport = UiStyles.scroll(root);
        viewport.setFitToHeight(true);
        viewport.getStyleClass().add("login-screen");
        Scene scene = new Scene(viewport);
        UiStyles.apply(scene);
        return scene;
    }
}
