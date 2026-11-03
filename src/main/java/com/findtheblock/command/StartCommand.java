package com.findtheblock.command;

import com.findtheblock.config.ConfigManager;
import com.findtheblock.config.DifficultyTier;
import com.findtheblock.game.GameManager;
import com.findtheblock.party.PartyManager;
import com.findtheblock.util.Chat;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Handles standalone /start command to start party ready check or admin lobby match.
 */
public class StartCommand implements CommandExecutor, TabCompleter {

    private final GameManager gameManager;
    private final PartyManager partyManager;
    private final ConfigManager config;

    public StartCommand(GameManager gameManager, PartyManager partyManager, ConfigManager config) {
        this.gameManager = gameManager;
        this.partyManager = partyManager;
        this.config = config;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        boolean wantsAll = args.length > 0 && args[0].equalsIgnoreCase("all");
        if (wantsAll) {
            if (!sender.hasPermission("findtheblock.admin")) {
                Chat.send(sender, "<red>Only administrators can start a match for all players.</red>");
                return true;
            }
            DifficultyTier tier = args.length > 1 ? DifficultyTier.fromString(args[1]) : config.getDefaultTier();
            gameManager.startAdminMatchForAll(sender, tier);
            return true;
        }

        if (sender.hasPermission("findtheblock.admin") && !(sender instanceof Player p && partyManager.isPartyLeader(p.getUniqueId()))) {
            DifficultyTier tier = args.length > 0 ? DifficultyTier.fromString(args[0]) : config.getDefaultTier();
            gameManager.startAdminMatchForAll(sender, tier);
            return true;
        }

        if (sender instanceof Player p) {
            DifficultyTier tier = args.length > 0 ? DifficultyTier.fromString(args[0]) : config.getDefaultTier();
            gameManager.initiatePartyMatch(p, tier);
        } else {
            DifficultyTier tier = args.length > 0 ? DifficultyTier.fromString(args[0]) : config.getDefaultTier();
            gameManager.startAdminMatchForAll(sender, tier);
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> list = new ArrayList<>(Arrays.asList("easy", "normal", "hard"));
            if (sender.hasPermission("findtheblock.admin")) {
                list.add("all");
            }
            return list.stream().filter(s -> s.startsWith(args[0].toLowerCase())).toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("all") && sender.hasPermission("findtheblock.admin")) {
            return Arrays.asList("easy", "normal", "hard");
        }
        return Collections.emptyList();
    }
}
