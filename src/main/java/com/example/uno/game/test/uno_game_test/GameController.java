package com.example.uno.game.test.uno_game_test;

import com.example.uno.game.test.uno_game_test.Models.*;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import java.net.URL;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class GameController implements Initializable {
    // Main UI components
    @FXML private VBox gamePane;
    @FXML private HBox playerHand;
    @FXML private ScrollPane yourHandScrollPane;
    @FXML private GridPane opponentArea;
    @FXML private Label statusLabel;

    // Game table components
    @FXML private ImageView discardPileView;
    @FXML private ImageView deckView;
    @FXML private Label deckCountLabel;

    // Game info components
    @FXML private Label currentPlayerLabel;
    @FXML private Label directionIndicator;
    @FXML private Label cardsLeftCount;

    // Status overlay
    @FXML private VBox statusOverlay;
    @FXML private Label statusOverlayLabel;

    // Action buttons
    @FXML private Button drawCardBtn;
    @FXML private Button unoButton;
    @FXML private Button skipButton;
    @FXML private Button challengeButton;
    @FXML private Button settingsBtn;
    @FXML private Button helpBtn;

    // Opponent components - now extended for up to 8 players
    @FXML private HBox opponent1Hand;
    @FXML private HBox opponent2Hand;
    @FXML private HBox opponent3Hand;
    @FXML private HBox opponent4Hand;
    @FXML private HBox opponent5Hand;
    @FXML private HBox opponent6Hand;
    @FXML private HBox opponent7Hand;
    @FXML private HBox opponent8Hand;

    @FXML private Label opponent1CardCount;
    @FXML private Label opponent2CardCount;
    @FXML private Label opponent3CardCount;
    @FXML private Label opponent4CardCount;
    @FXML private Label opponent5CardCount;
    @FXML private Label opponent6CardCount;
    @FXML private Label opponent7CardCount;
    @FXML private Label opponent8CardCount;

    // Game state
    private Game game;
    private final ScheduledExecutorService computerPlayerTimer = Executors.newSingleThreadScheduledExecutor();
    private final Map<Player, Boolean> playerUnoCallMap = new HashMap<>();
    private final Map<Player, Boolean> playerSkipAllowedMap = new HashMap<>();
    private int deckCount;
    private Card lastDrawnCard;
    private boolean canPlayDrawnCard = false;

    // Card images cache
    private final Map<String, Image> cardImageCache = new HashMap<>();
    private Image cardBackImage;

    // Arrays to store opponent UI elements
    private HBox[] opponentHands;
    private Label[] opponentCardCounts;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialize arrays for opponent UI components
        initializeOpponentArrays();

        // Initialize game with default 4 players (1 human + 3 computer)
        game = new Game(4);
        deckCount = calculateDeckCount();

        // Load card images
        loadCardBackImage();

        // Initialize UI components
        setupUIComponents();

        // Initial UI update
        updateUI();

        // Start computer player turns if needed
        checkAndStartComputerTurn();
    }

    private void initializeOpponentArrays() {
        // Initialize arrays for opponent hands and card counts
        opponentHands = new HBox[]{
                opponent1Hand, opponent2Hand, opponent3Hand, opponent4Hand,
                opponent5Hand, opponent6Hand, opponent7Hand, opponent8Hand
        };

        opponentCardCounts = new Label[]{
                opponent1CardCount, opponent2CardCount, opponent3CardCount, opponent4CardCount,
                opponent5CardCount, opponent6CardCount, opponent7CardCount, opponent8CardCount
        };

        // Initialize each hand container
        for (HBox hand : opponentHands) {
            if (hand != null) {
                hand.setSpacing(2);
                hand.setAlignment(Pos.CENTER);
            }
        }
    }

    private int calculateDeckCount() {
        // Calculate remaining cards in deck: 108 total - cards in hands - 1 discard
        int cardsInHands = 0;
        for (Player player : game.getPlayers()) {
            cardsInHands += player.getHand().size();
        }
        return 108 - cardsInHands - 1;
    }

    private void loadCardBackImage() {
        try {
            cardBackImage = new Image(getClass().getResourceAsStream("/images/cards/card_back.png"));
        } catch (Exception e) {
            System.err.println("Error loading card back image: " + e.getMessage());
            cardBackImage = null;
        }
    }

    private void setupUIComponents() {
        // Configure scrollPane for the player's hand
        yourHandScrollPane.setFitToWidth(true);
        yourHandScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        yourHandScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        // Initialize direction label
        directionIndicator.setText("Clockwise");

        // Set up deckView as a button for drawing cards
        deckView.setOnMouseClicked(event -> handleDrawCard());

        // Add hover effect to deckView
        deckView.setOnMouseEntered(e -> {
            if (!isDeckViewDisabled()) {
                ScaleTransition st = new ScaleTransition(Duration.millis(100), deckView);
                st.setToY(1.1);
                st.setToX(1.1);
                st.play();
            }
        });

        deckView.setOnMouseExited(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(100), deckView);
            st.setToY(1.0);
            st.setToX(1.0);
            st.play();
        });

        // Hide the old draw card button
        drawCardBtn.setVisible(false);
        drawCardBtn.setManaged(false);

        // Set up other button handlers
        unoButton.setOnAction(event -> callUno());
        skipButton.setOnAction(event -> skipTurn());
        challengeButton.setOnAction(event -> challengeUno());

        // Set up settings and help buttons
        settingsBtn.setOnAction(event -> showSettingsDialog());
        helpBtn.setOnAction(event -> showHelpDialog());

        // Initialize status overlay (hidden by default)
        statusOverlay.setVisible(false);

        // Initially disable skip button (only enabled after draw-two or draw-four)
        skipButton.setDisable(true);

        // Initialize UNO call map for all players
        for (Player player : game.getPlayers()) {
            playerUnoCallMap.put(player, false);
            playerSkipAllowedMap.put(player, false);
        }
    }

    public void startGame(int numPlayers) {
        // Clean up any existing game
        if (game != null) {
            game = null;
        }

        // Clear state maps
        playerUnoCallMap.clear();
        playerSkipAllowedMap.clear();

        // Create new game with specified players (limit between 2-8 players for UI)
        game = new Game(Math.max(2, Math.min(numPlayers, 8)));

        // Initialize UNO call map for all players
        for (Player player : game.getPlayers()) {
            playerUnoCallMap.put(player, false);
            playerSkipAllowedMap.put(player, false);
        }

        // Reset game state
        deckCount = calculateDeckCount();
        lastDrawnCard = null;
        canPlayDrawnCard = false;

        // Update UI to reflect the new player count
        updateOpponentVisibility();

        // Update UI with new game state
        updateUI();

        // Show start game notification
        showOverlayMessage("GAME STARTS!", 2000);

        // Start computer turns if needed
        checkAndStartComputerTurn();
    }

    /**
     * Updates visibility of opponent containers based on player count
     */
    private void updateOpponentVisibility() {
        int opponentCount = game.getPlayers().size() - 1; // -1 because one player is human

        // Show only active opponents, hide the rest
        for (int i = 0; i < opponentHands.length; i++) {
            boolean visible = i < opponentCount;

            // Find the parent container in the GridPane (StackPane or VBox)
            int rowIndex = i / 4; // 0 for first row, 1 for second row
            int colIndex = i % 4; // 0-3 for the columns

            // Find the parent VBox in the GridPane
            opponentArea.getChildren().stream()
                    .filter(node -> GridPane.getRowIndex(node) == rowIndex && GridPane.getColumnIndex(node) == colIndex)
                    .findFirst()
                    .ifPresent(container -> {
                        container.setVisible(visible);
                        container.setManaged(visible);
                    });
        }
    }

    private void updateUI() {
        // Update player's hand
        updatePlayerHand();

        // Update opponents' area
        updateOpponentsArea();

        // Update discard pile
        updateDiscardPile();

        // Update deck
        updateDeck();

        // Update game info (current player, direction, cards left)
        updateGameInfo();

        // Update action buttons and deck clickability
        updateActionControls();
    }

    private void updatePlayerHand() {
        playerHand.getChildren().clear();
        Player humanPlayer = game.getPlayers().getFirst();

        for (int i = 0; i < humanPlayer.getHand().size(); i++) {
            Card card = humanPlayer.getHand().get(i);
            final int cardIndex = i;

            // Create card view with a wrapper to manage interactions better
            StackPane cardContainer = new StackPane();
            cardContainer.setPrefSize(90, 130); // Fixed container size to prevent layout shifts
            cardContainer.setMinSize(90, 130);  // Ensure container stays same size even when card scales

            ImageView cardView = createCardImageView(card);
            cardContainer.getChildren().add(cardView);

            // Set the alignment to center to keep the card centered during scaling
            StackPane.setAlignment(cardView, Pos.CENTER);

            // Adjust hover behavior to prevent layout shifts
            cardContainer.setOnMouseEntered(e -> {
                // Prevent event from being passed to the scroll pane
                e.consume();

                ScaleTransition st = new ScaleTransition(Duration.millis(100), cardView);
                st.setToY(1.1);
                st.setToX(1.1);
                st.play();

                // Use the toFront() on the container instead of just the card
                cardContainer.toFront();
            });

            cardContainer.setOnMouseExited(e -> {
                // Prevent event from being passed to the scroll pane
                e.consume();

                ScaleTransition st = new ScaleTransition(Duration.millis(100), cardView);
                st.setToY(1.0);
                st.setToX(1.0);
                st.play();
            });

            // Add click event for playing this card
            cardContainer.setOnMouseClicked(e -> {
                e.consume(); // Prevent click from propagating to scroll pane
                handleCardClick(cardIndex);
            });

            // Set margin to ensure proper spacing
            HBox.setMargin(cardContainer, new javafx.geometry.Insets(0, -5, 0, -5)); // Tighter margin to accommodate the fixed container size

            // Add to player hand
            playerHand.getChildren().add(cardContainer);
        }

        // Ensure ScrollPane shows the hand correctly
        playerHand.setSpacing(0); // Use container margins instead of spacing

        // Disable the automatic scrolling during hover to prevent jumps
        yourHandScrollPane.setPannable(true); // Allow manual panning
        yourHandScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED); // Show scrollbar when needed

        // Reset scroll position to the start
        Platform.runLater(() -> yourHandScrollPane.setHvalue(0));
    }

    private void updateOpponentsArea() {
        // Clear all opponent hands
        for (HBox hand : opponentHands) {
            if (hand != null) {
                hand.getChildren().clear();
            }
        }

        // Get total opponent count
        int opponentCount = game.getPlayers().size() - 1; // Exclude human player

        // Update each visible opponent
        for (int i = 0; i < opponentCount; i++) {
            Player opponent = game.getPlayers().get(i + 1); // +1 to skip human player

            if (i < opponentHands.length && opponentHands[i] != null && opponentCardCounts[i] != null) {
                updateOpponentHandUI(opponent, opponentHands[i], opponentCardCounts[i]);
            }
        }
    }

    private void updateOpponentHandUI(Player opponent, HBox opponentHand, Label opponentCardCount) {
        // Add card backs for each card in opponent's hand (max 7 shown)
        for (int j = 0; j < Math.min(opponent.getHand().size(), 7); j++) {
            ImageView cardBack = new ImageView(cardBackImage);
            cardBack.setFitHeight(40); // Smaller cards for opponents
            cardBack.setFitWidth(28);
            cardBack.setPreserveRatio(true);

            // Offset cards slightly to show depth
            cardBack.setTranslateX(-j * 8);

            // Highlight current player
            if (game.getCurrentPlayer() == opponent) {
                cardBack.setStyle("-fx-effect: dropshadow(three-pass-box, rgba(255,215,0,0.8), 10, 0, 0, 0);");
            }

            opponentHand.getChildren().add(cardBack);
        }

        // If there are more cards than we show, add a counter
        if (opponent.getHand().size() > 7) {
            Label moreCards = new Label("+" + (opponent.getHand().size() - 7));
            moreCards.setStyle("-fx-text-fill: white; -fx-background-color: rgba(0,0,0,0.5); -fx-padding: 2px 5px; -fx-background-radius: 3px;");
            opponentHand.getChildren().add(moreCards);
        }

        // Update card count
        if (opponentCardCount != null) {
            opponentCardCount.setText(opponent.getHand().size() + " cards");

            // Highlight if player has UNO
            if (opponent.getHand().size() == 1) {
                if (playerUnoCallMap.getOrDefault(opponent, false)) {
                    opponentCardCount.setStyle("-fx-text-fill: #ff4500; -fx-font-weight: bold;");
                    opponentCardCount.setText("UNO!");
                } else {
                    opponentCardCount.setStyle("-fx-text-fill: yellow; -fx-font-weight: bold;");
                    opponentCardCount.setText("1 card (no UNO!)");
                }
            } else {
                opponentCardCount.setStyle("-fx-text-fill: white;");
            }
        }
    }

    private void updateDiscardPile() {
        Card topCard = game.getTopCard();
        discardPileView.setImage(getCardImage(topCard));
    }

    private void updateDeck() {
        // Set deck image to card back
        deckView.setImage(cardBackImage);

        // Update deck count
        deckCountLabel.setText(String.valueOf(deckCount));

        // Update visual state of the deck based on whether it's clickable
        if (isDeckViewDisabled()) {
            deckView.setOpacity(0.7);
            deckView.setEffect(null);
            deckView.setCursor(javafx.scene.Cursor.DEFAULT);
        } else {
            deckView.setOpacity(1.0);
            deckView.setCursor(javafx.scene.Cursor.HAND);

            // Add subtle glow effect when it's the player's turn
            if (game.getCurrentPlayer() == game.getPlayers().getFirst()) {
                javafx.scene.effect.DropShadow glow = new javafx.scene.effect.DropShadow();
                glow.setColor(Color.GOLD);
                glow.setWidth(20);
                glow.setHeight(20);
                deckView.setEffect(glow);
            } else {
                deckView.setEffect(null);
            }
        }
    }

    private boolean isDeckViewDisabled() {
        Player currentPlayer = game.getCurrentPlayer();
        boolean isHumanTurn = (currentPlayer == game.getPlayers().getFirst());
        return !isHumanTurn || canPlayDrawnCard;
    }

    private void updateGameInfo() {
        // Update current player
        Player currentPlayer = game.getCurrentPlayer();
        currentPlayerLabel.setText(currentPlayer.getName());

        // Highlight if it's the human player's turn
        if (currentPlayer == game.getPlayers().getFirst()) {
            currentPlayerLabel.setStyle("-fx-text-fill: #FF9500; -fx-font-weight: bold;");
        } else {
            currentPlayerLabel.setStyle("-fx-text-fill: white;");
        }

        // Update direction indicator
        directionIndicator.setText(game.isClockwise() ? "Clockwise" : "Counter Clockwise");

        // Update cards left count
        int totalCardsInHands = game.getPlayers().stream()
                .mapToInt(player -> player.getHand().size())
                .sum();
        cardsLeftCount.setText(String.valueOf(totalCardsInHands));

        // Update status text
        if (currentPlayer == game.getPlayers().getFirst()) {
            statusLabel.setText(canPlayDrawnCard ?
                    "Play the card you just drew or pass your turn." :
                    "Your turn! Play a card or click the deck to draw.");
        } else {
            statusLabel.setText(currentPlayer.getName() + "'s turn...");
        }
    }

    private void updateActionControls() {
        Player currentPlayer = game.getCurrentPlayer();
        boolean isHumanTurn = (currentPlayer == game.getPlayers().getFirst());

        // Skip button (only enabled when allowed to skip)
        skipButton.setDisable(!isHumanTurn || !playerSkipAllowedMap.getOrDefault(currentPlayer, false));

        // UNO button is enabled when human has 2 cards (about to play second-to-last card)
        unoButton.setDisable(game.getPlayers().getFirst().getHand().size() != 2);

        // Challenge button is enabled when any opponent has one card but didn't call UNO
        boolean canChallenge = game.getPlayers().stream()
                .anyMatch(player -> player != game.getPlayers().getFirst() && // Not human
                        player.getHand().size() == 1 &&                // Has one card
                        !playerUnoCallMap.getOrDefault(player, false)); // Didn't call UNO
        challengeButton.setDisable(!canChallenge);

        // Update deck visual state
        updateDeck();
    }

    @FXML
    private void handleCardClick(int cardIndex) {
        Player humanPlayer = game.getPlayers().getFirst();

        if (game.getCurrentPlayer() != humanPlayer) {
            // Not player's turn
            showOverlayMessage("NOT YOUR TURN", 1000);
            return;
        }

        Card card = humanPlayer.getHand().get(cardIndex);

        // Check if card can be played
        if (!canPlayCard(card)) {
            showOverlayMessage("INVALID MOVE", 1000);
            return;
        }

        // Check UNO call for second-to-last card
        if (humanPlayer.getHand().size() == 2 &&
                !playerUnoCallMap.getOrDefault(humanPlayer, false)) {
            // Player is about to have 1 card but didn't call UNO
            showOverlayMessage("FORGOT TO CALL UNO!", 1500);
            // Penalty: draw 2 cards
            for (int i = 0; i < 2; i++) {
                drawCard(humanPlayer);
            }
            // Reset ability to play drawn card
            canPlayDrawnCard = false;
            updateUI();
            return;
        }

        if (card.getColor() == Card.Color.WILD) {
            // For wild cards, prompt for color selection
            showColorSelectionDialog(card.getType(), () -> {
                // After color selection callback
                playCard(cardIndex);
                handleCardEffects(card);
                // Reset ability to play drawn card
                canPlayDrawnCard = false;
            });
        } else {
            // Regular card, play directly
            playCard(cardIndex);
            handleCardEffects(card);
            // Reset ability to play drawn card
            canPlayDrawnCard = false;
        }
    }

    private boolean canPlayCard(Card card) {
        Card topCard = game.getTopCard();
        Card.Color currentColor = game.getCurrentColor();

        // Wild cards can always be played
        if (card.getColor() == Card.Color.WILD) {
            return true;
        }

        // Match by color
        if (card.getColor() == currentColor) {
            return true;
        }

        // Match by number or type
        if (card.getType() == Card.Type.NUMBER) {
            return topCard.getType() == Card.Type.NUMBER && card.getNumber() == topCard.getNumber();
        } else {
            return card.getType() == topCard.getType();
        }
    }

    private void playCard(int cardIndex) {
        if (!game.playCard(cardIndex)) {
            // This should not happen as we already checked if the card can be played
            System.err.println("Failed to play card at index " + cardIndex);
            return;
        }

        // Update UI
        updateUI();

        // Check for game end
        if (checkGameStatus()) {
            return; // Game ended
        }

        // Start computer turns if needed
        checkAndStartComputerTurn();
    }

    private void handleCardEffects(Card card) {
        // Show overlay message based on card type
        switch (card.getType()) {
            case SKIP:
                showOverlayMessage("SKIP", 1500);
                break;
            case REVERSE:
                showOverlayMessage("REVERSE", 1500);
                break;
            case DRAW_TWO:
                showOverlayMessage("+2 CARDS", 1500);
                break;
            case DRAW_FOUR:
                showOverlayMessage("+4 CARDS", 1500);
                break;
        }

        // Update player UNO status if needed
        Player currentPlayer = game.getCurrentPlayer();
        if (currentPlayer.getHand().size() == 1 && playerUnoCallMap.getOrDefault(currentPlayer, false)) {
            showOverlayMessage("UNO!", 1500);
        }
    }

    @FXML
    public void handleDrawCard() {
        Player humanPlayer = game.getPlayers().getFirst();

        if (game.getCurrentPlayer() != humanPlayer) {
            // Not player's turn
            showOverlayMessage("NOT YOUR TURN", 1000);
            return;
        }

        if (canPlayDrawnCard) {
            // Already drew a card, must skip turn
            showOverlayMessage("PASS TURN", 1000);
            playerSkipAllowedMap.put(humanPlayer, false);
            canPlayDrawnCard = false;
            game.nextPlayer();
            updateUI();
            checkAndStartComputerTurn();
            return;
        }

        // Draw a card
        lastDrawnCard = drawCard(humanPlayer);
        deckCount--;

        // Show what was drawn
        showDrawnCardNotification(lastDrawnCard);

        // Check if drawn card can be played
        if (canPlayCard(lastDrawnCard)) {
            // Allow player to play the drawn card
            canPlayDrawnCard = true;
            updateUI();
        } else {
            // Card can't be played, move to next player
            game.nextPlayer();
            // Reset for next turn
            canPlayDrawnCard = false;
            updateUI();
            checkAndStartComputerTurn();
        }
    }

    private Card drawCard(Player player) {
        Card card = new Deck().drawCard(); // We don't have access to the actual deck, so create a temporary one
        player.addCard(card);
        return card;
    }

    private void showDrawnCardNotification(Card card) {
        String cardDesc = cardDescription(card);
        showOverlayMessage("DREW " + cardDesc, 1500);
    }

    private String cardDescription(Card card) {
        if (card.getType() == Card.Type.NUMBER) {
            return card.getColor() + " " + card.getNumber();
        } else {
            return card.getColor() + " " + card.getType().toString().replace("_", " ");
        }
    }

    @FXML
    public void callUno() {
        Player humanPlayer = game.getPlayers().getFirst();

        // Only allow UNO call when player has exactly 2 cards
        if (humanPlayer.getHand().size() == 2) {
            playerUnoCallMap.put(humanPlayer, true);
            showOverlayMessage("UNO!", 1500);
        } else {
            showOverlayMessage("TOO EARLY FOR UNO", 1500);
        }
    }

    @FXML
    public void skipTurn() {
        Player currentPlayer = game.getCurrentPlayer();

        if (currentPlayer != game.getPlayers().getFirst()) {
            // Not human's turn
            return;
        }

        if (!playerSkipAllowedMap.getOrDefault(currentPlayer, false)) {
            showOverlayMessage("CAN'T SKIP NOW", 1000);
            return;
        }

        // Skip the turn
        playerSkipAllowedMap.put(currentPlayer, false);
        game.nextPlayer();
        updateUI();

        // Start computer turns if needed
        checkAndStartComputerTurn();
    }

    @FXML
    public void challengeUno() {
        // Find an opponent with one card who didn't call UNO
        for (Player opponent : game.getPlayers()) {
            if (opponent != game.getPlayers().getFirst() &&
                    opponent.getHand().size() == 1 &&
                    !playerUnoCallMap.getOrDefault(opponent, false)) {

                // Challenge successful
                showOverlayMessage("CHALLENGE SUCCESS!", 1500);

                // Penalize the challenged player with 2 cards
                for (int i = 0; i < 2; i++) {
                    drawCard(opponent);
                    deckCount--;
                }

                updateUI();
                return;
            }
        }

        // If we got here, challenge failed (shouldn't happen with proper button enabling)
        showOverlayMessage("CHALLENGE FAILED", 1500);
    }

    private void showColorSelectionDialog(Card.Type wildType, Runnable onComplete) {
        Dialog<Card.Color> dialog = new Dialog<>();
        dialog.setTitle("Choose a Color");
        dialog.setHeaderText("Select a color for the " + wildType.toString().replace("_", " ") + " card");

        // Create color buttons with better styling
        Button redButton = createColorButton("RED", "#EC5D66", Card.Color.RED, dialog);
        Button blueButton = createColorButton("BLUE", "#3B6EEE", Card.Color.BLUE, dialog);
        Button greenButton = createColorButton("GREEN", "#3BCD80", Card.Color.GREEN, dialog);
        Button yellowButton = createColorButton("YELLOW", "#F4BE67", Card.Color.YELLOW, dialog);

        // Create a 2x2 grid of color buttons
        HBox topRow = new HBox(10, redButton, blueButton);
        HBox bottomRow = new HBox(10, greenButton, yellowButton);
        VBox colorGrid = new VBox(10, topRow, bottomRow);
        colorGrid.setAlignment(Pos.CENTER);

        dialog.getDialogPane().setContent(colorGrid);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);

        // Remove the default cancel button to force color selection
        Button cancelButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
        cancelButton.setVisible(false);

        // Handle dialog result
        dialog.showAndWait();
        onComplete.run();
    }

    private Button createColorButton(String text, String colorHex, Card.Color cardColor, Dialog<Card.Color> dialog) {
        Button button = new Button(text);
        button.getStyleClass().add("color-button");
        String textColor = "white";
        if (text.equals("YELLOW")) textColor = "black";

        button.setStyle("-fx-background-color: " + colorHex + "; -fx-text-fill: " + textColor +
                "; -fx-font-weight: bold; -fx-min-width: 100px; -fx-min-height: 80px;");
        button.setOnAction(event -> {
            game.setCurrentColor(cardColor);
            dialog.setResult(cardColor);
            dialog.close();
        });
        return button;
    }

    private void checkAndStartComputerTurn() {
        Player currentPlayer = game.getCurrentPlayer();

        if (currentPlayer.isComputer()) {
            playComputerTurn();
        }
    }

    private void playComputerTurn() {
        Player computer = game.getCurrentPlayer();
        if (!computer.isComputer()) {
            return;
        }

        // Update UI to show whose turn it is
        Platform.runLater(() -> {
            statusLabel.setText(computer.getName() + " is thinking...");
            updateUI();
        });

        // Add a slight delay to make the computer's turn visible
        PauseTransition pause = new PauseTransition(Duration.millis(1200));
        pause.setOnFinished(event -> {
            // Determine if computer should call UNO
            if (computer.getHand().size() == 2) {
                // Computer has 70% chance to remember calling UNO
                boolean callsUno = Math.random() < 0.7;
                if (callsUno) {
                    playerUnoCallMap.put(computer, true);
                    showOverlayMessage(computer.getName() + " calls UNO!", 1500);
                } else {
                    playerUnoCallMap.put(computer, false);
                }
            }

            // Find a card to play
            int cardToPlay = selectComputerCardToPlay(computer);

            if (cardToPlay >= 0) {
                Card selectedCard = computer.getHand().get(cardToPlay);

                // For wild cards, choose a color based on cards in hand
                if (selectedCard.getColor() == Card.Color.WILD) {
                    game.setCurrentColor(chooseBestColorForComputer(computer));
                }

                // Play the selected card
                game.playCard(cardToPlay);

                // Show card effect
                handleCardEffects(selectedCard);

            } else {
                // No valid move, draw a card
                Card drawn = drawCard(computer);
                deckCount--;
                showOverlayMessage(computer.getName() + " DRAWS A CARD", 1000);

                // Check if computer can play the drawn card
                if (canPlayCard(drawn)) {
                    // 80% chance computer plays drawn card if possible
                    if (Math.random() < 0.8) {
                        // Get the index of the drawn card (last card in hand)
                        int drawnCardIndex = computer.getHand().size() - 1;

                        // For wild cards, choose a color
                        if (drawn.getColor() == Card.Color.WILD) {
                            game.setCurrentColor(chooseBestColorForComputer(computer));
                        }

                        // Play the drawn card
                        game.playCard(drawnCardIndex);

                        // Show card effect
                        handleCardEffects(drawn);
                    } else {
                        // Computer decides not to play drawn card
                        game.nextPlayer();
                    }
                } else {
                    // Can't play drawn card, move to next player
                    game.nextPlayer();
                }
            }

            updateUI();

            // Check for game end
            if (checkGameStatus()) {
                return; // Game ended
            }

            // Check if it's still computer's turn
            checkAndStartComputerTurn();
        });

        pause.play();
    }

    private int selectComputerCardToPlay(Player computer) {
        List<Card> hand = computer.getHand();
        Card topCard = game.getTopCard();
        Card.Color currentColor = game.getCurrentColor();

        // First try to play a number card matching the color
        for (int i = 0; i < hand.size(); i++) {
            Card card = hand.get(i);
            if (card.getType() == Card.Type.NUMBER && card.getColor() == currentColor) {
                return i;
            }
        }

        // Then try to play a special card matching the color
        for (int i = 0; i < hand.size(); i++) {
            Card card = hand.get(i);
            if (card.getType() != Card.Type.NUMBER && card.getColor() == currentColor) {
                return i;
            }
        }

        // Then try to play a card matching the number/type
        for (int i = 0; i < hand.size(); i++) {
            Card card = hand.get(i);
            if (topCard.getType() == Card.Type.NUMBER && card.getType() == Card.Type.NUMBER) {
                if (card.getNumber() == topCard.getNumber()) {
                    return i;
                }
            } else if (card.getType() == topCard.getType()) {
                return i;
            }
        }

        // Finally try to play a wild card
        for (int i = 0; i < hand.size(); i++) {
            Card card = hand.get(i);
            if (card.getColor() == Card.Color.WILD) {
                return i;
            }
        }

        // No playable card found
        return -1;
    }

    private Card.Color chooseBestColorForComputer(Player computer) {
        // Count cards of each color in hand
        int[] colorCounts = new int[4]; // RED, BLUE, GREEN, YELLOW

        for (Card card : computer.getHand()) {
            if (card.getColor() == Card.Color.RED) colorCounts[0]++;
            else if (card.getColor() == Card.Color.BLUE) colorCounts[1]++;
            else if (card.getColor() == Card.Color.GREEN) colorCounts[2]++;
            else if (card.getColor() == Card.Color.YELLOW) colorCounts[3]++;
        }

        // Find color with highest count
        int maxCount = -1;
        int maxIndex = 0;

        for (int i = 0; i < 4; i++) {
            if (colorCounts[i] > maxCount) {
                maxCount = colorCounts[i];
                maxIndex = i;
            }
        }

        // Map index back to Color enum
        return switch (maxIndex) {
            case 0 -> Card.Color.RED;
            case 1 -> Card.Color.BLUE;
            case 2 -> Card.Color.GREEN;
            default -> Card.Color.YELLOW;
        };
    }

    private boolean checkGameStatus() {
        for (Player player : game.getPlayers()) {
            if (player.getHand().isEmpty()) {
                // Game over, show winner
                showGameOverDialog(player);
                return true;
            }
        }
        return false;
    }

    private void showGameOverDialog(Player winner) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("Game Over");
        alert.setHeaderText(winner.getName() + " Wins!");

        StringBuilder contentText = new StringBuilder("Final scores:\n");
        for (Player player : game.getPlayers()) {
            int score = calculateScore(player);
            contentText.append(player.getName()).append(": ").append(score).append(" points\n");
        }

        alert.setContentText(contentText.toString());

        ButtonType newGameButton = new ButtonType("New Game");
        ButtonType quitButton = new ButtonType("Quit");

        alert.getButtonTypes().setAll(newGameButton, quitButton);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == newGameButton) {
            startGame(game.getPlayers().size());
        } else {
            Platform.exit();
        }
    }

    private int calculateScore(Player player) {
        int score = 0;
        for (Card card : player.getHand()) {
            switch (card.getType()) {
                case NUMBER:
                    score += card.getNumber();
                    break;
                case SKIP:
                case REVERSE:
                case DRAW_TWO:
                    score += 20;
                    break;
                case WILD:
                case DRAW_FOUR:
                    score += 50;
                    break;
            }
        }
        return score;
    }

    private void showOverlayMessage(String message, int durationMs) {
        // Set message text
        statusOverlayLabel.setText(message);

        // Show overlay
        statusOverlay.setVisible(true);
        statusOverlay.setOpacity(0);

        // Fade in
        FadeTransition fadeIn = new FadeTransition(Duration.millis(200), statusOverlay);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.setCycleCount(1);

        // Pause
        PauseTransition pause = new PauseTransition(Duration.millis(durationMs));

        // Fade out
        FadeTransition fadeOut = new FadeTransition(Duration.millis(200), statusOverlay);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        fadeOut.setCycleCount(1);

        // Chain animations
        fadeIn.setOnFinished(e -> pause.play());
        pause.setOnFinished(e -> fadeOut.play());
        fadeOut.setOnFinished(e -> statusOverlay.setVisible(false));

        // Start animation sequence
        fadeIn.play();
    }

    private ImageView createCardImageView(Card card) {
        ImageView cardView = new ImageView();
        cardView.setFitHeight(120);
        cardView.setFitWidth(80);
        cardView.setPreserveRatio(true);
        cardView.setImage(getCardImage(card));

        // Add style class for easier styling via CSS
        cardView.getStyleClass().add("player-card");

        return cardView;
    }

    private Image getCardImage(Card card) {
        String cardKey = cardImageKey(card);

        // Check if image is already loaded in cache
        if (cardImageCache.containsKey(cardKey)) {
            return cardImageCache.get(cardKey);
        }

        // If not loaded, try to load it
        try {
            String imagePath = "/images/cards/" + cardImageFilename(card);
            Image cardImage = new Image(getClass().getResourceAsStream(imagePath));
            cardImageCache.put(cardKey, cardImage);
            return cardImage;
        } catch (Exception e) {
            // If loading fails, create a basic representation
            return createFallbackCardImage(card);
        }
    }

    private String cardImageKey(Card card) {
        if (card.getType() == Card.Type.NUMBER) return card.getColor() + "_" + card.getNumber();
        else return card.getColor() + "_" + card.getType();
    }

    private String cardImageFilename(Card card) {
        String color = card.getColor().toString().toLowerCase();

        if (card.getType() == Card.Type.NUMBER) return color + "_" + card.getNumber() + ".png";
        else return color + "_" + card.getType().toString().toLowerCase() + ".png";
    }

    private Image createFallbackCardImage(Card card) {
        // This is a simple fallback when card images aren't available
        javafx.scene.canvas.Canvas canvas = new javafx.scene.canvas.Canvas(80, 120);
        javafx.scene.canvas.GraphicsContext gc = canvas.getGraphicsContext2D();

        // Card background
        gc.setFill(getCardColor(card.getColor()));
        gc.fillRoundRect(0, 0, 80, 120, 10, 10);

        // Card border
        gc.setStroke(Color.BLACK);
        gc.strokeRoundRect(1, 1, 78, 118, 9, 9);

        // Card text
        gc.setFill(Color.WHITE);
        gc.setTextAlign(javafx.scene.text.TextAlignment.CENTER);

        if (card.getType() == Card.Type.NUMBER) {
            gc.setFont(javafx.scene.text.Font.font("Arial", javafx.scene.text.FontWeight.BOLD, 40));
            gc.fillText(String.valueOf(card.getNumber()), 40, 70);
        } else {
            gc.setFont(javafx.scene.text.Font.font("Arial", javafx.scene.text.FontWeight.BOLD, 16));
            gc.fillText(card.getType().toString(), 40, 70);
        }

        // Convert Canvas to Image
        javafx.scene.image.WritableImage wImage = new javafx.scene.image.WritableImage(80, 120);
        canvas.snapshot(null, wImage);
        return wImage;
    }

    private Color getCardColor(Card.Color cardColor) {
        return switch (cardColor) {
            case RED -> Color.rgb(219, 50, 54);
            case BLUE -> Color.rgb(0, 101, 189);
            case GREEN -> Color.rgb(60, 174, 63);
            case YELLOW -> Color.rgb(254, 231, 21);
            case WILD -> Color.BLACK;
        };
    }

    @FXML
    public void showSettingsDialog() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Settings");
        dialog.setHeaderText("Game Settings");

        // Player count setting
        HBox playerCountBox = new HBox(10);
        playerCountBox.setAlignment(Pos.CENTER);
        Label playerCountLabel = new Label("Number of Players:");
        ComboBox<Integer> playerCountCombo = new ComboBox<>();
        playerCountCombo.getItems().addAll(2, 3, 4, 5, 6, 7, 8); // Extended to 8 players
        playerCountCombo.setValue(game.getPlayers().size());
        playerCountBox.getChildren().addAll(playerCountLabel, playerCountCombo);

        // Sound settings
        HBox soundBox = new HBox(10);
        soundBox.setAlignment(Pos.CENTER);
        Label soundLabel = new Label("Sound Effects:");
        CheckBox soundCheck = new CheckBox();
        soundCheck.setSelected(true);
        soundBox.getChildren().addAll(soundLabel, soundCheck);

        // Animation settings
        HBox animationBox = new HBox(10);
        animationBox.setAlignment(Pos.CENTER);
        Label animationLabel = new Label("Animations:");
        CheckBox animationCheck = new CheckBox();
        animationCheck.setSelected(true);
        animationBox.getChildren().addAll(animationLabel, animationCheck);

        // Organize content
        VBox content = new VBox(15);
        content.setAlignment(Pos.CENTER);
        content.getChildren().addAll(
                new Label("Game Settings"),
                playerCountBox,
                soundBox,
                animationBox
        );
        content.setPadding(new javafx.geometry.Insets(20));

        dialog.getDialogPane().setContent(content);

        // Add buttons
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, cancelButtonType);

        // Handle save button
        Button saveButton = (Button) dialog.getDialogPane().lookupButton(saveButtonType);
        saveButton.setOnAction(e -> {
            int newPlayerCount = playerCountCombo.getValue();
            if (newPlayerCount != game.getPlayers().size()) {
                startGame(newPlayerCount);
            }
        });

        dialog.showAndWait();
    }

    @FXML
    public void showHelpDialog() {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("UNO Help");
        alert.setHeaderText("How to Play UNO");

        String helpText =
                """
                        OBJECTIVE:
                        Be the first player to get rid of all your cards.
                        
                        CARD TYPES:
                        • Number cards (0-9): Match by color or number
                        • Skip: Next player loses their turn
                        • Reverse: Changes direction of play
                        • Draw Two: Next player draws 2 cards and loses their turn
                        • Wild: Change the current color
                        • Wild Draw Four: Next player draws 4 cards, loses their turn, and you change the color
                        
                        GAME PLAY:
                        1. Match the top card by color, number, or symbol
                        2. If you can't play, click on the deck to draw a card
                        3. When you have only one card left, press the UNO button
                        4. If someone forgets to call UNO, use the Challenge button
                        
                        SCORING:
                        • Number cards: Face value
                        • Skip/Reverse/Draw Two: 20 points
                        • Wild/Wild Draw Four: 50 points""";

        alert.setContentText(helpText);
        alert.showAndWait();
    }

    public void shutdown() {
        if (computerPlayerTimer != null) {
            computerPlayerTimer.shutdownNow();
            try {
                computerPlayerTimer.awaitTermination(2, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Method to handle window resizing events
     * This can be called when the window size changes
     */
    public void handleWindowResize() {
        // Adjust card sizes based on the current window size
        adaptCardSizesToWindowSize();

        // Update the UI
        updateUI();
    }

    /**
     * Adapts card and layout sizes based on the current window dimensions
     */
    private void adaptCardSizesToWindowSize() {
        if (gamePane == null) return;

        // Get the current window dimensions
        double width = gamePane.getWidth();
        double height = gamePane.getHeight();

        // Scale card sizes for different screens
        double cardScale = Math.min(width / 900.0, height / 690.0);

        // Adjust player card size
        double playerCardHeight = Math.max(80, Math.min(120, 120 * cardScale));
        double playerCardWidth = playerCardHeight * 0.67; // Maintain 2:3 aspect ratio

        // Adjust opponent card size
        double opponentCardHeight = Math.max(30, Math.min(40, 40 * cardScale));
        double opponentCardWidth = opponentCardHeight * 0.7;

        // Apply new sizes to all cards
        for (javafx.scene.Node node : playerHand.getChildren()) {
            if (node instanceof StackPane container) {
                if (!container.getChildren().isEmpty() && container.getChildren().getFirst() instanceof ImageView cardView) {
                    cardView.setFitHeight(playerCardHeight);
                    cardView.setFitWidth(playerCardWidth);
                }
            }
        }

        // Update spacing based on window size
        playerHand.setSpacing(Math.max(2, Math.min(10, 5 * cardScale)));

        // Update discardPileView and deckView sizes
        double tableCardHeight = Math.max(100, Math.min(160, 160 * cardScale));
        double tableCardWidth = tableCardHeight * 0.6875;

        discardPileView.setFitHeight(tableCardHeight);
        discardPileView.setFitWidth(tableCardWidth);

        deckView.setFitHeight(tableCardHeight);
        deckView.setFitWidth(tableCardWidth);
    }
}