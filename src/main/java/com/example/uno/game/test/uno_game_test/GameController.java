package com.example.uno.game.test.uno_game_test;

import com.example.uno.game.test.uno_game_test.Models.*;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
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
import javafx.util.Duration;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class GameController implements Initializable {
    // FXML components from GameUI.fxml
    @FXML private Label gameRoundLabel;
    @FXML private Label directionLabel;
    @FXML private Label currentPlayerLabel;
    @FXML private HBox topOpponentsRow;
    @FXML private VBox leftOpponentsColumn;
    @FXML private VBox rightOpponentsColumn;
    @FXML private ScrollPane playerHandScroll;
    @FXML private FlowPane playerHand;
    @FXML private ImageView discardPileView;
    @FXML private ImageView drawPileView;
    @FXML private Rectangle colorIndicator;
    @FXML private Label currentColorLabel;
    @FXML private Label statusLabel;
    @FXML private Button callUnoButton;
    @FXML private Button challengeButton;
    @FXML private HBox colorSelectionPane;
    @FXML private Rectangle playAreaBackground;
    @FXML private Button drawButton; // Added missing FXML button

    // Game state variables
    private boolean hasDrawnCard = false;
    private Game game;
    private final ScheduledExecutorService computerPlayerTimer = Executors.newSingleThreadScheduledExecutor();
    private int currentRound = 1;
    private boolean isUnoCallNeeded = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        javafx.application.Platform.runLater(() -> {
            Scene scene = drawButton.getScene(); // Using any control to get the scene
            if (scene != null) {
                ScreenSizeHandler.setupScreenSizeListener(scene);
            }
        });

        // Configure player hand scroll pane
        playerHandScroll.setFitToWidth(true);
        playerHandScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        playerHandScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        // Set up draw card button - Fixed: Changed to drawButton
        drawButton.setOnAction(e -> handleDrawCard());

        // Set up UNO button
        callUnoButton.setOnAction(e -> handleCallUno());

        // Set up challenge button
        challengeButton.setOnAction(e -> handleChallenge());

        // Set up the color selection buttons
        setupColorSelectionButtons();

        // Start a default game if not started by MenuController
        startGame(7); // Default to 8 players (1 human + 7 computer)
    }

    /**
     * Start a new game with the specified number of players
     * This method is called from the MenuController
     * @param numPlayers Number of computer players (total players will be numPlayers + 1)
     */
    public void startGame(int numPlayers) {
        // Create a new game with the specified number of players (plus human player)
        game = new Game(numPlayers + 1);

        // Reset game state
        isUnoCallNeeded = false;
        hasDrawnCard = false;
        currentRound = 1;

        // Update UI elements
        gameRoundLabel.setText("Round " + currentRound);
        updateUI();

        // Fixed: Removed reassignment of drawPileView that was causing a bug
        try {
            Image cardBackImage = new Image(getClass().getResourceAsStream("/com/example/uno/game/test/uno_game_test/images/card_back.png"));
            drawPileView.setImage(cardBackImage);
        } catch (Exception e) {
            // Handle case where image can't be loaded
            System.err.println("Could not load card back image: " + e.getMessage());
        }

        // Start computer player turns if needed
        checkAndStartComputerTurn();
    }

    /**
     * Set up the color selection panel buttons
     */
    private void setupColorSelectionButtons() {
        // Find all buttons in the colorSelectionPane and set up actions
        colorSelectionPane.getChildren().forEach(node -> {
            if (node instanceof Button button) {
                button.setOnAction(e -> {
                    // Get selected color from the button text
                    Card.Color selectedColor = Card.Color.valueOf(button.getText());
                    game.setCurrentColor(selectedColor);

                    // Hide color selection pane
                    colorSelectionPane.setVisible(false);
                    colorSelectionPane.setManaged(false);

                    // Continue the turn processing
                    completeWildCardPlay();
                });
            }
        });
    }

    /**
     * Continue processing after a wild card color has been selected
     */
    private void completeWildCardPlay() {
        // Update UI to reflect the new color
        updateColorIndicator();

        // Check for UNO or win conditions
        checkGameStatus();

        // Reset drawn card flag for next turn
        hasDrawnCard = false;

        // Start computer turns if needed
        checkAndStartComputerTurn();
    }

    /**
     * Update all UI elements based on the current game state
     */
    private void updateUI() {
        // Update player hand
        updatePlayerHand();

        // Update opponent views
        updateOpponentViews();

        // Update discard pile top card
        updateDiscardPile();

        // Update current color indicator
        updateColorIndicator();

        // Update status and direction labels
        updateStatusLabels();

        // Update button states
        updateButtonStates();
    }

    /**
     * Update the player's hand display
     */
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

    /**
     * Update the opponent displays
     */
    private void updateOpponentViews() {
        // Clear existing opponent views
        topOpponentsRow.getChildren().clear();
        leftOpponentsColumn.getChildren().clear();
        rightOpponentsColumn.getChildren().clear();

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
            HBox cardsView = new HBox(-15); // Increased overlap for card backs
            cardsView.setAlignment(Pos.CENTER);

            // Show up to 7 cards visually
            int cardCount = opponent.getHand().size();
            int visibleCards = Math.min(cardCount, 7);

            // Load the card back image once
            Image cardBackImage = null;
            try {
                // Fixed: Corrected the resource path
                cardBackImage = new Image(getClass().getResourceAsStream("/com/example/uno/game/test/uno_game_test/images/card_back.png"));
            } catch (Exception e) {
                // If image loading fails, we'll create a fallback rectangle
            }

            for (int j = 0; j < visibleCards; j++) {
                if (cardBackImage != null) {
                    // Use the card back image
                    ImageView cardBackView = new ImageView(cardBackImage);
                    cardBackView.setFitHeight(45);
                    cardBackView.setFitWidth(30);
                    cardBackView.setPreserveRatio(true);
                    cardsView.getChildren().add(cardBackView);
                } else {
                    // Fallback to the colored rectangle if image loading failed
                    Rectangle cardBack = new Rectangle(30, 45);
                    cardBack.setFill(Color.DARKBLUE);
                    cardBack.setStroke(Color.WHITE);
                    cardBack.setStrokeWidth(1);
                    cardBack.setArcHeight(5);
                    cardBack.setArcWidth(5);
                    cardsView.getChildren().add(cardBack);
                }
            }

            // Add card count label
            Label cardCountLabel = new Label(cardCount + " cards");

            // Indicate UNO status if applicable
            if (cardCount == 1) {
                cardCountLabel.setText("UNO!");
                cardCountLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
            }

            // Add everything to the opponent box
            opponentBox.getChildren().addAll(nameLabel, cardsView, cardCountLabel);

            // Position opponent based on index
            if (i <= 3) {
                // First 3 opponents go in top row
                topOpponentsRow.getChildren().add(opponentBox);
            } else if (i <= 5) {
                // Next 2 opponents go in left column
                leftOpponentsColumn.getChildren().add(opponentBox);
            } else {
                // Last 2 opponents go in right column
                rightOpponentsColumn.getChildren().add(opponentBox);
            }
        }
    }

    /**
     * Update the discard pile display
     */
    private void updateDiscardPile() {
        Card topCard = game.getTopCard();
        try {
            discardPileView.setImage(new Image(getClass().getResourceAsStream(topCard.getImagePath())));
        } catch (Exception e) {
            // Create a rectangle to represent the card if image loading fails
            Rectangle cardRect = new Rectangle(discardPileView.getFitWidth(), discardPileView.getFitHeight());
            cardRect.setFill(getJavaFXColor(topCard.getColor()));
            cardRect.setStroke(Color.BLACK);
            cardRect.setArcHeight(10);
            cardRect.setArcWidth(10);

            // We can't directly set a Rectangle to an ImageView
            // In a real implementation, you would create a snapshot of the rectangle
            // Here we'll just leave the existing image
        }
    }

    /**
     * Update the color indicator display
     */
    private void updateColorIndicator() {
        Card.Color currentColor = game.getCurrentColor();
        colorIndicator.setFill(getJavaFXColor(currentColor));
        currentColorLabel.setText(currentColor.toString());
    }

    /**
     * Update status and direction labels
     */
    private void updateStatusLabels() {
        Player currentPlayer = game.getCurrentPlayer();

        // Update current player label
        currentPlayerLabel.setText("Current Player: " + currentPlayer.getName());

        // Update status message
        if (currentPlayer == game.getPlayers().get(0)) {
            statusLabel.setText("Your turn!");
        } else {
            statusLabel.setText(currentPlayer.getName() + "'s turn");
        }

        // Update direction label
        directionLabel.setText(game.isClockwise() ? "↻ Clockwise" : "↺ Counter-Clockwise");
    }

    /**
     * Convert a card color to JavaFX Color
     */
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

    /**
     * Update button states based on game state
     */
    private void updateButtonStates() {
        boolean isPlayerTurn = game.getCurrentPlayer() == game.getPlayers().get(0);

        // Draw button is always enabled during player's turn
        drawButton.setDisable(!isPlayerTurn);

        // Call UNO button state
        Player humanPlayer = game.getPlayers().get(0);
        callUnoButton.setDisable(!isPlayerTurn || humanPlayer.getHand().size() != 2);

        // Challenge button state - enabled if any opponent has one card
        boolean canChallenge = false;
        for (int i = 1; i < game.getPlayers().size(); i++) {
            if (game.getPlayers().get(i).getHand().size() == 1) {
                canChallenge = true;
                break;
            }
        }
        challengeButton.setDisable(!canChallenge);
    }

    /**
     * Handle a click on a card in the player's hand
     */
    private void handleCardClick(int cardIndex) {
        if (game.getCurrentPlayer() != game.getPlayers().get(0)) {
            // Not player's turn
            return;
        }

        Card card = game.getPlayers().get(0).getHand().get(cardIndex);

        // Check if this is the player's last or second-to-last card
        Player humanPlayer = game.getPlayers().get(0);
        boolean isUnoSituation = humanPlayer.getHand().size() == 2;

        if (card.getColor() == Card.Color.WILD) {
            // For wild cards, show the color selection pane
            if (game.playCard(cardIndex)) {
                // Show color selection pane
                colorSelectionPane.setVisible(true);
                colorSelectionPane.setManaged(true);

                // Check UNO situation
                if (isUnoSituation) {
                    isUnoCallNeeded = true;
                }

                // Show notification based on card type
                if (card.getType() == Card.Type.DRAW_FOUR) {
                    showNotification("+4 CARDS", Color.RED);
                }

                // updateUI will be called after color selection
                updateUI();
            } else {
                // Invalid move
                showInvalidMoveAlert();
            }
        } else {
            if (game.playCard(cardIndex)) {
                // Show notification based on card type
                showCardActionNotification(card);

                // Check UNO situation
                if (isUnoSituation) {
                    isUnoCallNeeded = true;
                }

                // Update UI and advance the game
                updateUI();
                checkGameStatus();
                hasDrawnCard = false;
                checkAndStartComputerTurn();
            } else {
                // Invalid move
                showInvalidMoveAlert();
            }
        }
    }

    /**
     * Show a notification based on the card action
     */
    private void showCardActionNotification(Card card) {
        switch (card.getType()) {
            case SKIP:
                showNotification("SKIP!", Color.RED);
                break;
            case REVERSE:
                showNotification("REVERSE!", Color.ORANGE);
                break;
            case DRAW_TWO:
                showNotification("+2 CARDS", Color.RED);
                break;
            default:
                // No notification for regular cards
                break;
        }
    }

    /**
     * Show invalid move alert
     */
    private void showInvalidMoveAlert() {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Invalid Move");
        alert.setHeaderText("You can't play this card");
        alert.setContentText("The card must match the color or number of the top discard card.");
        alert.showAndWait();
    }

    /**
     * Show a temporary notification for important game events
     */
    private void showNotification(String message, Color color) {
        // Create notification text
        Font notificationFont = Font.font("System", FontWeight.BOLD, 24);
        javafx.scene.text.Text notification = new javafx.scene.text.Text(message);
        notification.setFont(notificationFont);
        notification.setFill(color);
        notification.setStroke(Color.WHITE);
        notification.setStrokeWidth(0.5);

        // Create a stack pane to hold the notification in center of play area
        StackPane notificationPane = new StackPane(notification);
        notificationPane.setAlignment(Pos.CENTER);

        // Add the notification to the play area background
        StackPane centerArea = (StackPane) playAreaBackground.getParent();
        centerArea.getChildren().add(notificationPane);

        // Fade out effect
        FadeTransition fadeOut = new FadeTransition(Duration.seconds(0.5), notification);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.setDelay(Duration.seconds(1.5));
        fadeOut.play();

        // Remove after fading
        fadeOut.setOnFinished(e -> centerArea.getChildren().remove(notificationPane));
    }

    /**
     * Handle the draw card button click
     */
    private void handleDrawCard() {
        if (game.getCurrentPlayer() != game.getPlayers().get(0)) {
            // Not player's turn
            return;
        }

        // Draw a card
        Card drawnCard = game.drawCard();

        // Show the drawn card
        showDrawnCardNotification(drawnCard);

        // Mark that player has drawn a card
        hasDrawnCard = true;

        // Check if the drawn card can be played
        if (drawnCard.canPlayOn(game.getTopCard()) || drawnCard.getColor() == Card.Color.WILD ||
                drawnCard.getColor() == game.getCurrentColor()) { // Fixed: Added WILD card check
            // Ask if the player wants to play the drawn card
            boolean playDrawnCard = showPlayDrawnCardPrompt(drawnCard);
            if (playDrawnCard) {
                // Player chooses to play the card
                int lastCardIndex = game.getPlayers().get(0).getHand().size() - 1;
                handleCardClick(lastCardIndex);
                return;
            }
        }

        // End turn automatically after drawing
        game.nextPlayer();
        updateUI();
        checkAndStartComputerTurn();
    }

    /**
     * Show notification for the drawn card
     */
    private void showDrawnCardNotification(Card drawnCard) {
        try {
            // Create an ImageView for the card
            ImageView cardView = new ImageView(new Image(getClass().getResourceAsStream(drawnCard.getImagePath())));
            cardView.setFitHeight(150);
            cardView.setFitWidth(100);
            cardView.setPreserveRatio(true);

            // Create notification container
            VBox container = new VBox(10);
            container.setAlignment(Pos.CENTER);
            container.getChildren().addAll(
                    new Label("Card Drawn:"),
                    cardView
            );

            // Create stack pane centered in play area
            StackPane notificationPane = new StackPane(container);
            notificationPane.setAlignment(Pos.CENTER);

            // Show in center area
            StackPane centerArea = (StackPane) playAreaBackground.getParent();
            centerArea.getChildren().add(notificationPane);

            // Fade out after a delay
            FadeTransition fadeOut = new FadeTransition(Duration.seconds(0.5), container);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);
            fadeOut.setDelay(Duration.seconds(2));
            fadeOut.play();

            // Remove after fading
            fadeOut.setOnFinished(e -> centerArea.getChildren().remove(notificationPane));
        } catch (Exception e) {
            // If images fail to load, just use a text notification
            showNotification("Drew " + drawnCard.toString(), Color.BLACK);
        }
    }

    /**
     * Prompt user if they want to play the drawn card
     */
    private boolean showPlayDrawnCardPrompt(Card drawnCard) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Play Drawn Card");
        alert.setHeaderText("You drew a playable card");
        alert.setContentText("Do you want to play the " + drawnCard.toString() + "?");

        return alert.showAndWait().orElse(null) == javafx.scene.control.ButtonType.OK;
    }

    /**
     * Handle the Call UNO button click
     */
    private void handleCallUno() {
        // If the player needs to call UNO, mark it as done
        isUnoCallNeeded = false;
        showNotification("UNO!", Color.PURPLE);
    }

    /**
     * Handle the Challenge button click
     */
    private void handleChallenge() {
        // Find players with one card who haven't called UNO
        boolean challengeSuccessful = false;

        // In a real implementation, you would track which players have called UNO
        // For now, we'll simulate a 50% chance of successful challenge
        if (Math.random() < 0.5) {
            // Challenge successful
            challengeSuccessful = true;

            // Find the first opponent with one card
            for (int i = 1; i < game.getPlayers().size(); i++) {
                Player opponent = game.getPlayers().get(i);
                if (opponent.getHand().size() == 1) {
                    // Penalize with two cards
                    opponent.addCard(game.drawCard());
                    opponent.addCard(game.drawCard());

                    showNotification("Challenge Successful!", Color.GREEN);
                    break;
                }
            }
        } else {
            // Challenge failed
            // Penalize the challenger
            game.getPlayers().get(0).addCard(game.drawCard());
            game.getPlayers().get(0).addCard(game.drawCard());

            showNotification("Challenge Failed!", Color.RED);
        }

        updateUI();
    }

    /**
     * Check game status for UNO or win conditions
     */
    private void checkGameStatus() {
        for (Player player : game.getPlayers()) {
            if (player.hasWon()) {
                // Game over
                showGameOverAlert(player);
                return;
            } else if (player.getHand().size() == 1) {
                // Player has UNO
                if (player == game.getPlayers().get(0) && isUnoCallNeeded) {
                    // Human player didn't call UNO - penalize (only if they didn't press the UNO button)
                    if (Math.random() < 0.3) { // 30% chance a computer player will "catch" them
                        player.addCard(game.drawCard());
                        player.addCard(game.drawCard());
                        showNotification("Failed to call UNO!", Color.RED);
                    }
                    isUnoCallNeeded = false;
                } else if (player != game.getPlayers().get(0)) {
                    // Computer player - randomly decides whether to "forget" to call UNO
                    if (Math.random() < 0.7) { // 70% chance they call UNO correctly
                        showNotification(player.getName() + " calls UNO!", Color.PURPLE);
                    }
                }
            }
        }
    }

    /**
     * Show game over alert
     */
    private void showGameOverAlert(Player winner) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Game Over");
        alert.setHeaderText(winner.getName() + " wins!");
        alert.setContentText("Round " + currentRound + " finished.");
        alert.showAndWait();

        // Start a new game or round
        startNewRound();
    }

    /**
     * Start a new round
     */
    private void startNewRound() {
        currentRound++;
        gameRoundLabel.setText("Round " + currentRound);

        // Start a new game with the same number of players
        game = new Game(game.getPlayers().size());
        isUnoCallNeeded = false;
        hasDrawnCard = false;
        updateUI();
    }

    /**
     * Check and start computer player turns
     */
    private void checkAndStartComputerTurn() {
        Player currentPlayer = game.getCurrentPlayer();

        if (currentPlayer != null && currentPlayer.isComputer()) { // Fixed: Added null check
            // Update status immediately to show whose turn it is
            Platform.runLater(() -> {
                statusLabel.setText(currentPlayer.getName() + " is thinking...");
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

    /**
     * Execute a computer player's turn
     */
    private void playComputerTurn() {
        Player computer = game.getCurrentPlayer();
        if (computer == null || !computer.isComputer()) { // Fixed: Added null check
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

            // Show notification for special cards
            showCardActionNotification(selectedCard);

            // Play the card
            game.playCard(cardToPlay);

            // Check for UNO
            if (computer.getHand().size() == 1) {
                if (Math.random() < 0.7) { // 70% chance computer remembers to call UNO
                    showNotification(computer.getName() + " calls UNO!", Color.PURPLE);
                }
            }
        } else {
            // No valid move, draw a card
            game.drawCardForPlayer();
        }

        updateUI();
        checkGameStatus();

        // Check if it's still computer's turn
        checkAndStartComputerTurn();
    }

    /**
     * Clean up resources when the game is closed
     */
    public void shutdown() {
        computerPlayerTimer.shutdown();
    }
}