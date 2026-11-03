package com.findtheblock.listener;

import com.findtheblock.game.GameManager;
import com.findtheblock.game.TargetEngine;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;

/**
 * Listens for crafting, smelting, looting, and pickup actions to verify objective acquisition.
 */
public class AcquisitionListener implements Listener {

    private final GameManager gameManager;
    private final TargetEngine targetEngine;

    public AcquisitionListener(GameManager gameManager, TargetEngine targetEngine) {
        this.gameManager = gameManager;
        this.targetEngine = targetEngine;
    }

    private void checkItem(Player player, ItemStack item) {
        if (player == null || item == null || !gameManager.isGameRunning()) return;
        if (!gameManager.getContestants().contains(player.getUniqueId())) return;

        Material target = gameManager.getCurrentTarget();
        if (target != null && targetEngine.isTarget(item, target)) {
            gameManager.onTargetAcquired(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player) {
            checkItem(player, event.getItem().getItemStack());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            ItemStack result = event.getRecipe().getResult();
            checkItem(player, result);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            // Check furnace result extract
            if (event.getInventory().getType() == InventoryType.FURNACE ||
                event.getInventory().getType() == InventoryType.BLAST_FURNACE ||
                event.getInventory().getType() == InventoryType.SMOKER) {
                if (event.getRawSlot() == 2 && event.getCurrentItem() != null) {
                    checkItem(player, event.getCurrentItem());
                }
            } else if (event.getCurrentItem() != null) {
                checkItem(player, event.getCurrentItem());
            }
        }
    }
}
