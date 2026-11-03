package com.findtheblock.ui;

import com.findtheblock.util.Chat;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.util.Set;

/**
 * Modern Paper BossBar showing live match progress and target objective.
 */
public class BossBarController {

    private BossBar bossBar;
    private boolean active = false;

    public void start(String targetName, int maxWinners) {
        Component title = Chat.toComponent("<gold><bold>Target:</bold></gold> <yellow>" + targetName +
                "</yellow> <dark_gray>|</dark_gray> <gray>Time: <white>00:00</white></gray>");

        this.bossBar = BossBar.bossBar(title, 1.0f, BossBar.Color.YELLOW, BossBar.Overlay.PROGRESS);
        this.active = true;
    }

    public void update(String targetName, int elapsedSeconds, int winnerCount, int maxWinners) {
        if (!active || bossBar == null) return;

        int m = elapsedSeconds / 60;
        int s = elapsedSeconds % 60;
        String timeStr = String.format("%02d:%02d", m, s);

        Component title = Chat.toComponent("<gold><bold>Target:</bold></gold> <yellow>" + targetName +
                "</yellow> <dark_gray>|</dark_gray> <gray>Time: <white>" + timeStr +
                "</white></gray> <dark_gray>|</dark_gray> <green>Winners: " + winnerCount + "/" + maxWinners + "</green>");

        bossBar.name(title);
    }

    public void addPlayer(Player player) {
        if (active && bossBar != null && player != null && player.isOnline()) {
            player.showBossBar(bossBar);
        }
    }

    public void removePlayer(Player player) {
        if (bossBar != null && player != null && player.isOnline()) {
            player.hideBossBar(bossBar);
        }
    }

    public void hideAll(Set<Player> players) {
        if (bossBar != null) {
            for (Player p : players) {
                if (p != null && p.isOnline()) {
                    p.hideBossBar(bossBar);
                }
            }
        }
        this.active = false;
        this.bossBar = null;
    }
}
