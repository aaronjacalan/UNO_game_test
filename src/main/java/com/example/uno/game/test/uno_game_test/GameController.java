package com.example.uno.game.test.uno_game_test;

import com.example.uno.game.test.uno_game_test.Models.*;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.util.Duration;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class GameController implements Initializable {
    // New FXML components
    @FXML private HBox topOpponentsRow;
    @FXML private VBox leftOpponentsColumn;
    @FXML private VBox rightOpponentsColumn;
    @FXML private ScrollPane playerHandScroll;
    @FXML private FlowPane playerHand;
    @FXML private ImageView discardPileView;
    @FXML private ImageView drawPileView;
    @FXML private Button drawButton;
    @FXML private Rectangle colorIndicator;
    @FXML private Label currentColorLabel;
    @FXML private Label statusLabel;
    @FXML private Button endTurnButton;
    @FXML private StackPane notificationArea;
    @FXML private Label gameDirectionLabel;
    @FXML private Label lastActionLabel;

    // Existing game state variables
    private boolean hasDrawnCard = false;
    private Game game;
    private final ScheduledExecutorService computerPlayerTimer = Executors.newSingleThreadScheduledExecutor();
    private boolean isFirstTurn = true;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Start a new game with 8 players (1 human + 7 computer)
        game = new Game(8);

        // Set up the notification area if it doesn't exist in FXML
        if (notificationArea == null) {
            notificationArea = new StackPane();
            notificationArea.setPrefHeight(50);
            VBox gamePane = new VBox();
            gamePane.getChildren().add(1, notificationArea); // Add after status label
        }

        // Set up the game direction label if it doesn't exist in FXML
        if (gameDirectionLabel == null) {
            gameDirectionLabel = new Label("Direction: Clockwise →");
            gameDirectionLabel.setStyle("-fx-font-weight: bold;");
        }

        // Set up the last action label if it doesn't exist in FXML
        if (lastActionLabel == null) {
            lastActionLabel = new Label("Game started!");
            lastActionLabel.setStyle("-fx-font-style: italic;");
        }

        // Create end turn button if it doesn't exist in FXML
        if (endTurnButton == null) {
            endTurnButton = new Button("End Turn");
            endTurnButton.getStyleClass().add("game-button");
        }

        // Configure player hand scroll pane if it's available
        if (playerHandScroll != null) {
            playerHandScroll.setFitToWidth(true);
            playerHandScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
            playerHandScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        }

        // Set up draw card button
        drawButton.setOnAction(e -> handleDrawCard());

        // Set up end turn button
        endTurnButton.setOnAction(e -> handleEndTurn());

        // Update button states
        updateButtonStates();

        // Set up the UI
        updateUI();

        // Start computer player turns if needed
        checkAndStartComputerTurn();
    }

    public void startGame(int numPlayers) {
        game = new Game(numPlayers + 1);
        isFirstTurn = true;
        hasDrawnCard = false;
        updateUI();
        updateButtonStates();
        updateGameDirectionLabel(true); // Default direction is clockwise
    }

    // Update button states based on game state
    private void updateButtonStates() {
        boolean isPlayerTurn = game.getCurrentPlayer() == game.getPlayers().getFirst();

        // Draw button is always enabled during player's turn
        drawButton.setDisable(!isPlayerTurn);

        // End Turn button is only enabled if player has drawn at least one card
        endTurnButton.setDisable(!isPlayerTurn || !hasDrawnCard);
    }

    private void updateUI() {
        // Update player hand
        updatePlayerHand();

        // Update opponent views
        updateOpponentViews();

        // Update discard pile top card
        updateDiscardPile();

        // Update current color indicator
        updateColorIndicator();

        // Update status labels
        Player currentPlayer = game.getCurrentPlayer();
        statusLabel.setText("Current turn: " + currentPlayer.getName());

        // Update button states
        updateButtonStates();
    }

    private void updatePlayerHand() {
        playerHand.getChildren().clear();
        Player humanPlayer = game.getPlayers().get(0);

        for (int i = 0; i < humanPlayer.getHand().size(); i++) {
            Card card = humanPlayer.getHand().get(i);
            final int cardIndex = i;

            // Create card view with stack pane for hover effects
            StackPane cardPane = new StackPane();
            cardPane.getStyleClass().add("player-card");

            ImageView cardView = new ImageView();
            cardView.setFitHeight(120);
            cardView.setFitWidth(80);
            cardView.setPreserveRatio(true);

            // Try to load the card image or use placeholder
            try {
                cardView.setImage(new Image(getClass().getResourceAsStream(card.getImagePath())));
                cardPane.getChildren().add(cardView);
            } catch (Exception exception) {
                // If card images aren't available, create a simple colored rectangle
                Rectangle cardRect = new Rectangle(80, 120);
                cardRect.setFill(getJavaFXColor(card.getColor()));
                cardRect.setStroke(Color.BLACK);
                cardRect.setArcHeight(10);
                cardRect.setArcWidth(10);

                Label cardLabel = new Label();
                if (card.getType() == Card.Type.NUMBER) {
                    cardLabel.setText(String.valueOf(card.getNumber()));
                } else {
                    cardLabel.setText(card.getType().toString().substring(0, 1));
                }
                cardLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
                cardLabel.setTextFill(Color.WHITE);

                cardPane.getChildren().addAll(cardRect, cardLabel);
            }

            // Add click event for playing this card
            cardPane.setOnMouseClicked(e -> handleCardClick(cardIndex));

            // Add hover effect for better UX
            cardPane.setOnMouseEntered(e -> cardPane.setTranslateY(-10));
            cardPane.setOnMouseExited(e -> cardPane.setTranslateY(0));

            playerHand.getChildren().add(cardPane);
        }
    }

    private void updateOpponentViews() {
        // Clear existing opponent views
        if (topOpponentsRow != null) topOpponentsRow.getChildren().clear();
        if (leftOpponentsColumn != null) leftOpponentsColumn.getChildren().clear();
        if (rightOpponentsColumn != null) rightOpponentsColumn.getChildren().clear();

        // Create opponent views (up to 7 opponents)
        for (int i = 1; i < game.getPlayers().size(); i++) {
            Player opponent = game.getPlayers().get(i);

            // Create the opponent container
            VBox opponentBox = new VBox(5);
            opponentBox.setAlignment(Pos.CENTER);
            opponentBox.getStyleClass().add("opponent-container");

            // Add name label
            Label nameLabel = new Label(opponent.getName());
            nameLabel.getStyleClass().add("opponent-name");

            // Highlight current player's turn
            if (game.getCurrentPlayer() == opponent) {
                opponentBox.getStyleClass().add("active-player");
                nameLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: white;");
            }

            // Create visual representation of cards
            HBox cardsView = new HBox(-5); // Negative spacing for overlapping cards
            cardsView.setAlignment(Pos.CENTER);

            // Show up to 7 cards visually
            int cardCount = opponent.getHand().size();
            int visibleCards = Math.min(cardCount, 7);

            for (int j = 0; j < visibleCards; j++) {
                Rectangle cardBack = new Rectangle(30, 45);
                cardBack.setFill(Color.DARKBLUE);
                cardBack.setStroke(Color.WHITE);
                cardBack.setStrokeWidth(1);
                cardBack.setArcHeight(5);
                cardBack.setArcWidth(5);
                cardsView.getChildren().add(cardBack);
            }

            // Add card count label
            Label cardCountLabel = new Label(cardCount + " cards");

            // Add everything to the opponent box
            opponentBox.getChildren().addAll(nameLabel, cardsView, cardCountLabel);

            // Position opponent based on index
            if (i <= 3 && topOpponentsRow != null) {
                // First 3 opponents go in top row
                topOpponentsRow.getChildren().add(opponentBox);
            } else if (i <= 5 && leftOpponentsColumn != null) {
                // Next 2 opponents go in left column
                leftOpponentsColumn.getChildren().add(opponentBox);
            } else if (rightOpponentsColumn != null) {
                // Last 2 opponents go in right column
                rightOpponentsColumn.getChildren().add(opponentBox);
            }
        }
    }

    private void updateDiscardPile() {
        Card topCard = game.getTopCard();
        try {
            discardPileView.setImage(new Image(getClass().getResourceAsStream(topCard.getImagePath())));
        } catch (Exception e) {
            // Handle case where images aren't available
            // This just keeps the existing image or does nothing
        }
    }

    private void updateColorIndicator() {
        Card.Color currentColor = game.getCurrentColor();
        colorIndicator.setFill(getJavaFXColor(currentColor));
        currentColorLabel.setText("Current Color: " + currentColor);
    }

    private Color getJavaFXColor(Card.Color cardColor) {
        return switch (cardColor) {
            case RED -> Color.RED;
            case BLUE -> Color.BLUE;
            case GREEN -> Color.GREEN;
            case YELLOW -> Color.YELLOW;
            case WILD -> Color.BLACK;
            default -> Color.GRAY;
        };
    }

    private void handleCardClick(int cardIndex) {
        if (game.getCurrentPlayer() != game.getPlayers().getFirst()) {
            // Not player's turn
            return;
        }

        Card card = game.getPlayers().getFirst().getHand().get(cardIndex);

        if (card.getColor() == Card.Color.WILD) {
            // For wild cards, prompt for color selection
            showColorSelectionDialog();
            if (game.playCard(cardIndex)) {
                // Update last action based on card type
                updateLastAction(card);
                updateUI();

                // Check for UNO or win
                checkGameStatus();

                // Reset drawn card flag for next turn
                hasDrawnCard = false;

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

                // Reset drawn card flag for next turn
                hasDrawnCard = false;

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

    // Update the last action label based on the card played
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
            updateGameDirectionLabel(false); // Toggle direction (using your existing method)
        } else if (card.getType() == Card.Type.DRAW_TWO) {
            actionText += " - Next player draws 2 cards!";
            showNotification("+2 CARDS", Color.RED);
        } else if (card.getType() == Card.Type.DRAW_FOUR) {
            actionText += " - Next player draws 4 cards!";
            showNotification("+4 CARDS", Color.DARKRED);
        }

        lastActionLabel.setText(actionText);
    }

    // Show a temporary notification for important game events
    private void showNotification(String message, Color color) {
        if (notificationArea == null) return;

        Text notification = new Text(message);
        notification.setFont(Font.font("System", FontWeight.BOLD, 24));
        notification.setFill(color);
        notification.setStroke(Color.WHITE);
        notification.setStrokeWidth(0.5);

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

    // Update the game direction indicator (using your existing method)
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
        } else {
            // Set to specific value
            gameDirectionLabel.setText("Direction: Counter-clockwise ←");
        }
    }

    private void showColorSelectionDialog() {
        Dialog<Card.Color> dialog = new Dialog<>();
        dialog.setTitle("Choose a Color");
        dialog.setHeaderText("Select a color for the Wild card.");

        // Create color buttons with improved styling
        Button redButton = new Button("Red");
        redButton.setStyle("-fx-background-color: #ff6666; -fx-text-fill: white;");
        redButton.setOnAction(event -> {
            game.setCurrentColor(Card.Color.RED);
            dialog.setResult(Card.Color.RED);
            dialog.close();
        });

        Button blueButton = new Button("Blue");
        blueButton.setStyle("-fx-background-color: #6666ff; -fx-text-fill: white;");
        blueButton.setOnAction(event -> {
            game.setCurrentColor(Card.Color.BLUE);
            dialog.setResult(Card.Color.BLUE);
            dialog.close();
        });

        Button greenButton = new Button("Green");
        greenButton.setStyle("-fx-background-color: #66ff66; -fx-text-fill: white;");
        greenButton.setOnAction(event -> {
            game.setCurrentColor(Card.Color.GREEN);
            dialog.setResult(Card.Color.GREEN);
            dialog.close();
        });

        Button yellowButton = new Button("Yellow");
        yellowButton.setStyle("-fx-background-color: #ffff66; -fx-text-fill: black;");
        yellowButton.setOnAction(event -> {
            game.setCurrentColor(Card.Color.YELLOW);
            dialog.setResult(Card.Color.YELLOW);
            dialog.close();
        });

        HBox buttonBox = new HBox(10, redButton, blueButton, greenButton, yellowButton);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.setPrefHeight(60);

        dialog.getDialogPane().setContent(buttonBox);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);

        Optional<Card.Color> result = dialog.showAndWait();
        result.ifPresent(color -> game.setCurrentColor(color));
    }

    // Using your existing handleDrawCard method
    private void handleDrawCard() {
        if (game.getCurrentPlayer() != game.getPlayers().getFirst()) {
            // Not player's turn
            return;
        }

        // Use the new drawCard method which doesn't advance the turn
        game.drawCard();

        // Update last action text
        int drawnCards = hasDrawnCard ? getCardsDrawnCount() + 1 : 1;
        lastActionLabel.setText("You drew " + drawnCards + (drawnCards == 1 ? " card" : " cards"));

        // Mark that player has drawn at least one card
        hasDrawnCard = true;

        // Update UI to show the new card and button states
        updateUI();
    }

    // Helper method to get how many cards have been drawn this turn (unchanged)
    private int getCardsDrawnCount() {
        // This is an estimate of cards drawn this turn based on the text
        String actionText = lastActionLabel.getText();
        if (actionText.startsWith("You drew ")) {
            try {
                String[] parts = actionText.split(" ");
                if (parts.length >= 3) {
                    return Integer.parseInt(parts[2]);
                }
            } catch (NumberFormatException e) {
                // If we can't parse the number, just return 1
            }
        }
        return 1;
    }

    // Handle end turn button click (unchanged)
    private void handleEndTurn() {
        if (game.getCurrentPlayer() != game.getPlayers().getFirst() || !hasDrawnCard) {
            // Not player's turn or player hasn't drawn a card yet
            return;
        }

        // Advance to the next player's turn
        game.nextPlayer();
        hasDrawnCard = false;  // Reset the flag for next turn
        updateUI();

        // Show notification that turn has ended
        showNotification("Turn Ended", Color.GRAY);

        // Start computer turns if needed
        checkAndStartComputerTurn();
    }

    private void checkGameStatus() {
        for (Player player : game.getPlayers()) {
            if (player.hasWon()) {
                // Game over
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Game Over");
                alert.setHeaderText(player.getName() + " wins!");
                alert.setContentText("Game finished.");
                alert.showAndWait();

                // Start a new game
                game = new Game(game.getPlayers().size()); // Keep same player count
                isFirstTurn = true;
                hasDrawnCard = false;
                updateUI();
                updateGameDirectionLabel(true);
                return;
            } else if (player.hasUno()) {
                // Player has UNO
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
                updateUI(); // Update the UI to highlight the current player
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
            // Still using drawCardForPlayer for computer players (draws and ends turn)
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