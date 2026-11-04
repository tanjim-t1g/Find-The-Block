package com.findtheblock.listener;

import com.findtheblock.FindTheBlock;
import com.findtheblock.config.ConfigManager;
import com.findtheblock.game.GameManager;
import com.findtheblock.game.SpectatorMenuHolder;
import com.findtheblock.lobby.LobbyManager;
import com.findtheblock.party.PartyManager;
import com.findtheblock.ui.MatchSelectorMenu;
import com.findtheblock.util.Chat;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

/**
 * Handles void lobby protections, match selector compass, and spectator interactions/protections.
 */
public class ProtectionListener implements Listener {

    private final FindTheBlock plugin;
    private final GameManager gameManager;
    private final LobbyManager lobbyManager;
    private final ConfigManager config;
    private final PartyManager partyManager;

    public ProtectionListener(FindTheBlock plugin, GameManager gameManager, LobbyManager lobbyManager,
                              ConfigManager config, PartyManager partyManager) {
        this.plugin = plugin;
        this.gameManager = gameManager;
        this.lobbyManager = lobbyManager;
        this.config = config;
        this.partyManager = partyManager;
    }

    private boolean isLobby(World world) {
        return world != null && world.getName().equalsIgnoreCase(config.getLobbyWorldName());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (isLobby(event.getBlock().getWorld()) || gameManager.getSpectatorManager().isSpectator(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (isLobby(event.getBlock().getWorld()) || gameManager.getSpectatorManager().isSpectator(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (isLobby(player.getWorld()) || gameManager.getSpectatorManager().isSpectator(player.getUniqueId())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player damager) {
            if (gameManager.getSpectatorManager().isSpectator(damager.getUniqueId())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHunger(FoodLevelChangeEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (isLobby(player.getWorld()) || gameManager.getSpectatorManager().isSpectator(player.getUniqueId())) {
                event.setCancelled(true);
                player.setFoodLevel(20);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onItemDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (isLobby(player.getWorld()) || gameManager.getSpectatorManager().isSpectator(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onItemPickup(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (gameManager.getSpectatorManager().isSpectator(player.getUniqueId())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMobTarget(EntityTargetLivingEntityEvent event) {
        if (event.getTarget() instanceof Player player) {
            if (gameManager.getSpectatorManager().isSpectator(player.getUniqueId())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onDeath(org.bukkit.event.entity.PlayerDeathEvent event) {
        if (isLobby(event.getEntity().getWorld())) {
            event.setKeepInventory(true);
            event.setKeepLevel(true);
            event.getDrops().clear();
            event.setDroppedExp(0);
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (isLobby(player.getWorld())) {
            double thresholdY = config.isVoidTeleportAuto()
                    ? (player.getWorld().getMinHeight() - 62.0)
                    : config.getVoidTeleportY();
            if (player.getLocation().getY() < thresholdY) {
                player.setFallDistance(0.0f);
                player.teleport(player.getWorld().getSpawnLocation());
            }
        } else if (gameManager.getSpectatorManager().isSpectator(player.getUniqueId())) {
            // Guard against spectator falling below void boundary in match worlds
            if (player.getLocation().getY() < -50 && gameManager.getActiveMatch() != null) {
                player.teleport(gameManager.getActiveMatch().getSpawnLocation());
            }
        }
    }

    @EventHandler
    public void onLobbyInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (isLobby(player.getWorld())) {
            ItemStack item = event.getItem();
            if (item != null && item.getType() == Material.COMPASS) {
                if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                    event.setCancelled(true);
                    new MatchSelectorMenu(plugin, gameManager, partyManager, player).open();
                }
            }
        }
    }

    @EventHandler
    public void onLobbyInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player && isLobby(player.getWorld())) {
            // Prevent moving or dropping compass while in lobby inventory
            if (event.getClickedInventory() != null && event.getClickedInventory().equals(player.getInventory())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onSpectatorInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (gameManager.getSpectatorManager().isSpectator(player.getUniqueId())) {
            ItemStack item = event.getItem();
            if (item != null && item.getType() == Material.COMPASS) {
                if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                    event.setCancelled(true);
                    gameManager.getSpectatorManager().openSpectatorMenu(player, gameManager.getContestants());
                }
            } else {
                // Disallow block interactions, chests, doors, etc. for spectators
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onSpectatorInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player spectator)) return;

        // 1. Handle Spectator Teleport Menu clicks
        if (event.getInventory().getHolder() instanceof SpectatorMenuHolder) {
            event.setCancelled(true);
            ItemStack clicked = event.getCurrentItem();
            if (clicked != null && clicked.getType() == Material.PLAYER_HEAD && clicked.getItemMeta() instanceof SkullMeta skull) {
                if (skull.getOwningPlayer() != null && skull.getOwningPlayer().getPlayer() != null) {
                    Player target = skull.getOwningPlayer().getPlayer();
                    if (target.isOnline()) {
                        spectator.teleport(target.getLocation());
                        spectator.closeInventory();
                        Chat.send(spectator, "<gray>Teleported to <yellow>" + target.getName() + "</yellow>.</gray>");
                    }
                }
            }
            return;
        }

        // 2. Prevent spectator from moving their hotbar teleporter compass
        if (gameManager.getSpectatorManager().isSpectator(spectator.getUniqueId())) {
            if (event.getClickedInventory() != null && event.getClickedInventory().equals(spectator.getInventory())) {
                event.setCancelled(true);
            }
        }
    }
}
