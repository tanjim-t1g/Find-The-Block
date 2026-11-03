package com.findtheblock.game;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

/**
 * Custom inventory holder for the spectator racer teleportation menu.
 */
public class SpectatorMenuHolder implements InventoryHolder {

    private Inventory inventory;

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }
}
