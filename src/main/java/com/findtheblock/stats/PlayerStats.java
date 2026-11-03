package com.findtheblock.stats;

import java.util.UUID;

/**
 * Encapsulates persistent statistics for a single player.
 */
public class PlayerStats {

    private final UUID uuid;
    private String name;
    private int wins;
    private int secondPlaces;
    private int thirdPlaces;
    private int gamesPlayed;
    private int bestTimeSeconds;

    public PlayerStats(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
        this.wins = 0;
        this.secondPlaces = 0;
        this.thirdPlaces = 0;
        this.gamesPlayed = 0;
        this.bestTimeSeconds = 0;
    }

    public PlayerStats(UUID uuid, String name, int wins, int secondPlaces, int thirdPlaces, int gamesPlayed, int bestTimeSeconds) {
        this.uuid = uuid;
        this.name = name;
        this.wins = wins;
        this.secondPlaces = secondPlaces;
        this.thirdPlaces = thirdPlaces;
        this.gamesPlayed = gamesPlayed;
        this.bestTimeSeconds = bestTimeSeconds;
    }

    public UUID getUuid() { return uuid; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getWins() { return wins; }
    public void addWin() { this.wins++; }

    public int getSecondPlaces() { return secondPlaces; }
    public void addSecondPlace() { this.secondPlaces++; }

    public int getThirdPlaces() { return thirdPlaces; }
    public void addThirdPlace() { this.thirdPlaces++; }

    public int getGamesPlayed() { return gamesPlayed; }
    public void addGamePlayed() { this.gamesPlayed++; }

    public int getBestTimeSeconds() { return bestTimeSeconds; }
    public void recordTime(int seconds) {
        if (this.bestTimeSeconds == 0 || seconds < this.bestTimeSeconds) {
            this.bestTimeSeconds = seconds;
        }
    }

    public double getWinRate() {
        if (gamesPlayed == 0) return 0.0;
        return ((double) wins / gamesPlayed) * 100.0;
    }

    public String getFormattedBestTime() {
        if (bestTimeSeconds <= 0) return "N/A";
        int m = bestTimeSeconds / 60;
        int s = bestTimeSeconds % 60;
        return String.format("%02d:%02d", m, s);
    }
}
