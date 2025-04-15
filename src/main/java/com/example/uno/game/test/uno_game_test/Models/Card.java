package com.example.uno.game.test.uno_game_test.Models;

/**
 * Card class for Uno game
 */
public class Card {
    private final Color color;
    private final Type type;
    private final int number; // -1 for action cards

    public enum Color {
        RED, BLUE, GREEN, YELLOW, WILD
    }

    public enum Type {
        NUMBER, SKIP, REVERSE, DRAW_TWO, WILD, DRAW_FOUR
    }

    public Card(Color color, Type type, int number) {
        this.color = color;
        this.type = type;
        this.number = number;
    }

    // Getters
    public Color getColor() {
        return color;
    }

    public Type getType() {
        return type;
    }

    public int getNumber() {
        return number;
    }

    @Override
    public String toString() {
        if (type == Type.NUMBER) {
            return color + " " + number;
        } else {
            return color + " " + type;
        }
    }

    public String getImagePath() {
        String colorStr = color.toString().toLowerCase();

        if (type == Type.NUMBER) {
            return "/images/cards/" + colorStr + "_" + number + ".png";
        } else {
            return "/images/cards/" + colorStr + "_" + type.toString().toLowerCase() + ".png";
        }
    }

    // Check if this card can be played on top of another card
    public boolean canPlayOn(Card other) {
        // Wild cards can always be played
        if (this.color == Color.WILD) {
            return true;
        }

        // Same color or same number/action
        return this.color == other.color ||
                (this.type == Type.NUMBER && other.type == Type.NUMBER && this.number == other.number) ||
                (this.type == other.type && this.type != Type.NUMBER);
    }
}