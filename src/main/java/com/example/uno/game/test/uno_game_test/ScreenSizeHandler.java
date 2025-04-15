package com.example.uno.game.test.uno_game_test;
import javafx.scene.Scene;

public class ScreenSizeHandler {

    /**
     * Updates the UI based on the current screen size.
     * Call this method when the scene is initialized and when window size changes.
     *
     * @param scene The current scene
     */
    public static void adjustForScreenSize(Scene scene) {
        double width = scene.getWidth();

        // Remove any existing screen size classes
        scene.getRoot().getStyleClass().removeAll(
                "small-screen", "medium-screen", "large-screen"
        );

        // Add appropriate class based on width
        if (width < 800) {
            scene.getRoot().getStyleClass().add("small-screen");
        } else if (width < 1100) {
            scene.getRoot().getStyleClass().add("medium-screen");
        } else {
            scene.getRoot().getStyleClass().add("large-screen");
        }
    }

    /**
     * Sets up a listener to automatically adjust UI when window size changes
     *
     * @param scene The current scene
     */
    public static void setupScreenSizeListener(Scene scene) {
        // Initial adjustment
        adjustForScreenSize(scene);

        // Listen for window size changes
        scene.widthProperty().addListener((obs, oldVal, newVal) -> {
            adjustForScreenSize(scene);
        });
    }
}