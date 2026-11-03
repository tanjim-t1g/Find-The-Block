package com.findtheblock.game;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles graceful reconnects for disconnected players during an active match.
 * Preserves their position, items, and status for up to 60 seconds.
 */
public class ReconnectTracker {

    public static class SavedSession {
        private final Location location;
        private final ItemStack[] inventory;
        private final ItemStack[] armor;
        private final double health;
        private final int foodLevel;
        private final float saturation;
        private final long disconnectTime;

        public SavedSession(Player player) {
            this.location = player.getLocation().clone();
            this.inventory = player.getInventory().getContents();
            this.armor = player.getInventory().getArmorContents();
            this.health = player.getHealth();
            this.foodLevel = player.getFoodLevel();
            this.saturation = player.getSaturation();
            this.disconnectTime = System.currentTimeMillis();
        }

        public void restore(Player player) {
            player.teleport(location);
            player.getInventory().setContents(inventory);
            player.getInventory().setArmorContents(armor);
            player.setHealth(Math.min(player.getMaxHealth(), health));
            player.setFoodLevel(foodLevel);
            player.setSaturation(saturation);
        }

        public boolean isExpired(long graceDurationMs) {
            return System.currentTimeMillis() - disconnectTime > graceDurationMs;
        }
    }

    private final Map<UUID, SavedSession> savedSessions = new ConcurrentHashMap<>();

    public void save(Player player) {
        if (player != null) {
            savedSessions.put(player.getUniqueId(), new SavedSession(player));
        }
    }

    public boolean hasSavedSession(UUID uuid, long graceDurationMs) {
        SavedSession session = savedSessions.get(uuid);
        if (session == null) return false;
        if (session.isExpired(graceDurationMs)) {
            savedSessions.remove(uuid);
            return false;
        }
        return true;
    }

    public boolean restore(Player player, long graceDurationMs) {
        SavedSession session = savedSessions.remove(player.getUniqueId());
        if (session == null || session.isExpired(graceDurationMs)) {
            return false;
        }
        session.restore(player);
        return true;
    }

    public void clear() {
        savedSessions.clear();
    }
}
