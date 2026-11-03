package com.findtheblock.command;

import com.findtheblock.game.GameManager;
import com.findtheblock.ui.RecipeMenu;
import com.findtheblock.util.Chat;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Handles standalone /recipe command to open target block recipe / guide.
 */
public class RecipeCommand implements CommandExecutor {

    private final GameManager gameManager;

    public RecipeCommand(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            Chat.send(sender, "<red>Only players can view recipes.</red>");
            return true;
        }

        if (gameManager.isGameRunning() && gameManager.getCurrentTarget() != null) {
            new RecipeMenu(gameManager.getCurrentTarget()).open(player);
        } else {
            Chat.send(player, "<yellow>No active match or target to view.</yellow>");
        }
        return true;
    }
}
