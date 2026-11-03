package com.findtheblock.command;

import com.findtheblock.game.GameManager;
import com.findtheblock.util.Chat;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Handles standalone /join command to join an active match mid-game.
 */
public class JoinCommand implements CommandExecutor {

    private final GameManager gameManager;

    public JoinCommand(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            Chat.send(sender, "<red>Only players can join matches.</red>");
            return true;
        }

        gameManager.joinMidGame(player);
        return true;
    }
}
