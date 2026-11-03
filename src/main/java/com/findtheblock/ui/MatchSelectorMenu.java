package com.findtheblock.ui;

import com.findtheblock.FindTheBlock;
import com.findtheblock.game.GameManager;
import com.findtheblock.game.GameState;
import com.findtheblock.party.PartyManager;
import com.findtheblock.util.Chat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * Match Selector GUI opened by the lobby compass to view active matches, join/spectate, or open Party Finder.
 */
public class MatchSelectorMenu implements Listener {

    private final FindTheBlock plugin;
    private final GameManager gameManager;
    private final PartyManager partyManager;
    private final Player viewer;
    private final Inventory inventory;

    public MatchSelectorMenu(FindTheBlock plugin, GameManager gameManager, PartyManager partyManager, Player viewer) {
        this.plugin = plugin;
        this.gameManager = gameManager;
        this.partyManager = partyManager;
        this.viewer = viewer;
        this.inventory = Bukkit.createInventory(null, 36,
                MiniMessage.miniMessage().deserialize("<gold><bold>Match Selector</bold></gold>"));
    }

    public void open() {
        populate();
        Bukkit.getPluginManager().registerEvents(this, plugin);
        viewer.openInventory(inventory);
    }

    private void populate() {
        inventory.clear();

        // Slot 13: Active Match status
        if (gameManager.isGameRunning()) {
            Material target = gameManager.getCurrentTarget();
            ItemStack matchItem = new ItemStack(target != null && target.isItem() ? target : Material.BEACON);
            ItemMeta meta = matchItem.getItemMeta();
            if (meta != null) {
                meta.displayName(MiniMessage.miniMessage().deserialize("<!italic><yellow><bold>Active Race: " +
                        (target != null ? target.name() : "Target") + "</bold></yellow>"));

                List<Component> lore = new ArrayList<>();
                int elapsed = gameManager.getElapsedSeconds();
                lore.add(MiniMessage.miniMessage().deserialize(String.format("<!italic><gray>Time Elapsed: <aqua>%02d:%02d</aqua></gray>", elapsed / 60, elapsed % 60)));
                lore.add(MiniMessage.miniMessage().deserialize("<!italic><gray>Alive Racers: <green>" + gameManager.getContestants().size() + "</green></gray>"));
                lore.add(MiniMessage.miniMessage().deserialize("<!italic><dark_gray>────────────────────</dark_gray>"));
                lore.add(MiniMessage.miniMessage().deserialize("<!italic><green><bold>Left-Click:</bold> Join Race Mid-Game</green>"));
                lore.add(MiniMessage.miniMessage().deserialize("<!italic><aqua><bold>Right-Click:</bold> Spectate Match</aqua>"));
                meta.lore(lore);
                matchItem.setItemMeta(meta);
            }
            inventory.setItem(13, matchItem);
        } else {
            ItemStack idle = new ItemStack(Material.STRUCTURE_VOID);
            ItemMeta meta = idle.getItemMeta();
            if (meta != null) {
                meta.displayName(MiniMessage.miniMessage().deserialize("<!italic><gray>No active matches running.</gray>"));
                meta.lore(Collections.singletonList(
                        MiniMessage.miniMessage().deserialize("<!italic><dark_gray>Click below to start a match or join a party!</dark_gray>")
                ));
                idle.setItemMeta(meta);
            }
            inventory.setItem(13, idle);
        }

        boolean isAdmin = viewer.hasPermission("findtheblock.admin");
        boolean inParty = partyManager.getParty(viewer.getUniqueId()) != null;

        if (isAdmin) {
            // Slot 28: Party Hub
            ItemStack partyItem = new ItemStack(Material.PLAYER_HEAD);
            ItemMeta pMeta = partyItem.getItemMeta();
            if (pMeta != null) {
                pMeta.displayName(MiniMessage.miniMessage().deserialize("<!italic><gold><bold>" +
                        (inParty ? "Manage My Party" : "Party Finder") + "</bold></gold>"));
                pMeta.lore(Arrays.asList(
                        MiniMessage.miniMessage().deserialize("<!italic><gray>Discover public parties, invite</gray>"),
                        MiniMessage.miniMessage().deserialize("<!italic><gray>friends, and queue together.</gray>"),
                        MiniMessage.miniMessage().deserialize("<!italic><yellow><bold>Click to Open!</bold></yellow>")
                ));
                partyItem.setItemMeta(pMeta);
            }
            inventory.setItem(28, partyItem);

            // Slot 30: Start Party Match
            ItemStack partyStart = new ItemStack(Material.EMERALD);
            ItemMeta psMeta = partyStart.getItemMeta();
            if (psMeta != null) {
                psMeta.displayName(MiniMessage.miniMessage().deserialize("<!italic><green><bold>Start Party Match</bold></green>"));
                psMeta.lore(Arrays.asList(
                        MiniMessage.miniMessage().deserialize("<!italic><gray>Start match for your party.</gray>"),
                        MiniMessage.miniMessage().deserialize("<!italic><gray>Requires at least <gold>2 party players</gold>.</gray>"),
                        MiniMessage.miniMessage().deserialize("<!italic><green><bold>Click to Choose Mode!</bold></green>")
                ));
                partyStart.setItemMeta(psMeta);
            }
            inventory.setItem(30, partyStart);

            // Slot 32: Start Match for All (Lobby)
            ItemStack allStart = new ItemStack(Material.NETHER_STAR);
            ItemMeta asMeta = allStart.getItemMeta();
            if (asMeta != null) {
                asMeta.displayName(MiniMessage.miniMessage().deserialize("<!italic><gold><bold>Start Game for All (Lobby)</bold></gold>"));
                asMeta.lore(Arrays.asList(
                        MiniMessage.miniMessage().deserialize("<!italic><gray>Start match for all players</gray>"),
                        MiniMessage.miniMessage().deserialize("<!italic><gray>currently waiting in the lobby.</gray>"),
                        MiniMessage.miniMessage().deserialize("<!italic><gold><bold>Click to Choose Mode!</bold></gold>")
                ));
                allStart.setItemMeta(asMeta);
            }
            inventory.setItem(32, allStart);

            // Slot 34: Stop All Matches
            boolean running = gameManager.isGameRunning();
            ItemStack stopItem = new ItemStack(running ? Material.REDSTONE_BLOCK : Material.BARRIER);
            ItemMeta stMeta = stopItem.getItemMeta();
            if (stMeta != null) {
                stMeta.displayName(MiniMessage.miniMessage().deserialize("<!italic><red><bold>Stop All Matches</bold></red>"));
                if (running) {
                    stMeta.lore(Arrays.asList(
                            MiniMessage.miniMessage().deserialize("<!italic><gray>Force-stop all active matches</gray>"),
                            MiniMessage.miniMessage().deserialize("<!italic><gray>and return everyone to lobby.</gray>"),
                            MiniMessage.miniMessage().deserialize("<!italic><red><bold>Click to End Game!</bold></red>")
                    ));
                } else {
                    stMeta.lore(Collections.singletonList(
                            MiniMessage.miniMessage().deserialize("<!italic><gray>No match is currently running.</gray>")
                    ));
                }
                stopItem.setItemMeta(stMeta);
            }
            inventory.setItem(34, stopItem);
        } else {
            // Non-admin layout
            // Slot 29: Party Hub
            ItemStack partyItem = new ItemStack(Material.PLAYER_HEAD);
            ItemMeta pMeta = partyItem.getItemMeta();
            if (pMeta != null) {
                pMeta.displayName(MiniMessage.miniMessage().deserialize("<!italic><gold><bold>" +
                        (inParty ? "Manage My Party" : "Party Finder") + "</bold></gold>"));
                pMeta.lore(Arrays.asList(
                        MiniMessage.miniMessage().deserialize("<!italic><gray>Discover public parties, invite</gray>"),
                        MiniMessage.miniMessage().deserialize("<!italic><gray>friends, and queue together.</gray>"),
                        MiniMessage.miniMessage().deserialize("<!italic><yellow><bold>Click to Open!</bold></yellow>")
                ));
                partyItem.setItemMeta(pMeta);
            }
            inventory.setItem(29, partyItem);

            // Slot 33: Start Party Match
            ItemStack start = new ItemStack(Material.EMERALD);
            ItemMeta sMeta = start.getItemMeta();
            if (sMeta != null) {
                sMeta.displayName(MiniMessage.miniMessage().deserialize("<!italic><green><bold>▶ Start Match</bold></green>"));
                sMeta.lore(Arrays.asList(
                        MiniMessage.miniMessage().deserialize("<!italic><gray>Start match for your party.</gray>"),
                        MiniMessage.miniMessage().deserialize("<!italic><gray>Requires at least <gold>2 party players</gold>.</gray>"),
                        MiniMessage.miniMessage().deserialize("<!italic><green><bold>Click to Start!</bold></green>")
                ));
                start.setItemMeta(sMeta);
            }
            inventory.setItem(33, start);
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!event.getInventory().equals(inventory)) return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;
        int slot = event.getRawSlot();
        boolean isAdmin = player.hasPermission("findtheblock.admin");

        if (slot == 13) { // Active match action
            if (gameManager.isGameRunning()) {
                player.closeInventory();
                if (event.getClick() == ClickType.RIGHT) {
                    gameManager.spectateMidGame(player);
                } else {
                    gameManager.joinMidGame(player);
                }
            }
        } else if ((slot == 28 && isAdmin) || (slot == 29 && !isAdmin)) { // Party Hub
            player.closeInventory();
            if (partyManager.getParty(player.getUniqueId()) != null) {
                new PartyMenu(plugin, partyManager, gameManager, player).open();
            } else {
                new PartyFinderMenu(plugin, partyManager, player).open();
            }
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.0f);
        } else if ((slot == 30 && isAdmin) || (slot == 33 && !isAdmin)) { // Start Party Match
            player.closeInventory();
            new DifficultySelectMenu(plugin, gameManager, partyManager, player, DifficultySelectMenu.StartTarget.PARTY).open();
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.0f);
        } else if (slot == 32 && isAdmin) { // Admin: Start Game for All (Lobby)
            player.closeInventory();
            new DifficultySelectMenu(plugin, gameManager, partyManager, player, DifficultySelectMenu.StartTarget.ALL_LOBBY).open();
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.0f);
        } else if (slot == 34 && isAdmin) { // Admin: Stop All Matches
            player.closeInventory();
            if (gameManager.getState() == GameState.LOBBY) {
                Chat.send(player, "<yellow>No match running.</yellow>");
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.0f);
            } else {
                gameManager.forceStop(player);
                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.8f, 1.0f);
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
