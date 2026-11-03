package com.findtheblock.command;

import com.findtheblock.stats.StatsManager;
import com.findtheblock.ui.LeaderboardMenu;
import com.findtheblock.util.Chat;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Handles standalone /top command to open the in-game GUI leaderboard.
 */
public class TopCommand implements CommandExecutor {

    private final StatsManager statsManager;

    public TopCommand(StatsManager statsManager) {
        this.statsManager = statsManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (sender instanceof Player player) {
            new LeaderboardMenu(statsManager, player).open(player);
        } else {
            Chat.send(sender, "<yellow>Top Racers:</yellow>");
            statsManager.getTopPlayers(5).forEach(s ->
                    Chat.send(sender, "<gray>" + s.getName() + ": <green>" + s.getWins() + " wins</green></gray>"));
        }
        return true;
    }
}
