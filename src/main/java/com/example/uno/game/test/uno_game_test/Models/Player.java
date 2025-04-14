package com.example.uno.game.test.uno_game_test.Models;

import java.util.ArrayList;
import java.util.List;

public class Player {
    private String name;
    private List<Card> hand;
    private boolean isComputer;

    public Player(String name, boolean isComputer) {
        this.name = name;
        this.isComputer = isComputer;
        this.hand = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public List<Card> getHand() {
        return hand;
    }

    public boolean isComputer() {
        return isComputer;
    }

    public void addCard(Card card) {
        hand.add(card);
    }

    public Card playCard(int index) {
        if (index >= 0 && index < hand.size()) {
            return hand.remove(index);
        }
        return null;
    }

    public boolean hasValidMove(Card topCard) {
        for (Card card : hand) {
            if (card.canPlayOn(topCard)) {
                return true;
            }
        }
        return false;
    }

    // For computer AI to select a card
    public int selectCardToPlay(Card topCard) {
        if (!isComputer) {
            return -1;
        }

        for (int i = 0; i < hand.size(); i++) {
            Card card = hand.get(i);
            if (card.canPlayOn(topCard)) {
                return i;
            }
        }

        return -1; // No valid move
    }

    public boolean hasUno() {
        return hand.size() == 1;
    }

    public boolean hasWon() {
        return hand.isEmpty();
    }
}