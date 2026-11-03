package com.findtheblock.game;

import com.findtheblock.FindTheBlock;
import com.findtheblock.util.Chat;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages spectator mode, hotbar teleporter compass, and player teleportation.
 * Spectators use Adventure flight with vanish and invulnerability so their hotbar remains visible and interactive.
 */
public class SpectatorManager {

    private final FindTheBlock plugin;
    private final Set<UUID> spectators = ConcurrentHashMap.newKeySet();

    public static final String COMPASS_TITLE = "Teleport to Racer";

    public SpectatorManager(FindTheBlock plugin) {
        this.plugin = plugin;
    }

    public void makeSpectator(Player player, Location arenaLocation) {
        if (player == null || !player.isOnline()) return;

        spectators.add(player.getUniqueId());

        // Adventure mode keeps the hotbar visible to the client so the compass can be held and used
        player.setGameMode(GameMode.ADVENTURE);
        player.getInventory().clear();
        player.setAllowFlight(true);
        player.setFlying(true);
        player.setFlySpeed(0.15f);
        player.setInvisible(true);
        player.setInvulnerable(true);
        player.setCollidable(false);
        player.setSilent(true);
        player.setHealth(20.0);
        player.setFoodLevel(20);
        player.setSaturation(20.0f);
        player.setFireTicks(0);
        player.getActivePotionEffects().forEach(e -> player.removePotionEffect(e.getType()));

        // Hide spectator from alive contestants and non-spectator players
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!spectators.contains(online.getUniqueId())) {
                online.hidePlayer(plugin, player);
            }
        }

        // Give Teleport Compass in hotbar slot 0
        ItemStack compass = new ItemStack(Material.COMPASS);
        ItemMeta meta = compass.getItemMeta();
        if (meta != null) {
            meta.displayName(Chat.toComponent("<!italic><gold><bold>" + COMPASS_TITLE + "</bold></gold> <gray>(Right-Click)</gray>"));
            meta.lore(List.of(Chat.toComponent("<!italic><gray>Right-click to teleport to an active racer!</gray>")));
            compass.setItemMeta(meta);
        }
        player.getInventory().setItem(0, compass);

        if (arenaLocation != null) {
            player.teleport(arenaLocation);
        }

        Chat.sendTitle(player, "<gold><bold>SPECTATOR MODE</bold></gold>", "<gray>Right-click compass to view racers</gray>", 10, 40, 10);
    }

    public void restorePlayer(Player player) {
        if (player == null || !player.isOnline()) return;

        spectators.remove(player.getUniqueId());
        player.setAllowFlight(false);
        player.setFlying(false);
        player.setFlySpeed(0.1f);
        player.setInvisible(false);
        player.setInvulnerable(false);
        player.setCollidable(true);
        player.setSilent(false);
        player.getInventory().clear();

        for (Player online : Bukkit.getOnlinePlayers()) {
            online.showPlayer(plugin, player);
        }
    }

    public void openSpectatorMenu(Player spectator, Set<UUID> activeContestants) {
        SpectatorMenuHolder holder = new SpectatorMenuHolder();
        Inventory inv = Bukkit.createInventory(holder, 27, Chat.toComponent("<dark_gray>Spectate Racers</dark_gray>"));
        holder.setInventory(inv);

        int slot = 0;
        for (UUID uuid : activeContestants) {
            if (spectators.contains(uuid)) continue;
            Player target = Bukkit.getPlayer(uuid);
            if (target != null && target.isOnline()) {
                ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
                SkullMeta meta = (SkullMeta) skull.getItemMeta();
                if (meta != null) {
                    meta.setOwningPlayer(target);
                    meta.displayName(Chat.toComponent("<!italic><yellow><bold>" + target.getName() + "</bold></yellow>"));
                    meta.lore(List.of(Chat.toComponent("<!italic><gray>Click to teleport to this racer</gray>")));
                    skull.setItemMeta(meta);
                }
                inv.setItem(slot++, skull);
                if (slot >= 27) break;
            }
        }

        if (slot == 0) {
            ItemStack empty = new ItemStack(Material.BARRIER);
            ItemMeta eMeta = empty.getItemMeta();
            if (eMeta != null) {
                eMeta.displayName(Chat.toComponent("<!italic><red><bold>No Active Racers</bold></red>"));
                eMeta.lore(List.of(Chat.toComponent("<!italic><gray>All racers have finished or disconnected.</gray>")));
                empty.setItemMeta(eMeta);
            }
            inv.setItem(13, empty);
        }

        spectator.openInventory(inv);
    }

    public void hideSpectatorsFrom(Player player) {
        if (player == null || !player.isOnline()) return;
        for (UUID specId : spectators) {
            Player spec = Bukkit.getPlayer(specId);
            if (spec != null && spec.isOnline()) {
                player.hidePlayer(plugin, spec);
            }
        }
    }

    public boolean isSpectator(UUID uuid) {
        return spectators.contains(uuid);
    }

    public Set<UUID> getSpectators() {
        return Collections.unmodifiableSet(spectators);
    }

    public void removeSpectator(UUID uuid) {
        spectators.remove(uuid);
    }

    public void clear() {
        for (UUID uuid : spectators) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                restorePlayer(p);
            }
        }
        spectators.clear();
    }
}
