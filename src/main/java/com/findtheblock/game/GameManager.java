package com.findtheblock.game;

import com.findtheblock.FindTheBlock;
import com.findtheblock.config.ConfigManager;
import com.findtheblock.config.DifficultyTier;
import com.findtheblock.factory.MatchWorldPair;
import com.findtheblock.factory.WorldFactory;
import com.findtheblock.lobby.LobbyManager;
import com.findtheblock.party.PartyManager;
import com.findtheblock.stats.StatsManager;
import com.findtheblock.ui.BossBarController;
import com.findtheblock.ui.RecipeMenu;
import com.findtheblock.ui.ReadyCheckMenu;
import com.findtheblock.ui.RouletteMenu;
import com.findtheblock.util.Chat;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Core Minigame Engine.
 * Handles state transitions, game loops, win conditions, and clean resets.
 */
public class GameManager {

    private final FindTheBlock plugin;
    private final ConfigManager config;
    private final WorldFactory worldFactory;
    private final LobbyManager lobbyManager;
    private final TargetEngine targetEngine;
    private final PartyManager partyManager;
    private final StatsManager statsManager;
    private final SpectatorManager spectatorManager;
    private final ReconnectTracker reconnectTracker;
    private final BossBarController bossBarController;

    private GameState state = GameState.LOBBY;
    private MatchWorldPair activeMatch = null;
    private Material currentTarget = null;
    private DifficultyTier activeTier = DifficultyTier.NORMAL;

    private final Set<UUID> contestants = ConcurrentHashMap.newKeySet();
    private final List<UUID> winners = new ArrayList<>();
    private final Map<UUID, Integer> finishTimes = new HashMap<>();

    private int elapsedSeconds = 0;
    private boolean twentyMinBuffsAwarded = false;
    private boolean adminStarted = false;
    private int underpopulatedSeconds = 0;
    private BukkitTask gameTimerTask = null;

    public GameManager(FindTheBlock plugin, ConfigManager config, WorldFactory worldFactory,
                       LobbyManager lobbyManager, TargetEngine targetEngine, PartyManager partyManager,
                       StatsManager statsManager, SpectatorManager spectatorManager,
                       ReconnectTracker reconnectTracker) {
        this.plugin = plugin;
        this.config = config;
        this.worldFactory = worldFactory;
        this.lobbyManager = lobbyManager;
        this.targetEngine = targetEngine;
        this.partyManager = partyManager;
        this.statsManager = statsManager;
        this.spectatorManager = spectatorManager;
        this.reconnectTracker = reconnectTracker;
        this.bossBarController = new BossBarController();
    }

    public void initiatePartyMatch(Player initiator) {
        initiatePartyMatch(initiator, config.getDefaultTier());
    }

