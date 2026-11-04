package com.findtheblock;

import com.findtheblock.command.FTBCommand;
import com.findtheblock.command.JoinCommand;
import com.findtheblock.command.LeaveCommand;
import com.findtheblock.command.MenuCommand;
import com.findtheblock.command.PartyCommand;
import com.findtheblock.command.RecipeCommand;
import com.findtheblock.command.SpectateCommand;
import com.findtheblock.command.StartCommand;
import com.findtheblock.command.TopCommand;
import com.findtheblock.config.ConfigManager;
import com.findtheblock.factory.WorldFactory;
import com.findtheblock.game.GameManager;
import com.findtheblock.game.ReconnectTracker;
import com.findtheblock.game.SpectatorManager;
import com.findtheblock.game.TargetEngine;
import com.findtheblock.hook.FTBPlaceholderHook;
import com.findtheblock.leaderboard.LeaderboardManager;
import com.findtheblock.listener.AcquisitionListener;
import com.findtheblock.listener.ChatListener;
import com.findtheblock.listener.ConnectionListener;
import com.findtheblock.listener.PortalListener;
import com.findtheblock.listener.ProtectionListener;
import com.findtheblock.lobby.LobbyManager;
import com.findtheblock.party.PartyManager;
import com.findtheblock.stats.StatsManager;
import org.bstats.bukkit.Metrics;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * FindTheBlock - Production Minigame Plugin.
 * High-performance competitive item rush minigame powered by a continuous Background World Factory Pipeline.
 */
public class FindTheBlock extends JavaPlugin {

    private ConfigManager configManager;
    private LobbyManager lobbyManager;
    private WorldFactory worldFactory;
    private TargetEngine targetEngine;
    private PartyManager partyManager;
    private StatsManager statsManager;
    private SpectatorManager spectatorManager;
    private ReconnectTracker reconnectTracker;
    private GameManager gameManager;
    private LeaderboardManager leaderboardManager;

    @Override
    public void onEnable() {
        // 1. Initialize Configuration
        this.configManager = new ConfigManager(this);

        // 2. Initialize Subsystems & Void Lobby
        this.lobbyManager = new LobbyManager(this, configManager);
        this.worldFactory = new WorldFactory(this, configManager);
        this.targetEngine = new TargetEngine(this, configManager);
        this.partyManager = new PartyManager(this);
        this.statsManager = new StatsManager(this);
        this.leaderboardManager = new LeaderboardManager(this, statsManager);
        this.spectatorManager = new SpectatorManager(this);
        this.reconnectTracker = new ReconnectTracker();

        this.gameManager = new GameManager(this, configManager, worldFactory, lobbyManager,
                targetEngine, partyManager, statsManager, spectatorManager, reconnectTracker);

        // 3. Register Event Listeners
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new AcquisitionListener(gameManager, targetEngine), this);
        pm.registerEvents(new ProtectionListener(this, gameManager, lobbyManager, configManager, partyManager), this);
        pm.registerEvents(new ConnectionListener(gameManager, lobbyManager, configManager, partyManager), this);
        pm.registerEvents(new PortalListener(gameManager), this);
        pm.registerEvents(new ChatListener(gameManager, lobbyManager), this);

        // 4. Register Commands & Tab Completers
        PluginCommand cmd = getCommand("findtheblock");
        if (cmd != null) {
            FTBCommand ftbCmd = new FTBCommand(this, gameManager, configManager, partyManager, statsManager, leaderboardManager);
            cmd.setExecutor(ftbCmd);
            cmd.setTabCompleter(ftbCmd);
        }

        PluginCommand leaveCmd = getCommand("leave");
        if (leaveCmd != null) {
            leaveCmd.setExecutor(new LeaveCommand(gameManager, partyManager));
        }

        PluginCommand partyCmd = getCommand("party");
        if (partyCmd != null) {
            PartyCommand partyExecutor = new PartyCommand(this, partyManager, gameManager, configManager);
            partyCmd.setExecutor(partyExecutor);
            partyCmd.setTabCompleter(partyExecutor);
        }

        PluginCommand joinCmd = getCommand("join");
        if (joinCmd != null) {
            joinCmd.setExecutor(new JoinCommand(gameManager));
        }

        PluginCommand spectateCmd = getCommand("spectate");
        if (spectateCmd != null) {
            spectateCmd.setExecutor(new SpectateCommand(gameManager));
        }

        PluginCommand recipeCmd = getCommand("recipe");
        if (recipeCmd != null) {
            recipeCmd.setExecutor(new RecipeCommand(gameManager));
        }

        PluginCommand menuCmd = getCommand("menu");
        if (menuCmd != null) {
            menuCmd.setExecutor(new MenuCommand(this, gameManager, partyManager));
        }

        PluginCommand topCmd = getCommand("top");
        if (topCmd != null) {
            topCmd.setExecutor(new TopCommand(statsManager));
        }

        PluginCommand startCmd = getCommand("start");
        if (startCmd != null) {
            StartCommand startExecutor = new StartCommand(gameManager, partyManager, configManager);
            startCmd.setExecutor(startExecutor);
            startCmd.setTabCompleter(startExecutor);
        }

        // 5. Register PlaceholderAPI Expansion if available
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new FTBPlaceholderHook(this, gameManager, statsManager).register();
            getLogger().info("Registered PlaceholderAPI expansion for FindTheBlock!");
        }

        // bStats Metrics
        int pluginId = 35123;
        new Metrics(this, pluginId);

        getLogger().info("FindTheBlock 1.0.0 enabled successfully! Dedicated void lobby initialized.");
    }

    @Override
    public void onDisable() {
        if (gameManager != null && gameManager.isGameRunning()) {
            gameManager.resetGame();
        }
        if (statsManager != null) {
            statsManager.save();
        }
        getLogger().info("FindTheBlock disabled.");
    }

    public ConfigManager getConfigManager() { return configManager; }
    public LobbyManager getLobbyManager() { return lobbyManager; }
    public WorldFactory getWorldFactory() { return worldFactory; }
    public TargetEngine getTargetEngine() { return targetEngine; }
    public PartyManager getPartyManager() { return partyManager; }
    public StatsManager getStatsManager() { return statsManager; }
    public SpectatorManager getSpectatorManager() { return spectatorManager; }
    public ReconnectTracker getReconnectTracker() { return reconnectTracker; }
    public LeaderboardManager getLeaderboardManager() { return leaderboardManager; }
    public GameManager getGameManager() { return gameManager; }
}
