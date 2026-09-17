package com.hospital.util;

import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Simple helper for switching between scenes in the primary Stage.
 */
public class SceneManager {
    private final Stage primaryStage;

    public SceneManager(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }

    public void show(Scene scene) {
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public Stage getPrimaryStage() {
        return primaryStage;
    }
}
