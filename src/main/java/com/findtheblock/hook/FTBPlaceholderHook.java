package com.findtheblock.hook;

import com.findtheblock.FindTheBlock;
import com.findtheblock.game.GameManager;
import com.findtheblock.stats.PlayerStats;
import com.findtheblock.stats.StatsManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Native PlaceholderAPI expansion for FindTheBlock.
 * Exposes placeholders for tablists, scoreboards, and holograms.
 */
public class FTBPlaceholderHook extends PlaceholderExpansion {

    private final FindTheBlock plugin;
    private final GameManager gameManager;
    private final StatsManager statsManager;

    public FTBPlaceholderHook(FindTheBlock plugin, GameManager gameManager, StatsManager statsManager) {
        this.plugin = plugin;
        this.gameManager = gameManager;
        this.statsManager = statsManager;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "findtheblock";
    }

    @Override
    public @NotNull String getAuthor() {
        return "tanjim-t1g";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        String lower = params.toLowerCase();

        // Global Match Placeholders
        if (lower.equals("state")) {
            return gameManager.getState().name();
        }
        if (lower.equals("state_display")) {
            return gameManager.getState().getDisplay();
        }
        if (lower.equals("target")) {
            return gameManager.getCurrentTarget() != null ? gameManager.getCurrentTarget().name() : "None";
        }
        if (lower.equals("time")) {
            int elapsed = gameManager.getElapsedSeconds();
            return String.format("%02d:%02d", elapsed / 60, elapsed % 60);
        }
        if (lower.equals("contestants")) {
            return String.valueOf(gameManager.getContestants().size());
        }

        // Leaderboard Top Placeholders: top_1_name, top_1_wins
        if (lower.startsWith("top_")) {
            String[] parts = lower.split("_");
            if (parts.length >= 3) {
                try {
                    int rank = Integer.parseInt(parts[1]);
                    String type = parts[2];
                    List<PlayerStats> top = statsManager.getTopPlayers(rank);
                    if (top.size() >= rank) {
                        PlayerStats entry = top.get(rank - 1);
                        if (type.equals("name")) return entry.getName();
                        if (type.equals("wins")) return String.valueOf(entry.getWins());
                        if (type.equals("time")) return entry.getFormattedBestTime();
                    }
                } catch (NumberFormatException ignored) {}
            }
            return "-";
        }

        // Personal Player Placeholders
        if (player != null) {
            PlayerStats stats = statsManager.getStats(player.getUniqueId());
            if (stats == null) return "0";

            switch (lower) {
                case "wins":
                    return String.valueOf(stats.getWins());
                case "played":
                    return String.valueOf(stats.getGamesPlayed());
                case "win_rate":
                    return String.format("%.1f%%", stats.getWinRate());
                case "best_time":
                    return stats.getFormattedBestTime();
                case "podiums":
                    return String.valueOf(stats.getWins() + stats.getSecondPlaces() + stats.getThirdPlaces());
                default:
                    break;
            }
        }

        return null;
    }
}
