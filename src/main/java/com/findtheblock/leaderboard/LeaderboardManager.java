package com.findtheblock.leaderboard;

import com.findtheblock.FindTheBlock;
import com.findtheblock.stats.PlayerStats;
import com.findtheblock.stats.StatsManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.TextDisplay;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Manages in-world holographic TextDisplay leaderboards in the lobby.
 */
public class LeaderboardManager {

    private final FindTheBlock plugin;
    private final StatsManager statsManager;
    private final File file;
    private FileConfiguration config;
    private final Map<String, TextDisplay> activeDisplays = new HashMap<>();

    public LeaderboardManager(FindTheBlock plugin, StatsManager statsManager) {
        this.plugin = plugin;
        this.statsManager = statsManager;
        this.file = new File(plugin.getDataFolder(), "leaderboards.yml");
        load();
    }

    public void load() {
        if (!file.exists()) {
            try {
                if (file.getParentFile() != null) file.getParentFile().mkdirs();
                file.createNewFile();
            } catch (IOException ignored) {}
        }
        this.config = YamlConfiguration.loadConfiguration(file);

        // Delay loading entities until worlds are fully active
        Bukkit.getScheduler().runTaskLater(plugin, this::spawnOrUpdateAll, 40L);
    }

    public void saveConfig() {
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save leaderboards.yml: " + e.getMessage());
        }
    }

    public boolean spawnLeaderboard(Location loc, String type) {
        if (loc.getWorld() == null) return false;

        String id = "board_" + UUID.randomUUID().toString().substring(0, 6);
        config.set("boards." + id + ".world", loc.getWorld().getName());
        config.set("boards." + id + ".x", loc.getX());
        config.set("boards." + id + ".y", loc.getY());
        config.set("boards." + id + ".z", loc.getZ());
        config.set("boards." + id + ".type", type != null ? type.toLowerCase() : "wins");
        saveConfig();

        createTextDisplay(id, loc, type != null ? type.toLowerCase() : "wins");
        return true;
    }

    public boolean removeNearestLeaderboard(Location loc) {
        if (loc.getWorld() == null) return false;

        String closestId = null;
        double closestDist = 10.0;

        if (config.isConfigurationSection("boards")) {
            for (String id : config.getConfigurationSection("boards").getKeys(false)) {
                String worldName = config.getString("boards." + id + ".world");
                if (worldName != null && worldName.equals(loc.getWorld().getName())) {
                    double x = config.getDouble("boards." + id + ".x");
                    double y = config.getDouble("boards." + id + ".y");
                    double z = config.getDouble("boards." + id + ".z");
                    Location boardLoc = new Location(loc.getWorld(), x, y, z);
                    double dist = boardLoc.distance(loc);
                    if (dist < closestDist) {
                        closestDist = dist;
                        closestId = id;
                    }
                }
            }
        }

        if (closestId != null) {
            TextDisplay display = activeDisplays.remove(closestId);
            if (display != null && display.isValid()) {
                display.remove();
            }
            config.set("boards." + closestId, null);
            saveConfig();
            return true;
        }

        return false;
    }

    public void spawnOrUpdateAll() {
        if (!config.isConfigurationSection("boards")) return;

        for (String id : config.getConfigurationSection("boards").getKeys(false)) {
            String worldName = config.getString("boards." + id + ".world");
            World world = Bukkit.getWorld(worldName != null ? worldName : "lobby");
            if (world == null) continue;

            double x = config.getDouble("boards." + id + ".x");
            double y = config.getDouble("boards." + id + ".y");
            double z = config.getDouble("boards." + id + ".z");
            String type = config.getString("boards." + id + ".type", "wins");
            Location loc = new Location(world, x, y, z);

            TextDisplay existing = activeDisplays.get(id);
            if (existing != null && existing.isValid()) {
                existing.text(formatLeaderboardText(type));
            } else {
                // Clear any orphaned text displays nearby
                for (Entity e : world.getNearbyEntities(loc, 1.5, 1.5, 1.5)) {
                    if (e instanceof TextDisplay td && td.getScoreboardTags().contains("ftb_leaderboard")) {
                        td.remove();
                    }
                }
                createTextDisplay(id, loc, type);
            }
        }
    }

    private void createTextDisplay(String id, Location loc, String type) {
        if (loc.getWorld() == null) return;

        TextDisplay display = loc.getWorld().spawn(loc, TextDisplay.class, td -> {
            td.setBillboard(Display.Billboard.CENTER);
            td.setDefaultBackground(true);
            td.setShadowed(true);
            td.addScoreboardTag("ftb_leaderboard");
            td.addScoreboardTag("ftb_board_" + id);
            td.text(formatLeaderboardText(type));
        });

        activeDisplays.put(id, display);
    }

    public Component formatLeaderboardText(String type) {
        List<PlayerStats> top = statsManager.getTopPlayers(10);
        StringBuilder sb = new StringBuilder();

        sb.append("<gold><bold>FINDTHEBLOCK TOP RACERS</bold></gold>\n");
        sb.append("<gray>─────────────────────────────</gray>\n");

        if (top.isEmpty()) {
            sb.append("<italic><gray>No race records yet. Start a match!</gray></italic>\n");
        } else {
            int rank = 1;
            for (PlayerStats s : top) {
                String rankColor = switch (rank) {
                    case 1 -> "<gold><bold>#1</bold></gold>";
                    case 2 -> "<yellow><bold>#2</bold></yellow>";
                    case 3 -> "<white><bold>#3</bold></white>";
                    default -> "<gray>#" + rank + "</gray>";
                };

                String score = type.equalsIgnoreCase("time")
                        ? "<aqua>" + s.getFormattedBestTime() + "</aqua>"
                        : "<green>" + s.getWins() + " wins</green>";

                sb.append(rankColor).append(" <white>").append(s.getName()).append("</white> ─ ").append(score).append("\n");
                rank++;
            }
        }

        sb.append("<gray>─────────────────────────────</gray>\n");
        sb.append("<dark_gray>Updates automatically after matches</dark_gray>");

        return MiniMessage.miniMessage().deserialize(sb.toString());
    }

    public void removeAll() {
        for (TextDisplay td : activeDisplays.values()) {
            if (td != null && td.isValid()) {
                td.remove();
            }
        }
        activeDisplays.clear();
    }
}
