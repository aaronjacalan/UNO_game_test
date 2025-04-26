package com.example.uno.game.test.uno_game_test;

import com.example.uno.game.test.uno_game_test.Models.*;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class GameController implements Initializable {
    @FXML private VBox gamePane;
    @FXML private HBox playerHand;
    @FXML private VBox opponentArea;
    @FXML private ImageView discardPileView;
    @FXML private Button drawButton;
    @FXML private Label statusLabel;
    @FXML private Label currentColorLabel;
    @FXML private Rectangle colorIndicator;
    @FXML private StackPane notificationArea; // New: Area for displaying notifications like skip and reverse
    @FXML private Label gameDirectionLabel; // New: Shows current game direction
    @FXML private Label lastActionLabel; // New: Shows the last card played and its effect

    private Game game;
    private final ScheduledExecutorService computerPlayerTimer = Executors.newSingleThreadScheduledExecutor();
    private boolean isFirstTurn = true; // Track if this is the first turn

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Start a new game with 8 players (1 human + 7 computer)
        game = new Game(8);

        // Set up the notification area if it doesn't exist in FXML
        if (notificationArea == null) {
            notificationArea = new StackPane();
            notificationArea.setPrefHeight(50);
            gamePane.getChildren().add(1, notificationArea); // Add after status label
        }

        // Set up the game direction label if it doesn't exist in FXML
        if (gameDirectionLabel == null) {
            gameDirectionLabel = new Label("Direction: Clockwise →");
            gameDirectionLabel.setStyle("-fx-font-weight: bold;");
            HBox directionBox = new HBox(gameDirectionLabel);
            directionBox.setAlignment(Pos.CENTER);
            gamePane.getChildren().add(directionBox);
        }

        // Set up the last action label if it doesn't exist in FXML
        if (lastActionLabel == null) {
            lastActionLabel = new Label("Game started!");
            lastActionLabel.setStyle("-fx-font-style: italic;");
            HBox lastActionBox = new HBox(lastActionLabel);
            lastActionBox.setAlignment(Pos.CENTER);
            gamePane.getChildren().add(lastActionBox);
        }

        updateUI();
        drawButton.setOnAction(e -> handleDrawCard());
        checkAndStartComputerTurn();
    }

    public void startGame(int numPlayers, java.util.List<String> playerNames) {
        game = new Game(numPlayers);

        // Set the player names
        for (int i = 0; i < Math.min(numPlayers, playerNames.size()); i++) {
            Player player = game.getPlayers().get(i);
            player.setName(playerNames.get(i));
        }

        isFirstTurn = true;
        updateUI();
        updateGameDirectionLabel(true);
    }

    private void updateUI() {
        // Update player hand
        playerHand.getChildren().clear();
        Player humanPlayer = game.getPlayers().get(0);

        for (int i = 0; i < humanPlayer.getHand().size(); i++) {
            Card card = humanPlayer.getHand().get(i);
            final int cardIndex = i;

            // Create card view
            ImageView cardView = new ImageView();
            cardView.setFitHeight(120);
            cardView.setFitWidth(80);

            // Try to load the card image or use placeholder
            try {
                cardView.setImage(new Image(getClass().getResourceAsStream(card.getImagePath())));
            } catch (Exception exception) {
                // If card images aren't available, create a simple colored rectangle
                Rectangle cardRect = new Rectangle(80, 120);
                cardRect.setFill(getJavaFXColor(card.getColor()));
                cardRect.setStroke(Color.BLACK);

                Label cardLabel = new Label();
                if (card.getType() == Card.Type.NUMBER) {
                    cardLabel.setText(String.valueOf(card.getNumber()));
                } else {
                    cardLabel.setText(card.getType().toString().substring(0, 1));
                }

                VBox cardBox = new VBox(cardRect, cardLabel);
                cardBox.setAlignment(Pos.CENTER);
                playerHand.getChildren().add(cardBox);

                // Add click event for playing this card
                cardBox.setOnMouseClicked(e -> handleCardClick(cardIndex));
                continue;
            }

            // Add click event for playing this card
            cardView.setOnMouseClicked(e -> handleCardClick(cardIndex));

            playerHand.getChildren().add(cardView);
        }

        // Update opponent area showing number of cards
        opponentArea.getChildren().clear();
        for (int i = 1; i < game.getPlayers().size(); i++) {
            Player opponent = game.getPlayers().get(i);
            HBox opponentBox = new HBox();
            opponentBox.setAlignment(Pos.CENTER);
            opponentBox.setSpacing(10);

            Label nameLabel = new Label(opponent.getName() + "'s hand");
            Label cardCountLabel = new Label(opponent.getHand().size() + " cards");

            if (game.getCurrentPlayer() == opponent) {
                nameLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: blue;");
            }

            opponentBox.getChildren().addAll(nameLabel, cardCountLabel);
            opponentArea.getChildren().add(opponentBox);
        }

        // Update discard pile top card
        Card topCard = game.getTopCard();
        try {
            discardPileView.setImage(new Image(getClass().getResourceAsStream(topCard.getImagePath())));
        } catch (Exception e) {
            // Handle the case where card images aren't available
        }

        // Update current color indicator
        updateColorIndicator();

        // Update status label
        Player currentPlayer = game.getCurrentPlayer();
        statusLabel.setText("Current turn: " + currentPlayer.getName());
    }

    private void updateColorIndicator() {
        Card.Color currentColor = game.getCurrentColor();
        colorIndicator.setFill(getJavaFXColor(currentColor));
        currentColorLabel.setText("Current Color: " + currentColor);
    }

    private Color getJavaFXColor(Card.Color cardColor) {
        switch (cardColor) {
            case RED: return Color.RED;
            case BLUE: return Color.BLUE;
            case GREEN: return Color.GREEN;
            case YELLOW: return Color.YELLOW;
            case WILD: return Color.BLACK;
            default: return Color.GRAY;
        }
    }

    private void handleCardClick(int cardIndex) {
        if (game.getCurrentPlayer() != game.getPlayers().get(0)) {
            // Not player's turn
            return;
        }

        Card card = game.getPlayers().get(0).getHand().get(cardIndex);

        if (card.getColor() == Card.Color.WILD) {
            // For wild cards, prompt for color selection
            showColorSelectionDialog();
            if (game.playCard(cardIndex)) {
                // Update last action based on card type
                updateLastAction(card);

                updateUI();

                // Check for UNO or win
                checkGameStatus();

                // Start computer turns if needed
                checkAndStartComputerTurn();
            }
        } else {
            if (game.playCard(cardIndex)) {
                // Update last action based on card type
                updateLastAction(card);

                updateUI();

                // Check for UNO or win
                checkGameStatus();

                // Start computer turns if needed
                checkAndStartComputerTurn();
            } else {
                // Invalid move
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Invalid Move");
                alert.setHeaderText("You can't play this card");
                alert.setContentText("The card must match the color or number of the top discard card.");
                alert.showAndWait();
            }
        }
    }

    // New: Update the last action label based on the card played
    private void updateLastAction(Card card) {
        String actionText = "Played ";

        // Add card information
        if (card.getType() == Card.Type.NUMBER) {
            actionText += card.getColor() + " " + card.getNumber();
        } else {
            actionText += card.getColor() + " " + card.getType();
        }

        // Handle special cards
        if (card.getType() == Card.Type.SKIP) {
            actionText += " - Player skipped!";
            showNotification("SKIP!", Color.RED);
        } else if (card.getType() == Card.Type.REVERSE) {
            actionText += " - Direction reversed!";
            showNotification("REVERSE!", Color.ORANGE);
            updateGameDirectionLabel(false); // Toggle direction
        } else if (card.getType() == Card.Type.DRAW_TWO) {
            actionText += " - Next player draws 2 cards!";
            showNotification("+2 CARDS", Color.RED);
        } else if (card.getType() == Card.Type.DRAW_FOUR) {
            actionText += " - Next player draws 4 cards!";
            showNotification("+4 CARDS", Color.DARKRED);
        }

        lastActionLabel.setText(actionText);
    }

    // New: Show a temporary notification for important game events
    private void showNotification(String message, Color color) {
        Text notification = new Text(message);
        notification.setFont(Font.font("System", FontWeight.BOLD, 24));
        notification.setFill(color);

        notificationArea.getChildren().clear();
        notificationArea.getChildren().add(notification);

        // Fade out effect
        FadeTransition fadeOut = new FadeTransition(Duration.seconds(0.5), notification);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.setDelay(Duration.seconds(1.5));
        fadeOut.play();

        // Remove after fading
        fadeOut.setOnFinished(e -> notificationArea.getChildren().clear());
    }

    // New: Update the game direction indicator
    private void updateGameDirectionLabel(boolean isClockwise) {
        if (isFirstTurn) {
            // On first turn, just set without toggling
            gameDirectionLabel.setText("Direction: Clockwise →");
            isFirstTurn = false;
            return;
        }

        // Get current direction from label text
        boolean currentIsClockwise = gameDirectionLabel.getText().contains("Clockwise");

        if (isClockwise) {
            // Set to specific value
            gameDirectionLabel.setText("Direction: Clockwise →");
        } else if (!isClockwise) {
            // Set to specific value
            gameDirectionLabel.setText("Direction: Counter-clockwise ←");
        } else {
            // Toggle current direction
            if (currentIsClockwise) {
                gameDirectionLabel.setText("Direction: Counter-clockwise ←");
            } else {
                gameDirectionLabel.setText("Direction: Clockwise →");
            }
        }
    }

    private void showColorSelectionDialog() {
        Dialog<Card.Color> dialog = new Dialog<>();
        dialog.setTitle("Choose a Color");
        dialog.setHeaderText("Select a color for the Wild card.");

        // Create color buttons
        Button redButton = new Button("Red");
        redButton.setOnAction(event -> {
            game.setCurrentColor(Card.Color.RED);
            dialog.setResult(Card.Color.RED);
            dialog.close();
        });

        Button blueButton = new Button("Blue");
        blueButton.setOnAction(event -> {
            game.setCurrentColor(Card.Color.BLUE);
            dialog.setResult(Card.Color.BLUE);
            dialog.close();
        });

        Button greenButton = new Button("Green");
        greenButton.setOnAction(event -> {
            game.setCurrentColor(Card.Color.GREEN);
            dialog.setResult(Card.Color.GREEN);
            dialog.close();
        });

        Button yellowButton = new Button("Yellow");
        yellowButton.setOnAction(event -> {
            game.setCurrentColor(Card.Color.YELLOW);
            dialog.setResult(Card.Color.YELLOW);
            dialog.close();
        });

        // Style the color buttons to show their actual colors
        redButton.setStyle("-fx-background-color: #ff6666; -fx-text-fill: white;");
        blueButton.setStyle("-fx-background-color: #6666ff; -fx-text-fill: white;");
        greenButton.setStyle("-fx-background-color: #66ff66; -fx-text-fill: white;");
        yellowButton.setStyle("-fx-background-color: #ffff66; -fx-text-fill: black;");

        HBox buttonBox = new HBox(10, redButton, blueButton, greenButton, yellowButton);
        buttonBox.setAlignment(Pos.CENTER);

        dialog.getDialogPane().setContent(buttonBox);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);

        Optional<Card.Color> result = dialog.showAndWait();
        result.ifPresent(color -> game.setCurrentColor(color));
    }

    private void handleDrawCard() {
        if (game.getCurrentPlayer() != game.getPlayers().get(0)) {
            // Not player's turn
            return;
        }

        game.drawCardForPlayer();
        lastActionLabel.setText("You drew a card");
        updateUI();

        // Start computer turns if needed
        checkAndStartComputerTurn();
    }

    private void checkGameStatus() {
        for (Player player : game.getPlayers()) {
            if (player.hasWon()) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Game Over");
                alert.setHeaderText(player.getName() + " wins!");
                alert.setContentText("Game finished.");
                alert.showAndWait();

                shutdown();

                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("gameSetupUI.fxml"));
                    Parent root = loader.load();
                    Stage stage = (Stage) statusLabel.getScene().getWindow();
                    stage.getScene().setRoot(root);
                    stage.setTitle("UNO - Setup Game");
                } catch (Exception e) {
                    e.printStackTrace();
                    game = new Game(4);
                    isFirstTurn = true;
                    updateUI();
                    updateGameDirectionLabel(true);
                }
                return;
            } else if (player.hasUno()) {
                String message = player.getName() + " has UNO!";
                statusLabel.setText(message);
                showNotification("UNO!", Color.PURPLE);
            }
        }
    }


    private void checkAndStartComputerTurn() {
        Player currentPlayer = game.getCurrentPlayer();

        if (currentPlayer.isComputer()) {
            // Update status immediately to show whose turn it is
            Platform.runLater(() -> {
                statusLabel.setText("Current turn: " + currentPlayer.getName() + " (thinking...)");
            });

            // Add a delay to make the computer's turn visible to the player
            computerPlayerTimer.schedule(() -> {
                Platform.runLater(() -> {
                    playComputerTurn();
                });
            }, 1, TimeUnit.SECONDS);
        }
    }

    private void playComputerTurn() {
        Player computer = game.getCurrentPlayer();
        if (!computer.isComputer()) {
            return;
        }

        int cardToPlay = computer.selectCardToPlay(game.getTopCard());
        if (cardToPlay >= 0) {
            Card selectedCard = computer.getHand().get(cardToPlay);

            // For wild cards, choose a random color
            if (selectedCard.getColor() == Card.Color.WILD) {
                Card.Color[] colors = {Card.Color.RED, Card.Color.BLUE, Card.Color.GREEN, Card.Color.YELLOW};
                game.setCurrentColor(colors[(int) (Math.random() * colors.length)]);
            }

            // Update last action based on the computer's played card
            String computerAction = computer.getName() + " played ";
            if (selectedCard.getType() == Card.Type.NUMBER) {
                computerAction += selectedCard.getColor() + " " + selectedCard.getNumber();
            } else {
                computerAction += selectedCard.getColor() + " " + selectedCard.getType();
            }

            // Handle special cards for computer play
            if (selectedCard.getType() == Card.Type.SKIP) {
                computerAction += " - Player skipped!";
                showNotification("SKIP!", Color.RED);
            } else if (selectedCard.getType() == Card.Type.REVERSE) {
                computerAction += " - Direction reversed!";
                showNotification("REVERSE!", Color.ORANGE);
                updateGameDirectionLabel(false); // Toggle direction
            } else if (selectedCard.getType() == Card.Type.DRAW_TWO) {
                computerAction += " - Next player draws 2 cards!";
                showNotification("+2 CARDS", Color.RED);
            } else if (selectedCard.getType() == Card.Type.DRAW_FOUR) {
                computerAction += " - Next player draws 4 cards!";
                showNotification("+4 CARDS", Color.DARKRED);
            }

            lastActionLabel.setText(computerAction);
            game.playCard(cardToPlay);

        } else {
            // No valid move, draw a card
            game.drawCardForPlayer();
            lastActionLabel.setText(computer.getName() + " drew a card");
        }

        updateUI();
        checkGameStatus();

        // Check if it's still computer's turn
        checkAndStartComputerTurn();
    }

    public void shutdown() {
        computerPlayerTimer.shutdown();
    }
}