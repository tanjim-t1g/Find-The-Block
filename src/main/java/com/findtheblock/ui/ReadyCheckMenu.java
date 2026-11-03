package com.findtheblock.ui;

import com.findtheblock.FindTheBlock;
import com.findtheblock.config.DifficultyTier;
import com.findtheblock.game.GameManager;
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
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 20-second interactive invitation/ready-check GUI before a match starts.
 */
public class ReadyCheckMenu implements Listener {

    private final FindTheBlock plugin;
    private final GameManager gameManager;
    private final Player leader;
    private final DifficultyTier tier;
    private final List<Player> targetPlayers;

    private final Set<UUID> accepted = ConcurrentHashMap.newKeySet();
    private final Set<UUID> declined = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Inventory> openInventories = new ConcurrentHashMap<>();

    private int secondsRemaining = 20;
    private BukkitTask timerTask;
    private boolean finished = false;

    public ReadyCheckMenu(FindTheBlock plugin, GameManager gameManager, Player leader,
                          DifficultyTier tier, List<Player> targetPlayers) {
        this.plugin = plugin;
        this.gameManager = gameManager;
        this.leader = leader;
        this.tier = tier;
        this.targetPlayers = new ArrayList<>(targetPlayers);
    }

    public void start() {
        if (targetPlayers.isEmpty()) return;

        // Register listener for clicks
        Bukkit.getPluginManager().registerEvents(this, plugin);

        for (Player p : targetPlayers) {
            if (p != null && p.isOnline()) {
                Inventory inv = createInventory(secondsRemaining);
                openInventories.put(p.getUniqueId(), inv);
                p.openInventory(inv);
                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.2f);
            }
        }

        // 20-second tick task
        this.timerTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            secondsRemaining--;

            if (secondsRemaining <= 0 || (accepted.size() + declined.size() >= targetPlayers.size())) {
                finishReadyCheck();
                return;
            }

            // Update clock item in all open inventories
            for (Map.Entry<UUID, Inventory> entry : openInventories.entrySet()) {
                Player p = Bukkit.getPlayer(entry.getKey());
                if (p != null && p.isOnline()) {
                    entry.getValue().setItem(13, createClockItem(secondsRemaining));
                    if (secondsRemaining <= 5) {
                        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.8f, 1.5f);
                    }
                }
            }
        }, 20L, 20L);
    }

    private Inventory createInventory(int seconds) {
        Inventory inv = Bukkit.createInventory(null, 27,
                MiniMessage.miniMessage().deserialize("<gold><bold>Ready to Race?</bold></gold> <yellow>(" + seconds + "s)</yellow>"));

        // Slot 11: Accept
        ItemStack accept = new ItemStack(Material.LIME_CONCRETE);
        ItemMeta acceptMeta = accept.getItemMeta();
        if (acceptMeta != null) {
            acceptMeta.displayName(MiniMessage.miniMessage().deserialize("<!italic><green><bold>Accept & Join Match</bold></green>"));
            acceptMeta.lore(Collections.singletonList(
                    MiniMessage.miniMessage().deserialize("<!italic><gray>Click to confirm you want to play!</gray>")
            ));
            accept.setItemMeta(acceptMeta);
        }
        inv.setItem(11, accept);

        // Slot 13: Clock
        inv.setItem(13, createClockItem(seconds));

        // Slot 15: Decline
        ItemStack decline = new ItemStack(Material.RED_CONCRETE);
        ItemMeta declineMeta = decline.getItemMeta();
        if (declineMeta != null) {
            declineMeta.displayName(MiniMessage.miniMessage().deserialize("<!italic><red><bold>Decline & Stay in Lobby</bold></red>"));
            declineMeta.lore(Collections.singletonList(
                    MiniMessage.miniMessage().deserialize("<!italic><gray>Click to stay in the lobby as a spectator.</gray>")
            ));
            decline.setItemMeta(declineMeta);
        }
        inv.setItem(15, decline);

        return inv;
    }

    private ItemStack createClockItem(int seconds) {
        ItemStack clock = new ItemStack(Material.CLOCK);
        ItemMeta meta = clock.getItemMeta();
        if (meta != null) {
            meta.displayName(MiniMessage.miniMessage().deserialize("<!italic><yellow><bold>Time Remaining: " + seconds + "s</bold></yellow>"));
            meta.lore(Arrays.asList(
                    MiniMessage.miniMessage().deserialize("<!italic><gray>Unconfirmed players will</gray>"),
                    MiniMessage.miniMessage().deserialize("<!italic><gray>automatically decline.</gray>")
            ));
            clock.setItemMeta(meta);
        }
        return clock;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!openInventories.containsKey(player.getUniqueId())) return;

        event.setCancelled(true);
        ItemStack item = event.getCurrentItem();
        if (item == null) return;

        if (item.getType() == Material.LIME_CONCRETE) {
            accepted.add(player.getUniqueId());
            declined.remove(player.getUniqueId());
            Chat.send(player, "<green>Accepted. Waiting for others...</green>");
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
            player.closeInventory();
        } else if (item.getType() == Material.RED_CONCRETE) {
            declined.add(player.getUniqueId());
            accepted.remove(player.getUniqueId());
            Chat.send(player, "<yellow>Declined match.</yellow>");
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            player.closeInventory();
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        openInventories.remove(player.getUniqueId());
    }

    private synchronized void finishReadyCheck() {
        if (finished) return;
        this.finished = true;

        if (timerTask != null) timerTask.cancel();
        HandlerList.unregisterAll(this);

        // Close any remaining open inventories
        for (UUID uuid : openInventories.keySet()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                p.closeInventory();
            }
        }
        openInventories.clear();

        // Any player who didn't answer within 20s is treated as declined
        List<Player> confirmedRacers = new ArrayList<>();
        for (Player p : targetPlayers) {
            if (p != null && p.isOnline()) {
                if (accepted.contains(p.getUniqueId())) {
                    confirmedRacers.add(p);
                } else {
                    Chat.send(p, "<yellow>Match invitation timed out.</yellow>");
                }
            }
        }

        if (confirmedRacers.size() < 2) {
            Chat.broadcast("<red>[Race] Cancelled: At least 2 players needed.</red>");
            return;
        }

        Chat.broadcast("<green>[Race] " + confirmedRacers.size() +
                " players ready. Starting...</green>");

        // Launch the match roulette & countdown with confirmed racers
        gameManager.startMatchWithPlayers(leader, tier, confirmedRacers);
    }
}
