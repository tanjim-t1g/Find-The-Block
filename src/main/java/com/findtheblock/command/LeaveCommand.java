package com.findtheblock.command;

import com.findtheblock.game.GameManager;
import com.findtheblock.party.PartyManager;
import com.findtheblock.util.Chat;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Handles the /leave command for players to leave an ongoing race or party.
 */
public class LeaveCommand implements CommandExecutor {

    private final GameManager gameManager;
    private final PartyManager partyManager;

    public LeaveCommand(GameManager gameManager, PartyManager partyManager) {
        this.gameManager = gameManager;
        this.partyManager = partyManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            Chat.send(sender, "<red>Only players can leave matches.</red>");
            return true;
        }

        // 1. Check if in an active match
        if (gameManager.isGameRunning()) {
            boolean left = gameManager.leaveMatch(player);
            if (left) return true;
        }

        // 2. If in lobby and in a party, leave party
        if (partyManager.getParty(player.getUniqueId()) != null) {
            partyManager.leaveParty(player);
            return true;
        }

        Chat.send(player, "<yellow>You are not currently in an active match or party.</yellow>");
        return true;
    }
}