    public void initiatePartyMatch(Player initiator, DifficultyTier tier) {
        if (state != GameState.LOBBY) {
            Chat.send(initiator, "<red>A match is already in progress.</red>");
            return;
        }

        if (partyManager.getParty(initiator.getUniqueId()) == null) {
            Chat.send(initiator, "<red>You must be in a party to start.</red>");
            return;
        }

        if (!partyManager.isPartyLeader(initiator.getUniqueId())) {
            Chat.send(initiator, "<red>Only the party leader can start.</red>");
            return;
        }

        List<Player> targetPlayers = new ArrayList<>();
        for (UUID uuid : partyManager.getPartyMembers(initiator.getUniqueId())) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) targetPlayers.add(p);
        }

        if (targetPlayers.size() < 2) {
            Chat.send(initiator, "<red>You need at least 2 online party members to start.</red>");
            return;
        }

        DifficultyTier selectedTier = tier != null ? tier : config.getDefaultTier();

        Chat.broadcast("<gold>[FTB]</gold> <yellow>" + initiator.getName() +
                " started a " + selectedTier.name() + " match! Accept in menu (20s).</yellow>");
        new ReadyCheckMenu(plugin, this, initiator, selectedTier, targetPlayers).start();
    }

    public synchronized void startAdminMatchForAll(CommandSender sender, DifficultyTier tier) {
        if (state != GameState.LOBBY) {
            Chat.send(sender, "<red>A match is already in progress.</red>");
            return;
        }

        List<Player> lobbyPlayers = new ArrayList<>();
        World lobbyWorld = lobbyManager.getLobbyWorld();
        if (lobbyWorld != null) {
            for (Player p : lobbyWorld.getPlayers()) {
                if (p != null && p.isOnline()) {
                    lobbyPlayers.add(p);
                }
            }
        } else {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p != null && p.isOnline()) {
                    lobbyPlayers.add(p);
                }
            }
        }

        if (lobbyPlayers.isEmpty()) {
            Chat.send(sender, "<red>No players in the lobby to start a match.</red>");
            return;
        }

        DifficultyTier selectedTier = tier != null ? tier : config.getDefaultTier();
        Chat.broadcast("<gold>[FTB]</gold> <yellow>Admin started a " +
                selectedTier.name() + " match for lobby players.</yellow>");
        startMatchWithPlayers(sender, selectedTier, lobbyPlayers, true);
    }

    public synchronized void startMatchWithPlayers(CommandSender sender, DifficultyTier tier, List<Player> startingPlayers) {
        startMatchWithPlayers(sender, tier, startingPlayers, false);
    }

    public synchronized void startMatchWithPlayers(CommandSender sender, DifficultyTier tier, List<Player> startingPlayers, boolean adminStarted) {
        if (state != GameState.LOBBY) {
            Chat.send(sender, "<red>A match is already running or starting!</red>");
            return;
        }

        this.adminStarted = adminStarted;
        this.underpopulatedSeconds = 0;
        this.state = GameState.STARTING;
        this.activeTier = tier != null ? tier : config.getDefaultTier();
        this.currentTarget = targetEngine.pickRandomTarget(activeTier);
        this.contestants.clear();
        this.winners.clear();
        this.finishTimes.clear();
        this.elapsedSeconds = 0;
        this.twentyMinBuffsAwarded = false;
        this.spectatorManager.clear();
        this.reconnectTracker.clear();

        for (Player p : startingPlayers) {
            contestants.add(p.getUniqueId());
            statsManager.recordParticipation(p.getUniqueId(), p.getName());
        }

        this.activeMatch = worldFactory.claimReadyWorldPair();

        RouletteMenu roulette = new RouletteMenu(plugin, config.getTargets(activeTier), currentTarget, () -> {
            launchMatch(startingPlayers);
        });

        roulette.start(startingPlayers);
    }

    public synchronized void startGame(CommandSender sender, DifficultyTier tier) {
        if (state != GameState.LOBBY) {
            Chat.send(sender, "<red>A match is already running or starting!</red>");
            return;
        }

        this.state = GameState.STARTING;
        this.activeTier = tier != null ? tier : config.getDefaultTier();
        this.currentTarget = targetEngine.pickRandomTarget(activeTier);
        this.contestants.clear();
        this.winners.clear();
        this.finishTimes.clear();
        this.elapsedSeconds = 0;
        this.twentyMinBuffsAwarded = false;
        this.spectatorManager.clear();
        this.reconnectTracker.clear();

        // Determine players: if sender is party leader, include party; otherwise all online players in lobby
        List<Player> startingPlayers = new ArrayList<>();
        if (sender instanceof Player leader && partyManager.isPartyLeader(leader.getUniqueId())) {
            for (UUID memberUuid : partyManager.getPartyMembers(leader.getUniqueId())) {
                Player p = Bukkit.getPlayer(memberUuid);
                if (p != null && p.isOnline()) startingPlayers.add(p);
            }
        } else {
            startingPlayers.addAll(lobbyManager.getLobbyWorld().getPlayers());
        }

        if (startingPlayers.isEmpty()) {
            if (sender instanceof Player p) startingPlayers.add(p);
            else {
                Chat.send(sender, "<red>No players available to start a match.</red>");
                this.state = GameState.LOBBY;
                return;
            }
        }

        for (Player p : startingPlayers) {
            contestants.add(p.getUniqueId());
            statsManager.recordParticipation(p.getUniqueId(), p.getName());
        }

        // Claim pre-warmed world pair from factory
        this.activeMatch = worldFactory.claimReadyWorldPair();

        // Spin Roulette GUI
        RouletteMenu roulette = new RouletteMenu(plugin, config.getTargets(activeTier), currentTarget, () -> {
            launchMatch(startingPlayers);
        });

        roulette.start(startingPlayers);
    }

    private void launchMatch(List<Player> players) {
        Location spawn = activeMatch.getSpawnLocation();
        String targetName = targetEngine.getFriendlyName(currentTarget);

        bossBarController.start(targetName, getTargetWinnerCount());

        for (Player player : players) {
            if (!player.isOnline()) continue;

            player.setGameMode(GameMode.SURVIVAL);
            player.getInventory().clear();
            player.setHealth(20.0);
            player.setFoodLevel(20);
            player.setSaturation(20.0f);
            player.getActivePotionEffects().forEach(e -> player.removePotionEffect(e.getType()));

            giveStarterKit(player);
            player.teleport(spawn);
            bossBarController.addPlayer(player);

            Chat.sendTitle(player,
                    "<gold><bold>" + targetName + "</bold></gold>",
                    "<yellow>Find or craft this item!</yellow>",
                    10, 60, 20);
        }

        this.state = GameState.IN_GAME;

        Chat.broadcast("<gold>[Race]</gold> <yellow>Target: <gold><bold>" + targetName + "</bold></gold> (<white>" + activeTier.name() + "</white>). Good luck!</yellow>");

        startMatchTimer();
    }

    private void giveStarterKit(Player player) {
        if (config.isGiveIronTools()) {
            player.getInventory().addItem(
                    new ItemStack(Material.IRON_SWORD),
                    new ItemStack(Material.IRON_PICKAXE),
                    new ItemStack(Material.IRON_AXE),
                    new ItemStack(Material.IRON_SHOVEL)
            );
        }
        if (config.isGiveSilkPickaxe()) {
            ItemStack silkPick = new ItemStack(Material.GOLDEN_PICKAXE);
            silkPick.addEnchantment(Enchantment.SILK_TOUCH, 1);
            player.getInventory().addItem(silkPick);
        }
    }

    private void startMatchTimer() {
        if (gameTimerTask != null) gameTimerTask.cancel();

        this.gameTimerTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (state != GameState.IN_GAME) {
                    cancel();
                    return;
                }

                elapsedSeconds++;
                int effectiveWinners = getTargetWinnerCount();
                bossBarController.update(targetEngine.getFriendlyName(currentTarget), elapsedSeconds, winners.size(), effectiveWinners);

                // 20-minute buffs: Speed II & Regeneration I (Infinite)
                if (config.isTwentyMinBuffs() && !twentyMinBuffsAwarded && elapsedSeconds >= 1200) {
                    twentyMinBuffsAwarded = true;
                    award20MinBuffs();
                }

                // If match is not started by an admin, stop if 1 or less active players remain for > 1 minute (60s)
                if (!adminStarted) {
                    long activeCount = contestants.stream()
                            .filter(uuid -> !winners.contains(uuid))
                            .map(Bukkit::getPlayer)
                            .filter(p -> p != null && p.isOnline() && activeMatch != null && activeMatch.containsWorld(p.getWorld()))
                            .count();

                    if (activeCount <= 1) {
                        underpopulatedSeconds++;
                        if (underpopulatedSeconds == 30) {
                            Chat.broadcast("<yellow>[Race] Only 1 player left. Match ends in 30s.</yellow>");
                        } else if (underpopulatedSeconds >= 60) {
                            Chat.broadcast("<red>[Race] Match ended: Not enough players.</red>");
                            resetGame();
                            cancel();
                            return;
                        }
                    } else {
                        underpopulatedSeconds = 0;
                    }
                }

                // 3-hour match timeout
                if (elapsedSeconds >= config.getTimeoutHours() * 3600) {
                    Chat.broadcast("<red>[Race] Match reached timeout limit.</red>");
                    resetGame();
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    private void award20MinBuffs() {
        Chat.broadcast("<green>[Race] 20 minutes reached: Speed II & Regeneration I granted.</green>");

        for (UUID uuid : contestants) {
            if (winners.contains(uuid)) continue;
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, PotionEffect.INFINITE_DURATION, 1, false, true, true));
                p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, PotionEffect.INFINITE_DURATION, 0, false, true, true));
                Chat.sendTitle(p, "<green><bold>BUFFS UNLOCKED</bold></green>", "<yellow>Speed II & Regeneration I</yellow>", 10, 60, 20);
                Chat.playSound(p, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.2f);
            }
        }
    }

    public synchronized void onTargetAcquired(Player player) {
        if (state != GameState.IN_GAME || winners.contains(player.getUniqueId())) {
            return;
        }

        winners.add(player.getUniqueId());
        int place = winners.size();
        finishTimes.put(player.getUniqueId(), elapsedSeconds);

        statsManager.recordPodiumFinish(player.getUniqueId(), player.getName(), place, elapsedSeconds);

        int m = elapsedSeconds / 60;
        int s = elapsedSeconds % 60;
        String timeStr = String.format("%02d:%02d", m, s);
        String targetName = targetEngine.getFriendlyName(currentTarget);

        String placeTitle = place == 1 ? "<gold><bold>1st Place</bold></gold>" :
                            place == 2 ? "<yellow><bold>2nd Place</bold></yellow>" :
                                         "<red><bold>3rd Place</bold></red>";

        Chat.broadcast("<gold>[Race]</gold> <yellow><bold>" + player.getName() +
                "</bold> found <gold>" + targetName + "</gold> in <white>" + timeStr + "</white>! (" + placeTitle + ")</yellow>");

        Chat.sendTitle(player, placeTitle, "<white>" + targetName + " in " + timeStr + "</white>", 10, 80, 20);
        spawnVictoryFirework(player.getLocation());

        // Place finisher in spectator mode
        spectatorManager.makeSpectator(player, player.getLocation());

        // Check if podium is complete
        int targetWinners = getTargetWinnerCount();
        if (winners.size() >= targetWinners || winners.size() >= contestants.size()) {
            concludeMatch();
        }
    }

    private void concludeMatch() {
        this.state = GameState.CELEBRATING;
        if (gameTimerTask != null) gameTimerTask.cancel();

        Chat.broadcast("<gold>[Race]</gold> <green><bold>Match Over!</bold></green>");
        for (int i = 0; i < winners.size(); i++) {
            UUID winnerId = winners.get(i);
            Player p = Bukkit.getPlayer(winnerId);
            String name = p != null ? p.getName() : "Unknown";
            int time = finishTimes.getOrDefault(winnerId, elapsedSeconds);
            int m = time / 60;
            int s = time % 60;
            Chat.broadcast("  <yellow>#" + (i + 1) + ":</yellow> <white>" + name + "</white> <gray>(" + String.format("%02d:%02d", m, s) + ")</gray>");
        }
        Chat.broadcast("  <gray>Returning to lobby in " + config.getCelebrationSeconds() + "s...</gray>");

        if (plugin.getLeaderboardManager() != null) {
            plugin.getLeaderboardManager().spawnOrUpdateAll();
        }

        Bukkit.getScheduler().runTaskLater(plugin, this::resetGame, config.getCelebrationSeconds() * 20L);
    }

    public void resetGame() {
        if (gameTimerTask != null) {
            gameTimerTask.cancel();
            gameTimerTask = null;
        }

        this.state = GameState.RESETTING;

        // Evacuate all online contestants and spectators to lobby
        Set<Player> allPlayers = new HashSet<>();
        for (UUID uuid : contestants) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) allPlayers.add(p);
        }
        for (UUID uuid : spectatorManager.getSpectators()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) allPlayers.add(p);
        }
        if (activeMatch != null) {
            if (activeMatch.getOverworld() != null) {
                allPlayers.addAll(activeMatch.getOverworld().getPlayers());
            }
            if (activeMatch.getNether() != null) {
                allPlayers.addAll(activeMatch.getNether().getPlayers());
            }
        }

        bossBarController.hideAll(allPlayers);

        for (Player p : allPlayers) {
            lobbyManager.teleportToLobby(p);
        }

        // Asynchronously purge and delete active match worlds
        if (activeMatch != null) {
            worldFactory.purgeWorldPairAsync(activeMatch, lobbyManager.getLobbyWorld());
            activeMatch = null;
        }

        spectatorManager.clear();
        reconnectTracker.clear();
        contestants.clear();
        winners.clear();
        finishTimes.clear();
        currentTarget = null;
        elapsedSeconds = 0;
        twentyMinBuffsAwarded = false;
        adminStarted = false;
        underpopulatedSeconds = 0;

        this.state = GameState.LOBBY;
        Chat.broadcast("<gold>[FTB]</gold> <green>Ready for next match.</green>");
    }

    public void forceStop(CommandSender sender) {
        if (state == GameState.LOBBY) {
            Chat.send(sender, "<yellow>No match running.</yellow>");
            return;
        }
        Chat.broadcast("<red>[FTB] Match stopped by admin.</red>");
        resetGame();
    }

    public boolean leaveMatch(Player player) {
        if (player == null) return false;
        UUID uuid = player.getUniqueId();

        boolean wasContestant = contestants.remove(uuid);
        boolean wasWinner = winners.remove(uuid);
        boolean wasSpectator = spectatorManager.isSpectator(uuid);

        if (wasSpectator) {
            spectatorManager.restorePlayer(player);
        }

        if (wasContestant || wasWinner || wasSpectator) {
            bossBarController.removePlayer(player);
            lobbyManager.teleportToLobby(player);
            Chat.send(player, "<yellow>You left the match.</yellow>");
            Chat.broadcast("<gray>[Race] " + player.getName() + " left the match.</gray>");
            return true;
        }

        return false;
    }

    public void joinMidGame(Player player) {
        if (state != GameState.IN_GAME || activeMatch == null) {
            Chat.send(player, "<yellow>No match running.</yellow>");
            return;
        }
        if (contestants.contains(player.getUniqueId())) {
            Chat.send(player, "<red>You are already in the race.</red>");
            return;
        }
        if (spectatorManager.isSpectator(player.getUniqueId())) {
            Chat.send(player, "<red>You are already spectating.</red>");
            return;
        }
        if (!config.isAllowLateEntry()) {
            Chat.send(player, "<red>Mid-game joining is disabled.</red>");
            return;
        }

        contestants.add(player.getUniqueId());
        statsManager.recordParticipation(player.getUniqueId(), player.getName());

        player.setGameMode(GameMode.SURVIVAL);
        player.getInventory().clear();
        player.setHealth(20.0);
        player.setFoodLevel(20);
        giveStarterKit(player);
        player.teleport(activeMatch.getSpawnLocation());
        bossBarController.addPlayer(player);

        Chat.broadcast("<yellow>" + player.getName() + " joined the race.</yellow>");
        Chat.sendTitle(player,
                "<gold><bold>" + targetEngine.getFriendlyName(currentTarget) + "</bold></gold>",
                "<yellow>Find or craft this item!</yellow>",
                10, 60, 20);
    }

    public void spectateMidGame(Player player) {
        if (state != GameState.IN_GAME || activeMatch == null) {
            Chat.send(player, "<yellow>No match running.</yellow>");
            return;
        }
        if (contestants.contains(player.getUniqueId())) {
            Chat.send(player, "<red>You are already in the race.</red>");
            return;
        }
        if (spectatorManager.isSpectator(player.getUniqueId())) {
            Chat.send(player, "<red>You are already spectating.</red>");
            return;
        }

        spectatorManager.makeSpectator(player, activeMatch.getSpawnLocation());
        bossBarController.addPlayer(player);
    }

    public int getTargetWinnerCount() {
        int total = contestants.size();
        if (total <= 1) return 1;
        if (total <= config.getMaxWinners()) return total - 1;
        return config.getMaxWinners();
    }

    private void spawnVictoryFirework(Location loc) {
        if (loc == null || loc.getWorld() == null) return;
        Firework fw = loc.getWorld().spawn(loc, Firework.class);
        FireworkMeta meta = fw.getFireworkMeta();
        meta.addEffect(FireworkEffect.builder()
                .with(FireworkEffect.Type.BALL_LARGE)
                .withColor(Color.LIME, Color.YELLOW, Color.WHITE)
                .withFade(Color.AQUA)
                .flicker(true)
                .trail(true)
                .build());
        meta.setPower(1);
        fw.setFireworkMeta(meta);
    }

    public GameState getState() { return state; }
    public boolean isGameRunning() { return state == GameState.IN_GAME; }
    public Material getCurrentTarget() { return currentTarget; }
    public int getElapsedSeconds() { return elapsedSeconds; }
    public Set<UUID> getContestants() { return contestants; }
    public List<UUID> getWinners() { return winners; }
    public MatchWorldPair getActiveMatch() { return activeMatch; }
    public DifficultyTier getActiveTier() { return activeTier; }
    public SpectatorManager getSpectatorManager() { return spectatorManager; }
    public ReconnectTracker getReconnectTracker() { return reconnectTracker; }
}
