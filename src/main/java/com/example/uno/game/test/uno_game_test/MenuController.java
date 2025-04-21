package com.example.uno.game.test.uno_game_test;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextInputDialog;
import javafx.stage.Stage;

import java.util.Optional;

public class MenuController {
    @FXML private Button singleplayerButton;
    @FXML private Button multiplayerButton;
    @FXML private Button exitButton;

    @FXML
    private void initialize() {
        singleplayerButton.setOnAction(e -> handleSingleplayer());
        multiplayerButton.setOnAction(e -> handleMultiplayer());
        exitButton.setOnAction(e -> exitGame());
    }

    private void handleSingleplayer() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("gameSetupUI.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) singleplayerButton.getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setTitle("UNO - Setup Game");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void exitGame() {
        System.exit(0);
    }

    private void handleMultiplayer() {
        // For now, just show a placeholder message
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Multiplayer Mode");
        alert.setHeaderText(null);
        alert.setContentText("Multiplayer mode is not implemented yet.");
        alert.showAndWait();
    }

}