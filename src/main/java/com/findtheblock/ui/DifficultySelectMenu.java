package com.findtheblock.ui;

import com.findtheblock.FindTheBlock;
import com.findtheblock.config.DifficultyTier;
import com.findtheblock.game.GameManager;
import com.findtheblock.party.PartyManager;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

/**
 * Interactive GUI for choosing match difficulty (Easy, Normal/Medium, Hard) before starting.
 */
public class DifficultySelectMenu implements Listener {

    public enum StartTarget {
        PARTY,
        ALL_LOBBY
    }

    private final FindTheBlock plugin;
    private final GameManager gameManager;
    private final PartyManager partyManager;
    private final Player viewer;
    private final Inventory inventory;
    private final StartTarget startTarget;

    public DifficultySelectMenu(FindTheBlock plugin, GameManager gameManager, PartyManager partyManager, Player viewer) {
        this(plugin, gameManager, partyManager, viewer, StartTarget.PARTY);
    }

    public DifficultySelectMenu(FindTheBlock plugin, GameManager gameManager, PartyManager partyManager, Player viewer, StartTarget startTarget) {
        this.plugin = plugin;
        this.gameManager = gameManager;
        this.partyManager = partyManager;
        this.viewer = viewer;
        this.startTarget = startTarget;
        String title = startTarget == StartTarget.ALL_LOBBY
                ? "<gold><bold>Select Mode (All Lobby)</bold></gold>"
                : "<gold><bold>Select Game Mode</bold></gold>";
        this.inventory = Bukkit.createInventory(null, 27, MiniMessage.miniMessage().deserialize(title));
    }

    public void open() {
        populate();
        Bukkit.getPluginManager().registerEvents(this, plugin);
        viewer.openInventory(inventory);
    }

    private void populate() {
        inventory.clear();

        String actionHint = startTarget == StartTarget.ALL_LOBBY
                ? "<!italic><gold><bold>Click to Start for All Lobby!</bold></gold>"
                : "<!italic><green><bold>Click to Select & Start!</bold></green>";

        // Slot 11: Easy Mode
        ItemStack easy = new ItemStack(Material.LIME_CONCRETE);
        ItemMeta eMeta = easy.getItemMeta();
        if (eMeta != null) {
            eMeta.displayName(MiniMessage.miniMessage().deserialize("<!italic><green><bold>Easy Mode</bold></green>"));
            eMeta.lore(Arrays.asList(
                    MiniMessage.miniMessage().deserialize("<!italic><gray>Surface and basic farming/crafting items.</gray>"),
                    MiniMessage.miniMessage().deserialize("<!italic><gray>Ideal for fast, casual races.</gray>"),
                    MiniMessage.miniMessage().deserialize("<!italic><dark_gray>────────────────────</dark_gray>"),
                    MiniMessage.miniMessage().deserialize(actionHint)
            ));
            easy.setItemMeta(eMeta);
        }
        inventory.setItem(11, easy);

        // Slot 13: Normal / Medium Mode
        ItemStack normal = new ItemStack(Material.YELLOW_CONCRETE);
        ItemMeta nMeta = normal.getItemMeta();
        if (nMeta != null) {
            nMeta.displayName(MiniMessage.miniMessage().deserialize("<!italic><yellow><bold>Normal / Medium Mode</bold></yellow>"));
            nMeta.lore(Arrays.asList(
                    MiniMessage.miniMessage().deserialize("<!italic><gray>Cave mining and Nether exploration items.</gray>"),
                    MiniMessage.miniMessage().deserialize("<!italic><gray>The standard FindTheBlock experience.</gray>"),
                    MiniMessage.miniMessage().deserialize("<!italic><dark_gray>────────────────────</dark_gray>"),
                    MiniMessage.miniMessage().deserialize(actionHint)
            ));
            normal.setItemMeta(nMeta);
        }
        inventory.setItem(13, normal);

        // Slot 15: Hard Mode
        ItemStack hard = new ItemStack(Material.RED_CONCRETE);
        ItemMeta hMeta = hard.getItemMeta();
        if (hMeta != null) {
            hMeta.displayName(MiniMessage.miniMessage().deserialize("<!italic><red><bold>Hard Mode</bold></red>"));
            hMeta.lore(Arrays.asList(
                    MiniMessage.miniMessage().deserialize("<!italic><gray>Deep caves, fortresses, and challenge items.</gray>"),
                    MiniMessage.miniMessage().deserialize("<!italic><gray>For experienced speedrunners and survival pros.</gray>"),
                    MiniMessage.miniMessage().deserialize("<!italic><dark_gray>────────────────────</dark_gray>"),
                    MiniMessage.miniMessage().deserialize(actionHint)
            ));
            hard.setItemMeta(hMeta);
        }
        inventory.setItem(15, hard);

        // Slot 22: Back Button
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta bMeta = back.getItemMeta();
        if (bMeta != null) {
            bMeta.displayName(MiniMessage.miniMessage().deserialize("<!italic><gray>Back to Match Selector</gray>"));
            back.setItemMeta(bMeta);
        }
        inventory.setItem(22, back);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!event.getInventory().equals(inventory)) return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;
        int slot = event.getRawSlot();

        DifficultyTier selectedTier = null;
        if (slot == 11) selectedTier = DifficultyTier.EASY;
        else if (slot == 13) selectedTier = DifficultyTier.NORMAL;
        else if (slot == 15) selectedTier = DifficultyTier.HARD;
        else if (slot == 22) {
            player.closeInventory();
            new MatchSelectorMenu(plugin, gameManager, partyManager, player).open();
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.0f);
            return;
        }

        if (selectedTier != null) {
            player.closeInventory();
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.2f);

            if (startTarget == StartTarget.ALL_LOBBY) {
                gameManager.startAdminMatchForAll(player, selectedTier);
            } else {
                gameManager.initiatePartyMatch(player, selectedTier);
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getInventory().equals(inventory)) {
            HandlerList.unregisterAll(this);
        }
    }
}
