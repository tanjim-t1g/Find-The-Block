package com.findtheblock.util;

import com.findtheblock.FindTheBlock;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

public class UpdateChecker {

    private final FindTheBlock plugin;
    private boolean updateAvailable = false;
    private String latestVersion = "";
    private String downloadUrl = "https://modrinth.com/project/findtheblock";
    private String githubUrl = "https://github.com/tanjim-t1g/Find-The-Block/releases/latest";
    private String minMinecraft = "1.21.0";

    public UpdateChecker(FindTheBlock plugin) {
        this.plugin = plugin;
    }

    public void checkForUpdates() {
        if (!plugin.getConfigManager().isUpdateCheckerEnabled()) {
            return;
        }

        Bukkit.getAsyncScheduler().runNow(plugin, task -> {
            try {
                URL url = URI.create("https://api.github.com/repos/tanjim-t1g/Find-The-Block/releases/latest").toURL();
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("User-Agent", "FindTheBlock-UpdateChecker");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                if (connection.getResponseCode() == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    String json = response.toString();
                    latestVersion = extractJsonField(json, "tag_name");
                    if (latestVersion.startsWith("v") || latestVersion.startsWith("V")) {
                        latestVersion = latestVersion.substring(1);
                    }

                    String body = extractJsonField(json, "body");
                    minMinecraft = parseMinMinecraft(body);

                    if (!isServerCompatible(minMinecraft)) {
                        return;
                    }

                    String currentVersion = plugin.getDescription().getVersion();
                    if (compareVersions(latestVersion, currentVersion) > 0) {
                        updateAvailable = true;
                        printConsoleBanner(currentVersion, latestVersion);
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to check for updates: " + e.getMessage());
            }
        });
    }

    private boolean isServerCompatible(String minMC) {
        try {
            String serverVersion = Bukkit.getMinecraftVersion(); // e.g. "1.21.1" or "1.21"
            return compareVersions(serverVersion, minMC) >= 0;
        } catch (Exception e) {
            return true; // default allow if parsing fails
        }
    }

    private String parseMinMinecraft(String body) {
        if (body == null) return "1.21.0";
        for (String line : body.split("\n")) {
            if (line.toLowerCase().contains("min-minecraft:")) {
                String[] parts = line.split(":");
                if (parts.length > 1) {
                    return parts[1].trim();
                }
            }
        }
        return "1.21.0";
    }

    private int compareVersions(String v1, String v2) {
        try {
            String[] parts1 = v1.split("-")[0].split("\\.");
            String[] parts2 = v2.split("-")[0].split("\\.");
            int length = Math.max(parts1.length, parts2.length);
            for (int i = 0; i < length; i++) {
                int p1 = i < parts1.length ? Integer.parseInt(parts1[i].replaceAll("[^0-9]", "")) : 0;
                int p2 = i < parts2.length ? Integer.parseInt(parts2[i].replaceAll("[^0-9]", "")) : 0;
                if (p1 != p2) {
                    return Integer.compare(p1, p2);
                }
            }
        } catch (Exception ignored) {}
        return v1.compareTo(v2);
    }

    private String extractJsonField(String json, String field) {
        String search = "\"" + field + "\":";
        int idx = json.indexOf(search);
        if (idx == -1) return "";
        int start = json.indexOf("\"", idx + search.length());
        if (start == -1) return "";
        int end = json.indexOf("\"", start + 1);
        if (end == -1) return "";
        return json.substring(start + 1, end);
    }

    private void printConsoleBanner(String current, String latest) {
        plugin.getLogger().info("==================================================");
        plugin.getLogger().info("  FindTheBlock v" + latest + " is available!");
        plugin.getLogger().info("  You are currently running v" + current);
        plugin.getLogger().info("  Download: " + downloadUrl);
        plugin.getLogger().info("==================================================");
    }

    public void sendOpNotification(Player player) {
        if (!updateAvailable) return;

        player.sendMessage(Component.text("--------------------------------------------------", NamedTextColor.DARK_GRAY));
        player.sendMessage(Component.text(" FindTheBlock ", NamedTextColor.GOLD, TextDecoration.BOLD)
                .append(Component.text("v" + latestVersion, NamedTextColor.GREEN))
                .append(Component.text(" is available!", NamedTextColor.YELLOW)));
        
        Component links = Component.text("[Modrinth]", NamedTextColor.GREEN, TextDecoration.BOLD)
                .clickEvent(ClickEvent.openUrl(downloadUrl))
                .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                .append(Component.text("[GitHub]", NamedTextColor.AQUA, TextDecoration.BOLD)
                .clickEvent(ClickEvent.openUrl(githubUrl)));
        
        player.sendMessage(links);
        player.sendMessage(Component.text("--------------------------------------------------", NamedTextColor.DARK_GRAY));
    }

    public boolean isUpdateAvailable() {
        return updateAvailable;
    }
}
