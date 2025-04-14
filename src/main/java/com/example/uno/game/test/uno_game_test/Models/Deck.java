package com.example.uno.game.test.uno_game_test.Models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Deck {
    private List<Card> cards;
    private List<Card> discardPile;

    public Deck() {
        cards = new ArrayList<>();
        discardPile = new ArrayList<>();
        initializeDeck();
        shuffle();
    }

    private void initializeDeck() {
        // Add number cards (0-9) for each color
        for (Card.Color color : new Card.Color[] {Card.Color.RED, Card.Color.BLUE, Card.Color.GREEN, Card.Color.YELLOW}) {
            // Each color has one 0 card
            cards.add(new Card(color, Card.Type.NUMBER, 0));

            // Each color has two of each number 1-9
            for (int number = 1; number <= 9; number++) {
                cards.add(new Card(color, Card.Type.NUMBER, number));
                cards.add(new Card(color, Card.Type.NUMBER, number));
            }

            // Each color has two of each action card
            cards.add(new Card(color, Card.Type.SKIP, -1));
            cards.add(new Card(color, Card.Type.SKIP, -1));
            cards.add(new Card(color, Card.Type.REVERSE, -1));
            cards.add(new Card(color, Card.Type.REVERSE, -1));
            cards.add(new Card(color, Card.Type.DRAW_TWO, -1));
            cards.add(new Card(color, Card.Type.DRAW_TWO, -1));
        }

        // Add wild cards (4 of each)
        for (int i = 0; i < 4; i++) {
            cards.add(new Card(Card.Color.WILD, Card.Type.WILD, -1));
            cards.add(new Card(Card.Color.WILD, Card.Type.WILD_DRAW_FOUR, -1));
        }
    }

    public void shuffle() {
        Collections.shuffle(cards);
    }

    public Card drawCard() {
        if (cards.isEmpty()) {
            // If deck is empty, shuffle the discard pile (except the top card)
            // and make it the new deck
            if (discardPile.size() <= 1) {
                return null; // Game over, no more cards
            }

            Card topCard = discardPile.remove(discardPile.size() - 1);
            cards = new ArrayList<>(discardPile);
            discardPile.clear();
            discardPile.add(topCard);
            shuffle();
        }

        return cards.remove(cards.size() - 1);
    }

    public void discard(Card card) {
        discardPile.add(card);
    }

    public Card getTopDiscard() {
        if (discardPile.isEmpty()) {
            return null;
        }
        return discardPile.get(discardPile.size() - 1);
    }

    public int getRemainingCards() {
        return cards.size();
    }
}