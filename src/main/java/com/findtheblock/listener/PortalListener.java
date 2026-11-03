package com.findtheblock.listener;

import com.findtheblock.factory.MatchWorldPair;
import com.findtheblock.game.GameManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

/**
 * Ensures Nether and End portals seamlessly link between the match's dimensions.
 * The End is loaded on-demand only when a player actually enters an End portal.
 */
public class PortalListener implements Listener {

    private final GameManager gameManager;

    public PortalListener(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPortal(PlayerPortalEvent event) {
        if (!gameManager.isGameRunning()) return;

        MatchWorldPair match = gameManager.getActiveMatch();
        if (match == null) return;

        World fromWorld = event.getFrom().getWorld();
        if (fromWorld == null) return;

        if (event.getCause() == PlayerTeleportEvent.TeleportCause.NETHER_PORTAL) {
            if (fromWorld.equals(match.getOverworld())) {
                // Routing from Match Overworld -> Match Nether (scale 1/8)
                Location from = event.getFrom();
                Location to = new Location(match.getNether(), from.getX() / 8.0, from.getY(), from.getZ() / 8.0);
                event.setTo(to);
            } else if (fromWorld.equals(match.getNether())) {
                // Routing from Match Nether -> Match Overworld (scale x8)
                Location from = event.getFrom();
                Location to = new Location(match.getOverworld(), from.getX() * 8.0, from.getY(), from.getZ() * 8.0);
                event.setTo(to);
            }
        } else if (event.getCause() == PlayerTeleportEvent.TeleportCause.END_PORTAL) {
            if (fromWorld.equals(match.getOverworld())) {
                // Routing from Match Overworld -> Match End (loaded on-demand)
                World endWorld = match.getOrCreateEnd();
                if (endWorld != null) {
                    Location endSpawn = new Location(endWorld, 100.5, 49.0, 0.5, 90.0f, 0.0f);
                    createEndPlatform(endWorld);
                    event.setTo(endSpawn);
                }
            } else if (match.getEnd() != null && fromWorld.equals(match.getEnd())) {
                // Routing from Match End -> Match Overworld Spawn
                event.setTo(match.getSpawnLocation());
            }
        }
    }

    private void createEndPlatform(World endWorld) {
        for (int x = 98; x <= 102; x++) {
            for (int z = -2; z <= 2; z++) {
                endWorld.getBlockAt(x, 48, z).setType(Material.OBSIDIAN);
                for (int y = 49; y <= 51; y++) {
                    endWorld.getBlockAt(x, y, z).setType(Material.AIR);
                }
            }
        }
    }
}
