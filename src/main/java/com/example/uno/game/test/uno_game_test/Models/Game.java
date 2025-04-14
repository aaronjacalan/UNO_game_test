package com.example.uno.game.test.uno_game_test.Models;

import java.util.ArrayList;
import java.util.List;

public class Game {
    private Deck deck;
    private List<Player> players;
    private int currentPlayerIndex;
    private boolean isClockwise;
    private Card.Color currentColor;

    public Game(int numPlayers) {
        deck = new Deck();
        players = new ArrayList<>();

        // Create players (1 human, rest computer)
        players.add(new Player("You", false));
        for (int i = 1; i < numPlayers; i++) {
            players.add(new Player("Computer " + i, true));
        }

        // Deal initial cards (7 per player)
        for (int i = 0; i < 7; i++) {
            for (Player player : players) {
                player.addCard(deck.drawCard());
            }
        }

        // Set up initial game state
        currentPlayerIndex = 0;
        isClockwise = true;

        // Place first card from deck
        Card firstCard = deck.drawCard();
        while (firstCard.getColor() == Card.Color.WILD) {
            // First card cannot be a wild card, put it back and draw another
            deck.discard(firstCard);
            deck.shuffle();
            firstCard = deck.drawCard();
        }

        deck.discard(firstCard);
        currentColor = firstCard.getColor();
    }

    public Player getCurrentPlayer() {
        return players.get(currentPlayerIndex);
    }

    public Card getTopCard() {
        return deck.getTopDiscard();
    }

    public Card.Color getCurrentColor() {
        return currentColor;
    }

    public void setCurrentColor(Card.Color color) {
        this.currentColor = color;
    }

    public boolean playCard(int cardIndex) {
        Player player = getCurrentPlayer();
        Card card = player.getHand().get(cardIndex);
        Card topCard = getTopCard();

        if (card.canPlayOn(topCard) || card.getColor() == getCurrentColor()) {
            player.playCard(cardIndex);
            deck.discard(card);

            // Handle special cards
            if (card.getType() != Card.Type.NUMBER) {
                handleSpecialCard(card);
            } else {
                // For number cards, simply update color and move to next player
                currentColor = card.getColor();
                nextPlayer();
            }

            return true;
        }

        return false;
    }

    public Card drawCardForPlayer() {
        Card card = deck.drawCard();
        getCurrentPlayer().addCard(card);
        nextPlayer();
        return card;
    }

    private void handleSpecialCard(Card card) {
        switch (card.getType()) {
            case SKIP:
                currentColor = card.getColor();
                nextPlayer(); // Skip next player
                nextPlayer();
                break;

            case REVERSE:
                currentColor = card.getColor();
                isClockwise = !isClockwise;
                if (players.size() == 2) {
                    // In a 2-player game, reverse acts like skip
                    nextPlayer();
                }
                nextPlayer();
                break;

            case DRAW_TWO:
                currentColor = card.getColor();
                nextPlayer();
                // Next player draws 2 cards
                Player nextPlayer = getCurrentPlayer();
                nextPlayer.addCard(deck.drawCard());
                nextPlayer.addCard(deck.drawCard());
                nextPlayer();
                break;

            case WILD:
                // currentColor will be set by the player
                nextPlayer();
                break;

            case WILD_DRAW_FOUR:
                // currentColor will be set by the player
                nextPlayer();
                // Next player draws 4 cards
                Player drawFourPlayer = getCurrentPlayer();
                for (int i = 0; i < 4; i++) {
                    drawFourPlayer.addCard(deck.drawCard());
                }
                nextPlayer();
                break;
        }
    }

    private void nextPlayer() {
        if (isClockwise) {
            currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
        } else {
            currentPlayerIndex = (currentPlayerIndex - 1 + players.size()) % players.size();
        }
    }

    public boolean isGameOver() {
        for (Player player : players) {
            if (player.hasWon()) {
                return true;
            }
        }
        return false;
    }

    public Player getWinner() {
        for (Player player : players) {
            if (player.hasWon()) {
                return player;
            }
        }
        return null;
    }

    public List<Player> getPlayers() {
        return players;
    }
}