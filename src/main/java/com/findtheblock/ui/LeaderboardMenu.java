package com.findtheblock.ui;

import com.findtheblock.stats.PlayerStats;
import com.findtheblock.stats.StatsManager;
import com.findtheblock.util.Chat;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.List;

/**
 * Interactive GUI showing top leaderboard rankings and personal statistics.
 */
public class LeaderboardMenu implements InventoryHolder {

    private final Inventory inventory;

    public LeaderboardMenu(StatsManager statsManager, Player viewer) {
        this.inventory = Bukkit.createInventory(this, 54, Chat.toComponent("<gold><bold>Top Racers Leaderboard</bold></gold>"));
        buildMenu(statsManager, viewer);
    }

    private void buildMenu(StatsManager statsManager, Player viewer) {
        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta fMeta = filler.getItemMeta();
        if (fMeta != null) {
            fMeta.displayName(Chat.toComponent(" "));
            filler.setItemMeta(fMeta);
        }
        for (int i = 0; i < 54; i++) inventory.setItem(i, filler);

        List<PlayerStats> top = statsManager.getTopPlayers(10);
        int[] slots = {10, 11, 12, 13, 14, 15, 16, 20, 21, 22};

        for (int i = 0; i < top.size() && i < slots.length; i++) {
            PlayerStats p = top.get(i);
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            if (meta != null) {
                meta.setOwningPlayer(Bukkit.getOfflinePlayer(p.getUuid()));
                meta.displayName(Chat.toComponent("<gold><bold>#" + (i + 1) + " </bold><yellow>" + p.getName() + "</yellow></gold>"));
                meta.lore(List.of(
                        Chat.toComponent("<green>Wins: <white>" + p.getWins() + "</white></green>"),
                        Chat.toComponent("<yellow>Podiums: <white>" + (p.getWins() + p.getSecondPlaces() + p.getThirdPlaces()) + "</white></yellow>"),
                        Chat.toComponent("<aqua>Played: <white>" + p.getGamesPlayed() + "</white></aqua>"),
                        Chat.toComponent("<light_purple>Win Rate: <white>" + String.format("%.1f%%", p.getWinRate()) + "</white></light_purple>"),
                        Chat.toComponent("<gold>Best Time: <white>" + p.getFormattedBestTime() + "</white></gold>")
                ));
                head.setItemMeta(meta);
            }
            inventory.setItem(slots[i], head);
        }

        // Viewer's own stats in slot 49
        PlayerStats own = statsManager.getStats(viewer.getUniqueId(), viewer.getName());
        ItemStack myHead = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta myMeta = (SkullMeta) myHead.getItemMeta();
        if (myMeta != null) {
            myMeta.setOwningPlayer(viewer);
            myMeta.displayName(Chat.toComponent("<green><bold>Your Statistics</bold></green>"));
            myMeta.lore(List.of(
                    Chat.toComponent("<green>Wins: <white>" + own.getWins() + "</white></green>"),
                    Chat.toComponent("<aqua>Played: <white>" + own.getGamesPlayed() + "</white></aqua>"),
                    Chat.toComponent("<light_purple>Win Rate: <white>" + String.format("%.1f%%", own.getWinRate()) + "</white></light_purple>"),
                    Chat.toComponent("<gold>Best Time: <white>" + own.getFormattedBestTime() + "</white></gold>")
            ));
            myHead.setItemMeta(myMeta);
        }
        inventory.setItem(49, myHead);
    }

    public void open(Player player) {
        if (player != null && player.isOnline()) {
            player.openInventory(inventory);
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
