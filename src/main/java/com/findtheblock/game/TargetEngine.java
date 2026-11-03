package com.findtheblock.game;

import com.findtheblock.FindTheBlock;
import com.findtheblock.config.ConfigManager;
import com.findtheblock.config.DifficultyTier;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Manages target item selection and verification.
 */
public class TargetEngine {

    private final FindTheBlock plugin;
    private final ConfigManager config;

    public TargetEngine(FindTheBlock plugin, ConfigManager config) {
        this.plugin = plugin;
        this.config = config;
    }

    public Material pickRandomTarget(DifficultyTier tier) {
        List<Material> pool = config.getTargets(tier);
        if (pool.isEmpty()) return Material.CAMPFIRE;
        return pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
    }

    public boolean isTarget(ItemStack item, Material currentTarget) {
        if (item == null || currentTarget == null) return false;
        if (item.getType() == currentTarget) return true;
        // If target is SHULKER_BOX, accept any dyed or undyed shulker box
        if (currentTarget == Material.SHULKER_BOX && item.getType().name().endsWith("SHULKER_BOX")) {
            return true;
        }
        return false;
    }

    public String getFriendlyName(Material material) {
        if (material == null) return "Unknown";
        String[] parts = material.name().toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                sb.append(Character.toUpperCase(part.charAt(0)))
                  .append(part.substring(1))
                  .append(" ");
            }
        }
        return sb.toString().trim();
    }
}
