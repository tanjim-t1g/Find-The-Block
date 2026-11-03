package com.findtheblock.ui;

import com.findtheblock.FindTheBlock;
import com.findtheblock.game.GameManager;
import com.findtheblock.game.GameState;
import com.findtheblock.party.Party;
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
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;

/**
 * Party Management GUI for viewing members, toggling privacy, and initiating matches.
 */
public class PartyMenu implements Listener {

    private final FindTheBlock plugin;
    private final PartyManager partyManager;
    private final GameManager gameManager;
    private final Player viewer;
    private final Inventory inventory;

    public PartyMenu(FindTheBlock plugin, PartyManager partyManager, GameManager gameManager, Player viewer) {
        this.plugin = plugin;
        this.partyManager = partyManager;
        this.gameManager = gameManager;
        this.viewer = viewer;
        this.inventory = Bukkit.createInventory(null, 36,
                MiniMessage.miniMessage().deserialize("<gold><bold>My Party</bold></gold>"));
    }

    public void open() {
        populate();
        Bukkit.getPluginManager().registerEvents(this, plugin);
        viewer.openInventory(inventory);
    }

    private void populate() {
        inventory.clear();
        Party party = partyManager.getParty(viewer.getUniqueId());

        if (party == null) {
            Chat.send(viewer, "<yellow>You are not in a party. Opening Party Finder...</yellow>");
            new PartyFinderMenu(plugin, partyManager, viewer).open();
            return;
        }

        boolean isLeader = party.getLeaderUuid().equals(viewer.getUniqueId());

        // Display party members across middle row (slots 10-16)
        int slot = 10;
        for (UUID memberUuid : party.getMembers()) {
            if (slot > 16) break;

            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            if (meta != null) {
                Player m = Bukkit.getPlayer(memberUuid);
                if (m != null) meta.setOwningPlayer(m);

                String name = m != null ? m.getName() : "Offline";
                boolean isThisLeader = memberUuid.equals(party.getLeaderUuid());

                meta.displayName(MiniMessage.miniMessage().deserialize("<!italic><yellow>" + name + (isThisLeader ? " <gold>(Leader)</gold>" : "") + "</yellow>"));
                head.setItemMeta(meta);
            }
            inventory.setItem(slot++, head);
        }

        boolean isAdmin = viewer.hasPermission("findtheblock.admin");

        // Leader controls
        if (isLeader) {
            // Slot 28: Start Party Match
            ItemStack start = new ItemStack(Material.EMERALD_BLOCK);
            ItemMeta startMeta = start.getItemMeta();
            if (startMeta != null) {
                startMeta.displayName(MiniMessage.miniMessage().deserialize("<!italic><green><bold>▶ Start Match Ready-Check</bold></green>"));
                startMeta.lore(Arrays.asList(
                        MiniMessage.miniMessage().deserialize("<!italic><gray>Requires at least <gold>2 party members</gold>.</gray>"),
                        MiniMessage.miniMessage().deserialize("<!italic><gray>Opens 20s confirmation for all members.</gray>")
                ));
                start.setItemMeta(startMeta);
            }
            inventory.setItem(28, start);

            // Slot 31: Toggle Privacy
            ItemStack toggle = new ItemStack(party.isPublic() ? Material.OAK_DOOR : Material.IRON_DOOR);
            ItemMeta toggleMeta = toggle.getItemMeta();
            if (toggleMeta != null) {
                String status = party.isPublic() ? "<green>PUBLIC</green>" : "<yellow>PRIVATE</yellow>";
                toggleMeta.displayName(MiniMessage.miniMessage().deserialize("<!italic><gold>Visibility: " + status + "</gold>"));
                toggleMeta.lore(Collections.singletonList(
                        MiniMessage.miniMessage().deserialize("<!italic><gray>Click to toggle listed in Party Finder.</gray>")
                ));
                toggle.setItemMeta(toggleMeta);
            }
            inventory.setItem(31, toggle);

            // Slot 34: Disband Party
            ItemStack disband = new ItemStack(Material.TNT);
            ItemMeta disMeta = disband.getItemMeta();
            if (disMeta != null) {
                disMeta.displayName(MiniMessage.miniMessage().deserialize("<!italic><red><bold>Disband Party</bold></red>"));
                disband.setItemMeta(disMeta);
            }
            inventory.setItem(34, disband);
        } else {
            // Member controls
            // Slot 31: Leave Party
            ItemStack leave = new ItemStack(Material.BARRIER);
            ItemMeta leaveMeta = leave.getItemMeta();
            if (leaveMeta != null) {
                leaveMeta.displayName(MiniMessage.miniMessage().deserialize("<!italic><red><bold>Leave Party</bold></red>"));
                leave.setItemMeta(leaveMeta);
            }
            inventory.setItem(31, leave);
        }

        // Admin controls
        if (isAdmin) {
            // Slot 29: Start Game for All (Lobby)
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
            inventory.setItem(29, allStart);

            // Slot 33: Stop All Matches
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
            inventory.setItem(33, stopItem);
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!event.getInventory().equals(inventory)) return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;
        Party party = partyManager.getParty(player.getUniqueId());
        if (party == null) {
            player.closeInventory();
            return;
        }

        boolean isLeader = party.getLeaderUuid().equals(player.getUniqueId());
        boolean isAdmin = player.hasPermission("findtheblock.admin");
        int slot = event.getRawSlot();

        if (isLeader && slot == 28) { // Start Match Game Mode Selection
            player.closeInventory();
            new DifficultySelectMenu(plugin, gameManager, partyManager, player, DifficultySelectMenu.StartTarget.PARTY).open();
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.0f);
        } else if (isAdmin && slot == 29) { // Admin: Start Game for All (Lobby)
            player.closeInventory();
            new DifficultySelectMenu(plugin, gameManager, partyManager, player, DifficultySelectMenu.StartTarget.ALL_LOBBY).open();
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.0f);
        } else if (isLeader && slot == 31) { // Toggle privacy
            partyManager.setPublic(player, !party.isPublic());
            populate();
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
        } else if (!isLeader && slot == 31) { // Leave
            partyManager.leaveParty(player);
            player.closeInventory();
        } else if (isAdmin && slot == 33) { // Admin: Stop All Matches
            player.closeInventory();
            if (gameManager.getState() == GameState.LOBBY) {
                Chat.send(player, "<yellow>No match running.</yellow>");
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.0f);
            } else {
                gameManager.forceStop(player);
                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.8f, 1.0f);
            }
        } else if (isLeader && slot == 34) { // Disband
            partyManager.disbandParty(player.getUniqueId());
            player.closeInventory();
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getInventory().equals(inventory)) {
            HandlerList.unregisterAll(this);
        }
    }
}
