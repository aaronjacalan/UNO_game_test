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

    @FXML
    private void initialize() {
        // Set up Singleplayer button
        singleplayerButton.setOnAction(e -> handleSingleplayer());

        // Set up Multiplayer button
        multiplayerButton.setOnAction(e -> handleMultiplayer());
    }

    private void handleSingleplayer() {
        // Prompt user to input the number of players (1-7)
        TextInputDialog dialog = new TextInputDialog("1");
        dialog.setTitle("Singleplayer Setup");
        dialog.setHeaderText("Enter number of computer players (1-7):");
        dialog.setContentText("Number of computer players:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(input -> {
            try {
                int numPlayers = Integer.parseInt(input);
                if (numPlayers < 1 || numPlayers > 7) {
                    showError("Invalid input", "Please enter a number between 1 and 7.");
                } else {
                    // Start the singleplayer game
                    startSingleplayerGame(numPlayers);
                }
            } catch (NumberFormatException ex) {
                showError("Invalid input", "Please enter a valid number.");
            }
        });
    }

    private void handleMultiplayer() {
        // For now, just show a placeholder message
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Multiplayer Mode");
        alert.setHeaderText(null);
        alert.setContentText("Multiplayer mode is not implemented yet.");
        alert.showAndWait();
    }

    private void startSingleplayerGame(int numPlayers) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("GameUI.fxml"));
            Parent root = loader.load();

            GameController controller = loader.getController();
            controller.startGame(numPlayers);

            Stage stage = (Stage) singleplayerButton.getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setTitle("UNO - Singleplayer");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}