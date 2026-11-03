package com.findtheblock.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.time.Duration;

/**
 * Modern Kyori Adventure & MiniMessage chat utility.
 * Seamlessly supports gradients, hover tooltips, and click actions across all modern Paper platforms.
 */
public final class Chat {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.legacyAmpersand();

    private Chat() {}

    /**
     * Converts a string into an Adventure Component.
     * Supports MiniMessage tags (e.g. <green>, <gradient:#FFAA00:#FF5555>) and legacy & codes.
     */
    public static Component toComponent(String message) {
        if (message == null || message.isEmpty()) {
            return Component.empty();
        }
        // If message contains legacy '&' symbols, convert legacy format first
        if (message.contains("&")) {
            return LEGACY_SERIALIZER.deserialize(message);
        }
        return MINI_MESSAGE.deserialize(message);
    }

    public static void send(CommandSender sender, String message) {
        if (sender != null && message != null) {
            sender.sendMessage(toComponent(message));
        }
    }

    public static void broadcast(String message) {
        if (message != null) {
            Bukkit.broadcast(toComponent(message));
        }
    }

    public static void sendActionBar(Player player, String message) {
        if (player != null && player.isOnline() && message != null) {
            player.sendActionBar(toComponent(message));
        }
    }

    public static void sendTitle(Player player, String title, String subtitle, int fadeInTicks, int stayTicks, int fadeOutTicks) {
        if (player == null || !player.isOnline()) return;

        Title.Times times = Title.Times.times(
                Duration.ofMillis(fadeInTicks * 50L),
                Duration.ofMillis(stayTicks * 50L),
                Duration.ofMillis(fadeOutTicks * 50L)
        );

        Title titleObj = Title.title(toComponent(title), toComponent(subtitle), times);
        player.showTitle(titleObj);
    }

    public static void playSound(Player player, Sound sound, float volume, float pitch) {
        if (player != null && player.isOnline() && sound != null) {
            player.playSound(player.getLocation(), sound, volume, pitch);
        }
    }
}
