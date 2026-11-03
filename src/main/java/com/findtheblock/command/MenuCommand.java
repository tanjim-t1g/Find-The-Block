package com.findtheblock.command;

import com.findtheblock.FindTheBlock;
import com.findtheblock.game.GameManager;
import com.findtheblock.party.PartyManager;
import com.findtheblock.ui.MatchSelectorMenu;
import com.findtheblock.util.Chat;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Handles standalone /menu (alias /selector) command to open Match Selector.
 */
public class MenuCommand implements CommandExecutor {

    private final FindTheBlock plugin;
    private final GameManager gameManager;
    private final PartyManager partyManager;

    public MenuCommand(FindTheBlock plugin, GameManager gameManager, PartyManager partyManager) {
        this.plugin = plugin;
        this.gameManager = gameManager;
        this.partyManager = partyManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            Chat.send(sender, "<red>Only players can open menus.</red>");
            return true;
        }

        new MatchSelectorMenu(plugin, gameManager, partyManager, player).open();
        return true;
    }
}
