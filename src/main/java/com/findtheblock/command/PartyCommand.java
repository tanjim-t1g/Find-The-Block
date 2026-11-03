package com.findtheblock.command;

import com.findtheblock.FindTheBlock;
import com.findtheblock.config.ConfigManager;
import com.findtheblock.config.DifficultyTier;
import com.findtheblock.game.GameManager;
import com.findtheblock.party.PartyManager;
import com.findtheblock.ui.PartyFinderMenu;
import com.findtheblock.ui.PartyMenu;
import com.findtheblock.util.Chat;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Handles the standalone /party (alias /p) command and tab completion.
 */
public class PartyCommand implements CommandExecutor, TabCompleter {

    private final FindTheBlock plugin;
    private final PartyManager partyManager;
    private final GameManager gameManager;
    private final ConfigManager config;

    public PartyCommand(FindTheBlock plugin, PartyManager partyManager, GameManager gameManager, ConfigManager config) {
        this.plugin = plugin;
        this.partyManager = partyManager;
        this.gameManager = gameManager;
        this.config = config;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            Chat.send(sender, "<red>Only players can use party commands.</red>");
            return true;
        }

        if (args.length == 0) {
            if (partyManager.getParty(player.getUniqueId()) != null) {
                new PartyMenu(plugin, partyManager, gameManager, player).open();
            } else {
                new PartyFinderMenu(plugin, partyManager, player).open();
            }
            return true;
        }

        String action = args[0].toLowerCase();
        switch (action) {
            case "finder", "browse" -> new PartyFinderMenu(plugin, partyManager, player).open();
            case "menu" -> new PartyMenu(plugin, partyManager, gameManager, player).open();
            case "create" -> {
                boolean isPublic = args.length <= 1 || !args[1].equalsIgnoreCase("private");
                partyManager.createParty(player, isPublic);
            }
            case "public" -> partyManager.setPublic(player, true);
            case "private" -> partyManager.setPublic(player, false);
            case "invite" -> {
                if (args.length < 2) {
                    Chat.send(player, "<red>Usage: /party invite <player></red>");
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    Chat.send(player, "<red>Player not found.</red>");
                    return true;
                }
                partyManager.invitePlayer(player, target);
            }
            case "accept" -> partyManager.acceptInvite(player);
            case "deny", "decline" -> partyManager.denyInvite(player);
            case "leave" -> partyManager.leaveParty(player);
            case "disband" -> partyManager.disbandParty(player.getUniqueId());
            case "start" -> {
                DifficultyTier tier = args.length > 1 ? DifficultyTier.fromString(args[1]) : config.getDefaultTier();
                gameManager.initiatePartyMatch(player, tier);
            }
            default -> Chat.send(player, "<yellow>Party Usage:</yellow> <gray>/party [finder|create|invite|accept|deny|leave|disband|public|private|start]</gray>");
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> options = Arrays.asList("finder", "create", "invite", "accept", "deny", "leave", "disband", "public", "private", "start");
            return options.stream().filter(s -> s.startsWith(args[0].toLowerCase())).toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("create")) {
            return Arrays.asList("public", "private");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("start")) {
            return Arrays.asList("easy", "normal", "hard");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("invite")) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase())).toList();
        }
        return Collections.emptyList();
    }
}
