package com.findtheblock.game;

/**
 * Lifecycle states of a FindTheBlock match.
 */
public enum GameState {
    LOBBY("Waiting in Lobby"),
    STARTING("Selecting Target"),
    IN_GAME("Match in Progress"),
    CELEBRATING("Celebrating Winner"),
    RESETTING("Cleaning Worlds");

    private final String display;

    GameState(String display) {
        this.display = display;
    }

    public String getDisplay() {
        return display;
    }
}
