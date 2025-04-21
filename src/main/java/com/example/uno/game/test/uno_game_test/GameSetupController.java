package com.example.uno.game.test.uno_game_test;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class GameSetupController {

    @FXML private Spinner<Integer> numberOfPlayersSpinner;
    @FXML private CheckBox allowStackingCheckBox;
    @FXML private CheckBox allowJumpInCheckBox;
    @FXML private CheckBox drawCardsCheckBox;
    @FXML private CheckBox strictWildDrawFourCheckBox;
    @FXML private Button startGameButton;
    @FXML private Button cancelButton;
    @FXML private VBox playersContainer;
    @FXML private Label dateTimeLabel;

    private final AtomicInteger aiPlayerCounter = new AtomicInteger(1);
    private final String currentUser = "Player";
    private final Random random = new Random();
    private final Set<String> usedAiNames = new HashSet<>();
    private final List<String> namePool = new ArrayList<>(Arrays.asList(
            "Ven", "Raimar", "Grant", "Tim", "Romar", "Aaron", "Zillion", "Raymond", "Seth"
    ));

    public void initialize() {
        // Initialize default values
        if (numberOfPlayersSpinner != null) {
            numberOfPlayersSpinner.getValueFactory().setValue(4); // Default number of players

            numberOfPlayersSpinner.valueProperty().addListener(new ChangeListener<Integer>() {
                @Override
                public void changed(ObservableValue<? extends Integer> observable, Integer oldValue, Integer newValue) {
                    syncPlayersWithSpinner(oldValue, newValue);
                }
            });
        }

        // Set up button actions
        startGameButton.setOnAction(e -> handleStartGame());
        cancelButton.setOnAction(e -> handleCancel());

        if (dateTimeLabel != null) {
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            dateTimeLabel.setText(now.format(formatter));
        }

        // Initialize players container with host player and AI players based on spinner value
        if (playersContainer != null) {
            initializePlayersContainer();
        }
    }

    private void initializePlayersContainer() {
        playersContainer.getChildren().clear();
        usedAiNames.clear(); // Reset used names when initializing

        // Add host player
        addHostPlayer();

        addAiPlayer("Jay Vince");

        // Add AI players based on spinner value (minus the host player)
        int aiPlayersToAdd = numberOfPlayersSpinner.getValue() - 2;
        for (int i = 0; i < aiPlayersToAdd; i++) {
            String name = getUniqueAiName();
            addAiPlayer(name);
        }

    }

    private void addHostPlayer() {
        HBox hostEntry = new HBox();
        hostEntry.getStyleClass().addAll("player-entry", "host-player");

        Label nameLabel = new Label(currentUser);
        nameLabel.getStyleClass().add("player-name");

        Region spacer = new Region();
        spacer.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        Label roleLabel = new Label("Host");
        roleLabel.getStyleClass().add("player-role");

        hostEntry.getChildren().addAll(nameLabel, spacer, roleLabel);
        playersContainer.getChildren().add(hostEntry);
    }

    private void syncPlayersWithSpinner(Integer oldValue, Integer newValue) {
        if (playersContainer == null) return;

        int currentPlayers = playersContainer.getChildren().size();
        if (newValue > currentPlayers) {
            int playersToAdd = newValue - currentPlayers;
            for (int i = 0; i < playersToAdd; i++) {
                String name = getUniqueAiName();
                addAiPlayer(name);
            }
        } else if (newValue < currentPlayers) {
            // Remove excess players (but never the host)
            int playersToRemove = currentPlayers - newValue;
            for (int i = 0; i < playersToRemove; i++) {
                // Make sure we don't remove the host player
                if (playersContainer.getChildren().size() > 1) {
                    // Get the player name before removing to update used names set
                    HBox playerEntry = (HBox) playersContainer.getChildren().get(playersContainer.getChildren().size() - 1);
                    Label nameLabel = (Label) playerEntry.getChildren().get(0);
                    String name = nameLabel.getText();

                    // Remove the name from used names if it was from our pool
                    if (!name.startsWith("Computer ")) {
                        usedAiNames.remove(name);
                    }

                    playersContainer.getChildren().remove(playersContainer.getChildren().size() - 1);
                } else {
                    // If we can't remove any more players, adjust the spinner value
                    ((SpinnerValueFactory.IntegerSpinnerValueFactory)
                            numberOfPlayersSpinner.getValueFactory()).setValue(1);
                    break;
                }
            }
        }
    }

    private String getUniqueAiName() {
        if (usedAiNames.size() >= namePool.size()) {
            return "Computer " + aiPlayerCounter.getAndIncrement();
        }

        String name;
        do {
            int index = random.nextInt(namePool.size());
            name = namePool.get(index);
        } while (usedAiNames.contains(name));

        usedAiNames.add(name);
        return name;
    }

    @FXML
    private void addAiPlayer(String name) {
        if (playersContainer != null) {
            // Create a new player entry
            HBox playerEntry = new HBox();
            playerEntry.getStyleClass().addAll("player-entry", "ai-player");

            // Create and add player name label using the name pool
            Label nameLabel = new Label(name);
            nameLabel.getStyleClass().add("player-name");

            // Create spacer region
            Region spacer = new Region();
            spacer.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

            // Create role label
            Label roleLabel = new Label("AI");
            roleLabel.getStyleClass().add("player-role");

            // Add all components to the player entry
            playerEntry.getChildren().addAll(nameLabel, spacer, roleLabel);

            // Add the player entry to the players container
            playersContainer.getChildren().add(playerEntry);

            // Update the number of players spinner if manually adding exceeds the current setting
            int currentPlayerCount = playersContainer.getChildren().size();
            if (currentPlayerCount > numberOfPlayersSpinner.getValue()) {
                numberOfPlayersSpinner.getValueFactory().setValue(currentPlayerCount);
            }
        }
    }

    @FXML
    private void handleStartGame() {
        try {
            // Collect settings
            int numberOfPlayers = numberOfPlayersSpinner.getValue();
            boolean allowStacking = allowStackingCheckBox.isSelected();
            boolean allowJumpIn = allowJumpInCheckBox.isSelected();
            boolean drawCards = drawCardsCheckBox.isSelected();
            boolean strictWildDrawFour = strictWildDrawFourCheckBox.isSelected();

            // Collect player names from the UI
            List<String> playerNames = new ArrayList<>();
            for (int i = 0; i < playersContainer.getChildren().size(); i++) {
                HBox playerEntry = (HBox) playersContainer.getChildren().get(i);
                Label nameLabel = (Label) playerEntry.getChildren().get(0);
                playerNames.add(nameLabel.getText());
            }

            // Pass settings to the game UI controller
            FXMLLoader loader = new FXMLLoader(getClass().getResource("GameUI.fxml"));
            Parent root = loader.load();

            // Pass data to the next controller
            GameController gameUIController = loader.getController();
            gameUIController.startGame(numberOfPlayers, playerNames);

            Stage stage = (Stage) startGameButton.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("UNO - Gameplay");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleCancel() {
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }

}