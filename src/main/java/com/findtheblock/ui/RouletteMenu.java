package com.findtheblock.ui;

import com.findtheblock.FindTheBlock;
import com.findtheblock.util.Chat;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Visual GUI Roulette that spins through items before selecting the final target block.
 */
public class RouletteMenu implements InventoryHolder {

    private final FindTheBlock plugin;
    private final List<Material> pool;
    private final Material finalTarget;
    private final Runnable onComplete;
    private final Inventory inventory;

    public RouletteMenu(FindTheBlock plugin, List<Material> pool, Material finalTarget, Runnable onComplete) {
        this.plugin = plugin;
        this.pool = pool;
        this.finalTarget = finalTarget;
        this.onComplete = onComplete;
        this.inventory = Bukkit.createInventory(this, 27, Chat.toComponent("<gold><bold>Selecting Target Objective...</bold></gold>"));
        setupBorders();
    }

    private void setupBorders() {
        ItemStack glass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = glass.getItemMeta();
        if (meta != null) {
            meta.displayName(Chat.toComponent(" "));
            glass.setItemMeta(meta);
        }

        ItemStack pointer = new ItemStack(Material.REDSTONE_TORCH);
        ItemMeta pMeta = pointer.getItemMeta();
        if (pMeta != null) {
            pMeta.displayName(Chat.toComponent("<red><bold>▼ TARGET ▼</bold></red>"));
            pointer.setItemMeta(pMeta);
        }

        // Fill top and bottom rows
        for (int i = 0; i < 9; i++) inventory.setItem(i, glass);
        for (int i = 18; i < 27; i++) inventory.setItem(i, glass);

        inventory.setItem(4, pointer);
    }

    public void start(List<Player> players) {
        for (Player p : players) {
            p.openInventory(inventory);
        }

        new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = 35;

            @Override
            public void run() {
                ticks++;

                if (ticks < maxTicks) {
                    // Roll random item into slot 13
                    Material roll = pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
                    inventory.setItem(13, new ItemStack(roll));

                    for (Player p : players) {
                        if (p.isOnline()) {
                            Chat.playSound(p, Sound.BLOCK_NOTE_BLOCK_HAT, 0.8f, 1.5f);
                        }
                    }
                } else {
                    // Final stop on target
                    inventory.setItem(13, new ItemStack(finalTarget));
                    for (Player p : players) {
                        if (p.isOnline()) {
                            Chat.playSound(p, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
                        }
                    }

                    // Wait 1.5s preview, then finish
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        for (Player p : players) {
                            if (p.isOnline() && p.getOpenInventory().getTopInventory().getHolder() instanceof RouletteMenu) {
                                p.closeInventory();
                            }
                        }
                        if (onComplete != null) onComplete.run();
                    }, 30L);

                    cancel();
                }
            }
        }.runTaskTimer(plugin, 2L, 2L);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
