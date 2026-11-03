package com.findtheblock.ui;

import com.findtheblock.FindTheBlock;
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
 * Interactive Party Finder GUI for discovering, creating, and joining public parties.
 */
public class PartyFinderMenu implements Listener {

    private final FindTheBlock plugin;
    private final PartyManager partyManager;
    private final Player viewer;
    private final Inventory inventory;
    private final Map<Integer, UUID> slotToLeaderMap = new HashMap<>();

    public PartyFinderMenu(FindTheBlock plugin, PartyManager partyManager, Player viewer) {
        this.plugin = plugin;
        this.partyManager = partyManager;
        this.viewer = viewer;
        this.inventory = Bukkit.createInventory(null, 54,
                MiniMessage.miniMessage().deserialize("<gold><bold>Party Finder</bold></gold>"));
    }

    public void open() {
        populate();
        Bukkit.getPluginManager().registerEvents(this, plugin);
        viewer.openInventory(inventory);
    }

    private void populate() {
        inventory.clear();
        slotToLeaderMap.clear();

        List<Party> publicParties = partyManager.getPublicParties();
        int slot = 0;

        for (Party party : publicParties) {
            if (slot >= 45) break;

            ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) skull.getItemMeta();
            if (meta != null) {
                Player leader = Bukkit.getPlayer(party.getLeaderUuid());
                if (leader != null) meta.setOwningPlayer(leader);

                meta.displayName(MiniMessage.miniMessage().deserialize("<!italic><yellow><bold>" + party.getLeaderName() + "'s Party</bold></yellow>"));

                List<Component> lore = new ArrayList<>();
                lore.add(MiniMessage.miniMessage().deserialize("<!italic><gray>Members: <green>" + party.size() + " / " + party.getMaxSize() + "</green></gray>"));
                lore.add(MiniMessage.miniMessage().deserialize("<!italic><dark_gray>---------------------</dark_gray>"));

                for (UUID memberUuid : party.getMembers()) {
                    Player m = Bukkit.getPlayer(memberUuid);
                    String name = m != null ? m.getName() : "Offline";
                    String role = memberUuid.equals(party.getLeaderUuid()) ? " <gold>(Leader)</gold>" : "";
                    lore.add(MiniMessage.miniMessage().deserialize("<!italic><gray>• " + name + role + "</gray>"));
                }

                lore.add(MiniMessage.miniMessage().deserialize("<!italic><dark_gray>---------------------</dark_gray>"));
                lore.add(MiniMessage.miniMessage().deserialize("<!italic><yellow><bold>Click to Join!</bold></yellow>"));

                meta.lore(lore);
                skull.setItemMeta(meta);
            }

            inventory.setItem(slot, skull);
            slotToLeaderMap.put(slot, party.getLeaderUuid());
            slot++;
        }

        if (publicParties.isEmpty()) {
            ItemStack empty = new ItemStack(Material.STRUCTURE_VOID);
            ItemMeta meta = empty.getItemMeta();
            if (meta != null) {
                meta.displayName(MiniMessage.miniMessage().deserialize("<!italic><gray>No public parties open.</gray>"));
                meta.lore(Collections.singletonList(
                        MiniMessage.miniMessage().deserialize("<!italic><dark_gray>Click below to create your own!</dark_gray>")
                ));
                empty.setItemMeta(meta);
            }
            inventory.setItem(22, empty);
        }

        // Bottom control row
        // Slot 45: Create Public Party
        ItemStack pub = new ItemStack(Material.EMERALD_BLOCK);
        ItemMeta pubMeta = pub.getItemMeta();
        if (pubMeta != null) {
            pubMeta.displayName(MiniMessage.miniMessage().deserialize("<!italic><green><bold>+ Create Public Party</bold></green>"));
            pubMeta.lore(Collections.singletonList(
                    MiniMessage.miniMessage().deserialize("<!italic><gray>Listed publicly in the Party Finder.</gray>")
            ));
            pub.setItemMeta(pubMeta);
        }
        inventory.setItem(45, pub);

        // Slot 46: Create Private Party
        ItemStack priv = new ItemStack(Material.IRON_BLOCK);
        ItemMeta privMeta = priv.getItemMeta();
        if (privMeta != null) {
            privMeta.displayName(MiniMessage.miniMessage().deserialize("<!italic><yellow><bold>+ Create Private Party</bold></yellow>"));
            privMeta.lore(Collections.singletonList(
                    MiniMessage.miniMessage().deserialize("<!italic><gray>Unlisted; invite-only with /ftb party invite.</gray>")
            ));
            priv.setItemMeta(privMeta);
        }
        inventory.setItem(46, priv);

        // Slot 49: Refresh
        ItemStack refresh = new ItemStack(Material.NETHER_STAR);
        ItemMeta refMeta = refresh.getItemMeta();
        if (refMeta != null) {
            refMeta.displayName(MiniMessage.miniMessage().deserialize("<!italic><aqua><bold>Refresh List</bold></aqua>"));
            refresh.setItemMeta(refMeta);
        }
        inventory.setItem(49, refresh);

        // Slot 53: Close / Back
        ItemStack back = new ItemStack(Material.BARRIER);
        ItemMeta backMeta = back.getItemMeta();
        if (backMeta != null) {
            backMeta.displayName(MiniMessage.miniMessage().deserialize("<!italic><red><bold>Close</bold></red>"));
            back.setItemMeta(backMeta);
        }
        inventory.setItem(53, back);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!event.getInventory().equals(inventory)) return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;
        int slot = event.getRawSlot();

        if (slotToLeaderMap.containsKey(slot)) {
            UUID leaderUuid = slotToLeaderMap.get(slot);
            partyManager.joinPublicParty(player, leaderUuid);
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.2f);
            player.closeInventory();
            return;
        }

        if (slot == 45) { // Create Public
            if (partyManager.createParty(player, true)) {
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f);
            }
            player.closeInventory();
        } else if (slot == 46) { // Create Private
            if (partyManager.createParty(player, false)) {
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
            }
            player.closeInventory();
        } else if (slot == 49) { // Refresh
            populate();
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.0f);
        } else if (slot == 53) { // Close
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
