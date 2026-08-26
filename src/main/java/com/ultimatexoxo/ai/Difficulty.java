package com.ultimatexox.ai;

/**
 * AI difficulty levels for Version 1.
 *
 * Each level only changes three simple search parameters:
 *  - maxDepth:   how many moves ahead Minimax may look (upper bound)
 *  - timeBudget: soft limit in milliseconds for iterative deepening
 *  - randomness: chance of playing a random legal move instead of the best one
 *
 * The levels behave differently because depth/time/randomness differ.
 */
public enum Difficulty {

    EASY("Easy", "Plays quickly with a shallow look-ahead and makes mistakes."),
    MEDIUM("Medium", "Looks a few moves ahead and punishes obvious errors."),
    HARD("Hard", "Searches deeper within a larger time budget. Tough to beat."),
    IMPOSSIBLE("Impossible", "Strongest local Minimax search. Very hard to beat.");

    private final String label;
    private final String description;

    Difficulty(String label, String description) {
        this.label = label;
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return label;
    }

    /** Maximum search depth (plies). Iterative deepening never exceeds this. */
    public int getMaxDepth() {
        return switch (this) {
            case EASY -> 2;
            case MEDIUM -> 4;
            case HARD -> 6;
            case IMPOSSIBLE -> 12; // effectively bounded by the time budget
        };
    }

    /** Soft time budget per move in milliseconds. */
    public long getTimeBudgetMs() {
        return switch (this) {
            case EASY -> 100;
            case MEDIUM -> 500;
            case HARD -> 1500;
            case IMPOSSIBLE -> 3000;
        };
    }

    /** Probability [0..1] of playing a random legal move (used for weaker levels). */
    public double getRandomMoveChance() {
        return switch (this) {
            case EASY -> 0.30;
            case MEDIUM -> 0.05;
            case HARD -> 0.0;
            case IMPOSSIBLE -> 0.0;
        };
    }
}
