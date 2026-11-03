package com.findtheblock.stats;

import com.findtheblock.FindTheBlock;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Manages player stats persistence and leaderboard sorting.
 */
public class StatsManager {

    private final FindTheBlock plugin;
    private final File file;
    private FileConfiguration data;
    private final Map<UUID, PlayerStats> cache = new ConcurrentHashMap<>();

    public StatsManager(FindTheBlock plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "stats.yml");
        load();
    }

    public void load() {
        if (!file.exists()) {
            try {
                if (file.getParentFile() != null) file.getParentFile().mkdirs();
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().warning("Could not create stats.yml: " + e.getMessage());
            }
        }
        this.data = YamlConfiguration.loadConfiguration(file);

        cache.clear();
        if (data.isConfigurationSection("players")) {
            for (String key : data.getConfigurationSection("players").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    String name = data.getString("players." + key + ".name", "Unknown");
                    int wins = data.getInt("players." + key + ".wins", 0);
                    int second = data.getInt("players." + key + ".second", 0);
                    int third = data.getInt("players." + key + ".third", 0);
                    int played = data.getInt("players." + key + ".played", 0);
                    int bestTime = data.getInt("players." + key + ".best_time", 0);

                    cache.put(uuid, new PlayerStats(uuid, name, wins, second, third, played, bestTime));
                } catch (IllegalArgumentException ignored) {}
            }
        }
    }

    public synchronized void save() {
        for (PlayerStats stats : cache.values()) {
            String path = "players." + stats.getUuid().toString();
            data.set(path + ".name", stats.getName());
            data.set(path + ".wins", stats.getWins());
            data.set(path + ".second", stats.getSecondPlaces());
            data.set(path + ".third", stats.getThirdPlaces());
            data.set(path + ".played", stats.getGamesPlayed());
            data.set(path + ".best_time", stats.getBestTimeSeconds());
        }

        try {
            data.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save stats.yml: " + e.getMessage());
        }
    }

    public PlayerStats getStats(UUID uuid, String name) {
        return cache.computeIfAbsent(uuid, id -> new PlayerStats(id, name));
    }

    public PlayerStats getStats(UUID uuid) {
        return cache.get(uuid);
    }

    public void recordPodiumFinish(UUID uuid, String name, int place, int timeSeconds) {
        PlayerStats stats = getStats(uuid, name);
        stats.setName(name);
        if (place == 1) {
            stats.addWin();
            stats.recordTime(timeSeconds);
        } else if (place == 2) {
            stats.addSecondPlace();
        } else if (place == 3) {
            stats.addThirdPlace();
        }
        save();
    }

    public void recordParticipation(UUID uuid, String name) {
        PlayerStats stats = getStats(uuid, name);
        stats.setName(name);
        stats.addGamePlayed();
        save();
    }

    public List<PlayerStats> getTopPlayers(int limit) {
        return cache.values().stream()
                .sorted(Comparator.comparingInt(PlayerStats::getWins).reversed()
                        .thenComparingInt(p -> p.getBestTimeSeconds() == 0 ? Integer.MAX_VALUE : p.getBestTimeSeconds()))
                .limit(limit)
                .collect(Collectors.toList());
    }
}
