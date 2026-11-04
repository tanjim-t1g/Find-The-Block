package com.findtheblock.listener;

import com.findtheblock.config.ConfigManager;
import com.findtheblock.game.GameManager;
import com.findtheblock.game.GameState;
import com.findtheblock.lobby.LobbyManager;
import com.findtheblock.party.PartyManager;
import com.findtheblock.util.Chat;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Handles player logins, mid-game connection routing, and 60-second graceful reconnects.
 * Disconnected party leaders cause their party to disband and vanish from Party Finder.
 */
public class ConnectionListener implements Listener {

    private final GameManager gameManager;
    private final LobbyManager lobbyManager;
    private final ConfigManager config;
    private final PartyManager partyManager;

    public ConnectionListener(GameManager gameManager, LobbyManager lobbyManager,
                              ConfigManager config, PartyManager partyManager) {
        this.gameManager = gameManager;
        this.lobbyManager = lobbyManager;
        this.config = config;
        this.partyManager = partyManager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        long graceMs = config.getReconnectGraceSeconds() * 1000L;

        // Hide any active spectators from this player
        gameManager.getSpectatorManager().hideSpectatorsFrom(player);

        if (gameManager.getState() == GameState.IN_GAME) {
            // Check if player is reconnecting within the 60-second grace window
            if (gameManager.getReconnectTracker().hasSavedSession(player.getUniqueId(), graceMs)) {
                gameManager.getReconnectTracker().restore(player, graceMs);
                Chat.sendTitle(player, "<green><bold>WELCOME BACK</bold></green>", "<gray>Reconnected to match</gray>", 10, 40, 10);
                Chat.broadcast("<yellow>" + player.getName() + " reconnected.</yellow>");
                return;
            }

            // If disconnected player did not connect within 60 seconds, treat as Late Joiner
            gameManager.getContestants().remove(player.getUniqueId());

            // Teleport to lobby, clear temporary match state, and offer late joiner options
            lobbyManager.teleportToLobby(player);
            Chat.send(player, "<gold>[FTB]</gold> <yellow>Match in progress! Target: <gold>" + gameManager.getCurrentTarget().name() + "</gold></yellow>");
            Chat.send(player, "  <green><bold><click:run_command:/ftb join>[ Join Race ]</click></bold></green>  <aqua><bold><click:run_command:/ftb spectate>[ Spectate ]</click></bold></aqua>");
        } else {
            // Normal lobby join
            lobbyManager.teleportToLobby(player);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        // Restore spectator if quitting while in spectator mode
        if (gameManager.getSpectatorManager().isSpectator(player.getUniqueId())) {
            gameManager.getSpectatorManager().restorePlayer(player);
        }

        // Save session if in an active match
        if (gameManager.getState() == GameState.IN_GAME && gameManager.getContestants().contains(player.getUniqueId())) {
            gameManager.getReconnectTracker().save(player);
        }

        // If party leader disconnects, disband party and unlist from party finder
        if (partyManager.isPartyLeader(player.getUniqueId())) {
            partyManager.disbandParty(player.getUniqueId());
        } else if (partyManager.getParty(player.getUniqueId()) != null) {
            partyManager.leaveParty(player);
        }
    }
}
