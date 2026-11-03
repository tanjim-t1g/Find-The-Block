package com.findtheblock.listener;

import com.findtheblock.factory.MatchWorldPair;
import com.findtheblock.game.GameManager;
import com.findtheblock.lobby.LobbyManager;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * Separates chat channels between the dedicated void lobby and active match instances.
 * Lobby players chat with lobby players, while racers and spectators communicate within their race.
 */
public class ChatListener implements Listener {

    private final GameManager gameManager;
    private final LobbyManager lobbyManager;

    public ChatListener(GameManager gameManager, LobbyManager lobbyManager) {
        this.gameManager = gameManager;
        this.lobbyManager = lobbyManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        Player sender = event.getPlayer();
        World world = sender.getWorld();
        World lobbyWorld = lobbyManager.getLobbyWorld();
        MatchWorldPair match = gameManager.getActiveMatch();

        boolean inLobby = lobbyWorld != null && world.equals(lobbyWorld);
        boolean inMatch = gameManager.isGameRunning() && match != null &&
                (match.containsWorld(world) || gameManager.getContestants().contains(sender.getUniqueId()) || gameManager.getSpectatorManager().isSpectator(sender.getUniqueId()));

        if (inLobby) {
            // Restrict message to players currently in the lobby world; console audience is preserved
            event.viewers().removeIf(audience -> {
                if (audience instanceof Player p) {
                    return !p.getWorld().equals(lobbyWorld);
                }
                return false;
            });

            event.renderer((source, sourceDisplayName, message, viewer) ->
                MiniMessage.miniMessage().deserialize("<dark_gray>[Lobby]</dark_gray> <gray>" + source.getName() + ":</gray> ")
                        .append(message)
            );
        } else if (inMatch) {
            boolean isSpectator = gameManager.getSpectatorManager().isSpectator(sender.getUniqueId());
            String tag = isSpectator ? "<gray>[Spectator]</gray> " : "<gold>[Race]</gold> ";

            // Restrict message to racers and spectators in this match
            event.viewers().removeIf(audience -> {
                if (audience instanceof Player p) {
                    boolean pInMatch = match.containsWorld(p.getWorld())
                            || gameManager.getContestants().contains(p.getUniqueId())
                            || gameManager.getSpectatorManager().isSpectator(p.getUniqueId());
                    return !pInMatch;
                }
                return false;
            });

            event.renderer((source, sourceDisplayName, message, viewer) ->
                MiniMessage.miniMessage().deserialize(tag + "<yellow>" + source.getName() + ":</yellow> <white>")
                        .append(message)
                        .append(MiniMessage.miniMessage().deserialize("</white>"))
            );
        }
    }
}
