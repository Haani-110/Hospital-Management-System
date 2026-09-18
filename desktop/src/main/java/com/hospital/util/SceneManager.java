package com.hospital.util;

import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

/**
 * Owns the one desktop Scene, its persistent rail and its replaceable content.
 * Controllers still build views and supply their original navigation callbacks;
 * their temporary Scenes are view containers, never installed on the Stage.
 */
public class SceneManager {
    private static final double PORTAL_WIDTH = 1280;
    private static final double PORTAL_HEIGHT = 800;
    private static final double MIN_WIDTH = 1000;
    private static final double MIN_HEIGHT = 700;

    private final Stage primaryStage;
    private final BorderPane shell = new BorderPane();
    private final Scene applicationScene = new Scene(shell, PORTAL_WIDTH, PORTAL_HEIGHT);
    private boolean portalOpen;

    public SceneManager(Stage primaryStage) {
        this.primaryStage = primaryStage;
        primaryStage.setResizable(true);
        primaryStage.setScene(applicationScene); // The only Stage.setScene call.
        primaryStage.addEventHandler(WindowEvent.WINDOW_HIDDEN, event -> UiMotion.cancelTree(shell));
    }

    public void show(Scene view) {
        Parent root = view.getRoot();
        boolean login = root.getStyleClass().contains("login-screen");
        if (!login && !(root instanceof BorderPane)) {
            throw new IllegalArgumentException("Portal views must have a BorderPane root.");
        }
        for (String stylesheet : view.getStylesheets()) {
            if (!applicationScene.getStylesheets().contains(stylesheet)) {
                applicationScene.getStylesheets().add(stylesheet);
            }
        }
        // A node cannot belong to two Scenes. Release the temporary view first.
        view.setRoot(new Pane());
        UiMotion.cancelTree(shell.getCenter());

        Node content;
        if (login) {
            UiMotion.cancelTree(shell.getLeft());
            shell.setLeft(null); // Never retain the previous user's rail/session label.
            content = root;
            portalOpen = false;
            sizeEntryWindow(1100, 760, 900, 640);
            UiMotion.prepare(content);
        } else {
            BorderPane page = (BorderPane) root;
            Node incomingRail = page.getLeft();
            // Preserve every content region (headers, body and any footer).
            page.setLeft(null);
            content = page;
            if (!portalOpen) {
                shell.setLeft(incomingRail);
                sizeEntryWindow(PORTAL_WIDTH, PORTAL_HEIGHT, MIN_WIDTH, MIN_HEIGHT);
                portalOpen = true;
            } else {
                // The rail, its scroll position and logout control stay mounted.
                UiStyles.updateSidebar(shell.getLeft(), incomingRail);
            }
            UiMotion.prepareModule(content);
        }
        shell.setCenter(content);
        if (!primaryStage.isShowing()) primaryStage.show();
        UiMotion.revealAfterLayout(content);
        if (!login) UiMotion.revealAfterLayout(shell.getLeft());
    }

    /** Called only at login/portal boundaries, never during module navigation. */
    private void sizeEntryWindow(double width, double height, double minWidth, double minHeight) {
        Screen screen = Screen.getPrimary();
        if (Double.isFinite(primaryStage.getX()) && Double.isFinite(primaryStage.getY())) {
            var screens = Screen.getScreensForRectangle(primaryStage.getX(), primaryStage.getY(), 1, 1);
            if (!screens.isEmpty()) screen = screens.get(0);
        }
        Rectangle2D available = screen.getVisualBounds();
        primaryStage.setMinWidth(Math.min(minWidth, available.getWidth()));
        primaryStage.setMinHeight(Math.min(minHeight, available.getHeight()));
        if (!primaryStage.isMaximized() && !primaryStage.isFullScreen()) {
            primaryStage.setWidth(Math.min(width, available.getWidth()));
            primaryStage.setHeight(Math.min(height, available.getHeight()));
            if (Double.isFinite(primaryStage.getX())) {
                primaryStage.setX(Math.max(available.getMinX(),
                        Math.min(primaryStage.getX(), available.getMaxX() - primaryStage.getWidth())));
                primaryStage.setY(Math.max(available.getMinY(),
                        Math.min(primaryStage.getY(), available.getMaxY() - primaryStage.getHeight())));
            }
        }
    }

    public Stage getPrimaryStage() {
        return primaryStage;
    }
}
