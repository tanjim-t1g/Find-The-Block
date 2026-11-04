package com.findtheblock.config;

import com.findtheblock.FindTheBlock;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;

/**
 * Type-safe configuration manager for FindTheBlock.
 */
public class ConfigManager {

    private final FindTheBlock plugin;
    private final Map<DifficultyTier, List<Material>> targetPools = new EnumMap<>(DifficultyTier.class);

    private String lobbyWorldName;
    private boolean autoGeneratePlatform;
    private int platformSize;
    private int platformY;
    private Material platformMaterial;
    private double voidTeleportY;
    private boolean voidTeleportAuto = true;

    private int bufferSize;
    private int chunkRadius;
    private int chunksPerSecond;
    private double matchBorderSize;

    private int maxWinners;
    private int celebrationSeconds;
    private boolean twentyMinBuffs;
    private int timeoutHours;
    private boolean giveIronTools;
    private boolean giveSilkPickaxe;
    private int maxConcurrentMatches;

    private boolean allowLateEntry;
    private boolean allowSpectate;
    private int reconnectGraceSeconds;

    private int rollSeconds;
    private int previewSeconds;
    private int leaderInactivityMinutes;
    private DifficultyTier defaultTier;
    private boolean updateCheckerEnabled;
    private boolean updateNotifyOps;

    public ConfigManager(FindTheBlock plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        // Lobby settings
        this.lobbyWorldName = config.getString("lobby.world-name", "lobby");
        this.autoGeneratePlatform = config.getBoolean("lobby.platform.auto-generate", true);
        this.platformSize = config.getInt("lobby.platform.size", 100);
        this.platformY = config.getInt("lobby.platform.y-level", 100);
        String matStr = config.getString("lobby.platform.material", "BEDROCK");
        Material pMat = Material.matchMaterial(matStr);
        this.platformMaterial = (pMat != null && pMat.isBlock()) ? pMat : Material.BEDROCK;
        String voidVal = config.getString("lobby.void-teleport-y", "auto");
        if ("auto".equalsIgnoreCase(voidVal)) {
            this.voidTeleportAuto = true;
            this.voidTeleportY = -126.0;
        } else {
            this.voidTeleportAuto = false;
            try {
                this.voidTeleportY = Double.parseDouble(voidVal);
            } catch (NumberFormatException e) {
                this.voidTeleportY = -126.0;
            }
        }

        // World Factory settings
        this.bufferSize = Math.max(1, config.getInt("world-factory.buffer-size", 1));
        this.chunkRadius = Math.max(2, config.getInt("world-factory.chunk-radius", 6));
        this.chunksPerSecond = Math.max(5, config.getInt("world-factory.chunks-per-second", 30));
        this.matchBorderSize = config.getDouble("world-factory.match-border-size", 59999968.0);

        // Match settings
        this.maxWinners = Math.max(1, config.getInt("match.max-winners", 3));
        this.celebrationSeconds = Math.max(3, config.getInt("match.celebration-seconds", 10));
        this.twentyMinBuffs = config.getBoolean("match.twenty-minute-buffs", true);
        this.timeoutHours = config.getInt("match.timeout-hours", 3);
        this.maxConcurrentMatches = Math.max(1, config.getInt("match.max-concurrent-matches", 1));
        this.giveIronTools = config.getBoolean("match.starter-kit.iron-tools", true);
        this.giveSilkPickaxe = config.getBoolean("match.starter-kit.silk-touch-pickaxe", true);

        // Mid-game connection settings
        this.allowLateEntry = config.getBoolean("match.mid-game.allow-late-entry", true);
        this.allowSpectate = config.getBoolean("match.mid-game.allow-spectate", true);
        this.reconnectGraceSeconds = config.getInt("match.mid-game.reconnect-grace-seconds", 60);

        // Roulette & Party
        this.rollSeconds = config.getInt("match.roulette.roll-seconds", 3);
        this.previewSeconds = config.getInt("match.roulette.preview-seconds", 4);
        this.leaderInactivityMinutes = config.getInt("match.party.leader-inactivity-minutes", 10);
        this.defaultTier = DifficultyTier.fromString(config.getString("targets.default-tier", "NORMAL"));
        this.updateCheckerEnabled = config.getBoolean("update-checker.enabled", true);
        this.updateNotifyOps = config.getBoolean("update-checker.notify-ops", true);

        // Load item pools
        targetPools.clear();
        for (DifficultyTier tier : DifficultyTier.values()) {
            List<String> rawList = config.getStringList("targets." + tier.name());
            List<Material> materials = new ArrayList<>();
            for (String itemStr : rawList) {
                Material mat = Material.matchMaterial(itemStr.trim());
                if (mat != null && mat.isItem()) {
                    materials.add(mat);
                } else {
                    plugin.getLogger().warning("Invalid target material in " + tier.name() + ": " + itemStr);
                }
            }
            if (materials.isEmpty()) {
                // Fallbacks if empty
                materials.add(Material.CAMPFIRE);
                materials.add(Material.BOOKSHELF);
            }
            targetPools.put(tier, materials);
        }
    }

    public List<Material> getTargets(DifficultyTier tier) {
        return targetPools.getOrDefault(tier, targetPools.get(DifficultyTier.NORMAL));
    }

    public String getLobbyWorldName() { return lobbyWorldName; }
    public boolean isAutoGeneratePlatform() { return autoGeneratePlatform; }
    public int getPlatformSize() { return platformSize; }
    public int getPlatformY() { return platformY; }
    public Material getPlatformMaterial() { return platformMaterial; }
    public double getVoidTeleportY() { return voidTeleportY; }
    public boolean isVoidTeleportAuto() { return voidTeleportAuto; }

    public int getBufferSize() { return bufferSize; }
    public int getChunkRadius() { return chunkRadius; }
    public int getChunksPerSecond() { return chunksPerSecond; }
    public double getMatchBorderSize() { return matchBorderSize; }

    public int getMaxWinners() { return maxWinners; }
    public int getCelebrationSeconds() { return celebrationSeconds; }
    public boolean isTwentyMinBuffs() { return twentyMinBuffs; }
    public int getTimeoutHours() { return timeoutHours; }
    public int getMaxConcurrentMatches() { return maxConcurrentMatches; }
    public boolean isGiveIronTools() { return giveIronTools; }
    public boolean isGiveSilkPickaxe() { return giveSilkPickaxe; }

    public boolean isAllowLateEntry() { return allowLateEntry; }
    public boolean isAllowSpectate() { return allowSpectate; }
    public int getReconnectGraceSeconds() { return reconnectGraceSeconds; }

    public int getRollSeconds() { return rollSeconds; }
    public int getPreviewSeconds() { return previewSeconds; }
    public int getLeaderInactivityMinutes() { return leaderInactivityMinutes; }
    public DifficultyTier getDefaultTier() { return defaultTier; }
    public boolean isUpdateCheckerEnabled() { return updateCheckerEnabled; }
    public boolean isUpdateNotifyOps() { return updateNotifyOps; }
}
