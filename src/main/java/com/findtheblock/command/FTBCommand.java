package com.findtheblock.command;

import com.findtheblock.FindTheBlock;
import com.findtheblock.config.ConfigManager;
import com.findtheblock.config.DifficultyTier;
import com.findtheblock.game.GameManager;
import com.findtheblock.leaderboard.LeaderboardManager;
import com.findtheblock.party.PartyManager;
import com.findtheblock.stats.StatsManager;
import com.findtheblock.ui.LeaderboardMenu;
import com.findtheblock.ui.MatchSelectorMenu;
import com.findtheblock.ui.PartyFinderMenu;
import com.findtheblock.ui.PartyMenu;
import com.findtheblock.ui.RecipeMenu;
import com.findtheblock.util.Chat;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Main command and tab completer for FindTheBlock.
 */
public class FTBCommand implements CommandExecutor, TabCompleter {

    private final FindTheBlock plugin;
    private final GameManager gameManager;
    private final ConfigManager config;
    private final PartyManager partyManager;
    private final StatsManager statsManager;
    private final LeaderboardManager leaderboardManager;

    public FTBCommand(FindTheBlock plugin, GameManager gameManager, ConfigManager config,
                      PartyManager partyManager, StatsManager statsManager,
                      LeaderboardManager leaderboardManager) {
        this.plugin = plugin;
        this.gameManager = gameManager;
        this.config = config;
        this.partyManager = partyManager;
        this.statsManager = statsManager;
        this.leaderboardManager = leaderboardManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "start" -> {
                boolean wantsAll = args.length > 1 && args[1].equalsIgnoreCase("all");
                if (wantsAll) {
                    if (!sender.hasPermission("findtheblock.admin")) {
                        Chat.send(sender, "<red>Only administrators can start a match for all players.</red>");
                        return true;
                    }
                    DifficultyTier tier = args.length > 2 ? DifficultyTier.fromString(args[2]) : config.getDefaultTier();
                    gameManager.startAdminMatchForAll(sender, tier);
                } else if (sender.hasPermission("findtheblock.admin") && !(sender instanceof Player p && partyManager.isPartyLeader(p.getUniqueId()))) {
                    DifficultyTier tier = args.length > 1 ? DifficultyTier.fromString(args[1]) : config.getDefaultTier();
                    gameManager.startAdminMatchForAll(sender, tier);
                } else if (sender instanceof Player p) {
                    gameManager.initiatePartyMatch(p);
                } else {
                    DifficultyTier tier = args.length > 1 ? DifficultyTier.fromString(args[1]) : config.getDefaultTier();
                    gameManager.startAdminMatchForAll(sender, tier);
                }
            }
            case "startall" -> {
                if (!sender.hasPermission("findtheblock.admin")) {
                    Chat.send(sender, "<red>Only administrators can start a match for all players.</red>");
                    return true;
                }
                DifficultyTier tier = args.length > 1 ? DifficultyTier.fromString(args[1]) : config.getDefaultTier();
                gameManager.startAdminMatchForAll(sender, tier);
            }
            case "stop", "stopall" -> {
                if (!sender.hasPermission("findtheblock.admin")) {
                    Chat.send(sender, "<red>Only administrators can stop matches.</red>");
                    return true;
                }
                gameManager.forceStop(sender);
            }
            case "menu", "selector" -> {
                if (sender instanceof Player p) {
                    new MatchSelectorMenu(plugin, gameManager, partyManager, p).open();
                } else {
                    Chat.send(sender, "<red>Only players can open menus.</red>");
                }
            }
            case "join" -> {
                if (sender instanceof Player p) {
                    gameManager.joinMidGame(p);
                } else {
                    Chat.send(sender, "<red>Only players can join matches.</red>");
                }
            }
            case "spectate" -> {
                if (sender instanceof Player p) {
                    gameManager.spectateMidGame(p);
                } else {
                    Chat.send(sender, "<red>Only players can spectate.</red>");
                }
            }
            case "leave" -> {
                if (sender instanceof Player p) {
                    if (gameManager.isGameRunning() && gameManager.leaveMatch(p)) {
                        return true;
                    }
                    if (partyManager.getParty(p.getUniqueId()) != null) {
                        partyManager.leaveParty(p);
                        return true;
                    }
                    Chat.send(p, "<yellow>You are not currently in an active match or party.</yellow>");
                } else {
                    Chat.send(sender, "<red>Only players can leave matches.</red>");
                }
            }
            case "recipe" -> {
                if (sender instanceof Player p) {
                    if (gameManager.isGameRunning() && gameManager.getCurrentTarget() != null) {
                        new RecipeMenu(gameManager.getCurrentTarget()).open(p);
                    } else {
                        Chat.send(p, "<yellow>No active match or target to view.</yellow>");
                    }
                }
            }
            case "leaderboard" -> handleLeaderboardCommand(sender, args);
            case "top" -> {
                if (sender instanceof Player p) {
                    new LeaderboardMenu(statsManager, p).open(p);
                } else {
                    Chat.send(sender, "<yellow>Top Racers:</yellow>");
                    statsManager.getTopPlayers(5).forEach(s ->
                            Chat.send(sender, "<gray>" + s.getName() + ": <green>" + s.getWins() + " wins</green></gray>"));
                }
            }
            case "party" -> handlePartyCommand(sender, args);
            case "reload" -> {
                if (!sender.hasPermission("findtheblock.admin")) {
                    Chat.send(sender, "<red>No permission.</red>");
                    return true;
                }
                config.load();
                if (leaderboardManager != null) {
                    leaderboardManager.spawnOrUpdateAll();
                }
                Chat.send(sender, "<green>FindTheBlock configuration and leaderboards reloaded!</green>");
            }
            default -> sendHelp(sender);
        }

