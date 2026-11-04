package com.findtheblock.lobby;

import com.findtheblock.FindTheBlock;
import com.findtheblock.config.ConfigManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages the dedicated void lobby world and its 100x100 bedrock platform.
 */
public class LobbyManager {

    private final FindTheBlock plugin;
    private final ConfigManager config;
    private World lobbyWorld;
    private Location spawnLocation;

    public LobbyManager(FindTheBlock plugin, ConfigManager config) {
        this.plugin = plugin;
        this.config = config;
        initializeLobbyWorld();
    }

    public void initializeLobbyWorld() {
        String worldName = config.getLobbyWorldName();
        this.lobbyWorld = Bukkit.getWorld(worldName);

        if (this.lobbyWorld == null) {
            plugin.getLogger().info("Generating dedicated void lobby world '" + worldName + "'...");
            WorldCreator creator = new WorldCreator(worldName);
            creator.environment(World.Environment.NORMAL);
            creator.generator(new VoidChunkGenerator());
            this.lobbyWorld = creator.createWorld();
        }

        if (this.lobbyWorld == null) {
            plugin.getLogger().severe("Failed to initialize lobby world! Falling back to default world.");
            this.lobbyWorld = Bukkit.getWorlds().get(0);
        }

        // Configure game rules for peaceful lobby
        lobbyWorld.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        lobbyWorld.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
        lobbyWorld.setGameRule(GameRule.DO_MOB_SPAWNING, false);
        lobbyWorld.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
        lobbyWorld.setGameRule(GameRule.KEEP_INVENTORY, true);
        lobbyWorld.setTime(6000L); // Fixed noon
        lobbyWorld.setStorm(false);
        lobbyWorld.setThundering(false);

        // Build 100x100 Bedrock Platform at Y = 100 if requested
        if (config.isAutoGeneratePlatform()) {
            buildBedrockPlatform();
            this.spawnLocation = new Location(lobbyWorld, 0.5, config.getPlatformY() + 1.0, 0.5, 0.0f, 0.0f);
            lobbyWorld.setSpawnLocation(spawnLocation);
        } else {
            this.spawnLocation = lobbyWorld.getSpawnLocation();
        }
    }

    /**
     * Constructs the 100x100 Bedrock platform at Y=100.
     * Centered around (0, 0) from X: -50 to +49, Z: -50 to +49.
     */
    public void buildBedrockPlatform() {
        int halfSize = config.getPlatformSize() / 2;
        int y = config.getPlatformY();
        Material mat = config.getPlatformMaterial();

        // Check if platform is already constructed
        Block center = lobbyWorld.getBlockAt(0, y, 0);
        if (center.getType() == mat) {
            return;
        }

        plugin.getLogger().info("Building 100x100 Bedrock platform at Y=" + y + " in lobby world...");
        for (int x = -halfSize; x < halfSize; x++) {
            for (int z = -halfSize; z < halfSize; z++) {
                Block block = lobbyWorld.getBlockAt(x, y, z);
                if (block.getType() != mat) {
                    block.setType(mat, false);
                }
            }
        }
        plugin.getLogger().info("Lobby bedrock platform complete!");
    }

    public void teleportToLobby(Player player) {
        if (player == null || !player.isOnline()) return;

        player.setGameMode(GameMode.ADVENTURE);
        player.setAllowFlight(false);
        player.setFlying(false);
        player.setFlySpeed(0.1f);
        player.setInvisible(false);
        player.setInvulnerable(false);
        player.setCollidable(true);
        player.setSilent(false);
        player.setHealth(20.0);
        player.setFoodLevel(20);
        player.setSaturation(20.0f);
        player.setFireTicks(0);
        player.getActivePotionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));
        player.getInventory().clear();
        giveLobbyItems(player);

        for (Player online : Bukkit.getOnlinePlayers()) {
            online.showPlayer(plugin, player);
        }

        player.teleport(spawnLocation);
    }

    public void giveLobbyItems(Player player) {
        ItemStack compass = new ItemStack(Material.COMPASS);
        ItemMeta meta = compass.getItemMeta();
        if (meta != null) {
            meta.displayName(MiniMessage.miniMessage()
                    .deserialize("<!italic><gold><bold>Match Selector</bold></gold> <gray>(Right-Click)</gray>"));
            List<Component> lore = new ArrayList<>();
            lore.add(MiniMessage.miniMessage()
                    .deserialize("<!italic><gray>Right-click to browse matches or parties.</gray>"));
            meta.lore(lore);
            compass.setItemMeta(meta);
        }
        player.getInventory().setItem(4, compass);
    }

    public World getLobbyWorld() {
        return lobbyWorld;
    }

    public Location getSpawnLocation() {
        return spawnLocation != null ? spawnLocation : lobbyWorld.getSpawnLocation();
    }
}