        return true;
    }

    private void handleLeaderboardCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            Chat.send(sender, "<red>Only in-game players can spawn or remove leaderboards.</red>");
            return;
        }

        if (args.length < 2) {
            new LeaderboardMenu(statsManager, player).open(player);
            return;
        }

        if (!player.hasPermission("findtheblock.admin")) {
            Chat.send(player, "<red>You don't have permission to manage in-world leaderboards.</red>");
            return;
        }

        String action = args[1].toLowerCase();
        if (action.equals("spawn") || action.equals("create")) {
            String type = args.length > 2 ? args[2].toLowerCase() : "wins";
            if (leaderboardManager.spawnLeaderboard(player.getLocation().clone().add(0, 1.2, 0), type)) {
                Chat.send(player, "<green>Floating leaderboard spawned at your position! (Type: " + type + ")</green>");
            } else {
                Chat.send(player, "<red>Could not spawn leaderboard here.</red>");
            }
        } else if (action.equals("remove") || action.equals("delete")) {
            if (leaderboardManager.removeNearestLeaderboard(player.getLocation())) {
                Chat.send(player, "<green>Nearest in-world leaderboard was removed.</green>");
            } else {
                Chat.send(player, "<yellow>No in-world leaderboard found within 10 blocks.</yellow>");
            }
        } else {
            Chat.send(player, "<yellow>Usage:</yellow> <gray>/ftb leaderboard [spawn|remove] [wins|time]</gray>");
        }
    }

    private void handlePartyCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            Chat.send(sender, "<red>Only players can use party commands.</red>");
            return;
        }

        if (args.length < 2) {
            if (partyManager.getParty(player.getUniqueId()) != null) {
                new PartyMenu(plugin, partyManager, gameManager, player).open();
            } else {
                new PartyFinderMenu(plugin, partyManager, player).open();
            }
            return;
        }

        String action = args[1].toLowerCase();
        switch (action) {
            case "finder", "browse" -> new PartyFinderMenu(plugin, partyManager, player).open();
            case "menu" -> new PartyMenu(plugin, partyManager, gameManager, player).open();
            case "create" -> {
                boolean isPublic = args.length <= 2 || !args[2].equalsIgnoreCase("private");
                partyManager.createParty(player, isPublic);
            }
            case "public" -> partyManager.setPublic(player, true);
            case "private" -> partyManager.setPublic(player, false);
            case "invite" -> {
                if (args.length < 3) {
                    Chat.send(player, "<red>Usage: /ftb party invite <player></red>");
                    return;
                }
                Player target = Bukkit.getPlayer(args[2]);
                if (target == null) {
                    Chat.send(player, "<red>Player not found.</red>");
                    return;
                }
                partyManager.invitePlayer(player, target);
            }
            case "accept" -> partyManager.acceptInvite(player);
            case "deny", "decline" -> partyManager.denyInvite(player);
            case "leave" -> partyManager.leaveParty(player);
            case "disband" -> partyManager.disbandParty(player.getUniqueId());
            case "start" -> {
                DifficultyTier tier = args.length > 2 ? DifficultyTier.fromString(args[2]) : config.getDefaultTier();
                gameManager.initiatePartyMatch(player, tier);
            }
            default -> Chat.send(player, "<yellow>Party Usage:</yellow> <gray>/party [finder|create|invite|accept|deny|leave|disband|public|private|start]</gray>");
        }
    }

    private void sendHelp(CommandSender sender) {
        Chat.send(sender, "<gold><bold>FindTheBlock Commands</bold></gold>");
        Chat.send(sender, "  <yellow>/ftb start</yellow> <gray>- Start match / ready check</gray>");
        Chat.send(sender, "  <yellow>/ftb menu</yellow> <gray>- Open match menu</gray>");
        Chat.send(sender, "  <yellow>/ftb party</yellow> <gray>- Party menu & finder</gray>");
        Chat.send(sender, "  <yellow>/ftb join</yellow> <gray>- Join active race</gray>");
        Chat.send(sender, "  <yellow>/ftb spectate</yellow> <gray>- Spectate race</gray>");
        Chat.send(sender, "  <yellow>/leave</yellow> <gray>- Leave race or party</gray>");
        Chat.send(sender, "  <yellow>/ftb recipe</yellow> <gray>- View target recipe</gray>");
        Chat.send(sender, "  <yellow>/ftb top</yellow> <gray>- View leaderboards</gray>");
        if (sender.hasPermission("findtheblock.admin")) {
            Chat.send(sender, "  <red>/ftb startall [tier]</red> <gray>- Start match for all</gray>");
            Chat.send(sender, "  <red>/ftb stop</red> <gray>- Stop all matches</gray>");
            Chat.send(sender, "  <red>/ftb reload</red> <gray>- Reload config</gray>");
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> sub = new ArrayList<>(Arrays.asList("start", "menu", "join", "spectate", "leave", "recipe", "top", "party", "leaderboard"));
            if (sender.hasPermission("findtheblock.admin")) {
                sub.add("startall");
                sub.add("stop");
                sub.add("reload");
            }
            return sub.stream().filter(s -> s.startsWith(args[0].toLowerCase())).toList();
        }

        if (args.length == 2 && (args[0].equalsIgnoreCase("start") || args[0].equalsIgnoreCase("startall"))) {
            List<String> options = new ArrayList<>(Arrays.asList("easy", "normal", "hard"));
            if (sender.hasPermission("findtheblock.admin") && args[0].equalsIgnoreCase("start")) {
                options.add("all");
            }
            return options;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("leaderboard") && sender.hasPermission("findtheblock.admin")) {
            return Arrays.asList("spawn", "remove");
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("leaderboard") && args[1].equalsIgnoreCase("spawn")) {
            return Arrays.asList("wins", "time");
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("party")) {
            return Arrays.asList("finder", "create", "invite", "accept", "deny", "leave", "disband", "public", "private", "start");
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("party") && args[1].equalsIgnoreCase("start")) {
            return Arrays.asList("easy", "normal", "hard");
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("party") && args[1].equalsIgnoreCase("create")) {
            return Arrays.asList("public", "private");
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("party") && args[1].equalsIgnoreCase("invite")) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).filter(n -> n.toLowerCase().startsWith(args[2].toLowerCase())).toList();
        }

        return Collections.emptyList();
    }
}
