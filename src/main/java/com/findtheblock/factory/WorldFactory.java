package com.findtheblock.factory;

import com.findtheblock.FindTheBlock;
import com.findtheblock.config.ConfigManager;
import com.findtheblock.util.LocationUtil;
import org.bukkit.*;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

/**
 * Production Background World Factory Pipeline.
 * Continuously manages a warm queue of unique-seed match world pairs (Overworld + Nether).
 */
public class WorldFactory {

    private final FindTheBlock plugin;
    private final ConfigManager config;
    private final Queue<MatchWorldPair> readyQueue = new ConcurrentLinkedQueue<>();
    private boolean isGenerating = false;

    public WorldFactory(FindTheBlock plugin, ConfigManager config) {
        this.plugin = plugin;
        this.config = config;
    }

    /**
     * Pre-generates the next match world pair in the background without tick drops.
     */
    public synchronized void refillQueueAsync(Consumer<MatchWorldPair> onComplete) {
        if (readyQueue.size() >= config.getBufferSize() || isGenerating) {
            return;
        }

        this.isGenerating = true;
        String matchId = UUID.randomUUID().toString().substring(0, 8);
        long seed = ThreadLocalRandom.current().nextLong();

        plugin.getLogger().info("Factory Pipeline: Pre-generating standby pair 'ftb_" + matchId + "'...");

        // 1. Create Overworld
        WorldCreator overworldCreator = new WorldCreator("ftb_" + matchId);
        overworldCreator.environment(World.Environment.NORMAL);
        overworldCreator.seed(seed);
        World overworld = overworldCreator.createWorld();

        // 2. Create Nether
        WorldCreator netherCreator = new WorldCreator("ftb_" + matchId + "_nether");
        netherCreator.environment(World.Environment.NETHER);
        netherCreator.seed(seed);
        World nether = netherCreator.createWorld();

        if (overworld == null || nether == null) {
            plugin.getLogger().severe("Factory Pipeline: Failed to instantiate world pair!");
            this.isGenerating = false;
            return;
        }

        // Apply World Borders
        double borderSize = config.getMatchBorderSize();
        overworld.getWorldBorder().setSize(borderSize);
        nether.getWorldBorder().setSize(borderSize);

        // Turn off auto-saving during match for high I/O performance
        overworld.setAutoSave(false);
        nether.setAutoSave(false);

        MatchWorldPair pair = new MatchWorldPair(matchId, overworld, nether, seed);

        // Pre-generate spawn chunks smoothly using ChunkPacer
        int chunkRadius = config.getChunkRadius();
        int chunksPerSec = config.getChunksPerSecond();
        Location initialSpawn = overworld.getSpawnLocation();

        ChunkPacer pacer = new ChunkPacer(plugin, overworld,
                initialSpawn.getBlockX() >> 4, initialSpawn.getBlockZ() >> 4,
                chunkRadius, chunksPerSec, finishedPacer -> {

            // Find safe surface spawn once chunks are loaded
            Location safeSpawn = LocationUtil.findSafeSurfaceSpawn(overworld, initialSpawn);
            overworld.setSpawnLocation(safeSpawn);
            pair.setSpawnLocation(safeSpawn);
            pair.setChunksPreloaded(finishedPacer.getTotalChunks());
            pair.setReady(true);

            readyQueue.offer(pair);
            isGenerating = false;

            plugin.getLogger().info("Factory Pipeline: Standby pair 'ftb_" + matchId + "' is READY! (" +
                    finishedPacer.getTotalChunks() + " chunks preloaded)");

            if (onComplete != null) {
                onComplete.accept(pair);
            }
        });

        pacer.start();
    }

    /**
     * Claims the next ready world pair or creates one on-demand.
     */
    public MatchWorldPair claimReadyWorldPair() {
        MatchWorldPair pair = readyQueue.poll();
        if (pair == null) {
            plugin.getLogger().info("Factory Pipeline: Generating on-demand match world pair...");
            pair = createImmediateFallbackPair();
        }
        return pair;
    }

    private MatchWorldPair createImmediateFallbackPair() {
        String matchId = UUID.randomUUID().toString().substring(0, 8);
        long seed = ThreadLocalRandom.current().nextLong();

        World overworld = new WorldCreator("ftb_" + matchId).environment(World.Environment.NORMAL).seed(seed).createWorld();
        World nether = new WorldCreator("ftb_" + matchId + "_nether").environment(World.Environment.NETHER).seed(seed).createWorld();

        MatchWorldPair pair = new MatchWorldPair(matchId, overworld, nether, seed);
        Location spawn = LocationUtil.findSafeSurfaceSpawn(overworld, overworld.getSpawnLocation());
        pair.setSpawnLocation(spawn);
        pair.setReady(true);
        return pair;
    }

    /**
     * Asynchronously unloads and deletes a finished match world pair.
     */
    public void purgeWorldPairAsync(MatchWorldPair pair, World lobbyWorld) {
        if (pair == null) return;

        Location lobbySpawn = lobbyWorld.getSpawnLocation();

        // Evacuate any remaining players
        if (pair.getOverworld() != null) {
            for (Player player : pair.getOverworld().getPlayers()) {
                player.teleport(lobbySpawn);
            }
        }
        if (pair.getNether() != null) {
            for (Player player : pair.getNether().getPlayers()) {
                player.teleport(lobbySpawn);
            }
        }
        if (pair.getEnd() != null) {
            for (Player player : pair.getEnd().getPlayers()) {
                player.teleport(lobbySpawn);
            }
        }

        File overworldFolder = pair.getOverworld() != null ? pair.getOverworld().getWorldFolder() : null;
        File netherFolder = pair.getNether() != null ? pair.getNether().getWorldFolder() : null;
        File endFolder = pair.getEnd() != null ? pair.getEnd().getWorldFolder() : null;

        // Unload worlds on main thread without saving
        if (pair.getOverworld() != null) Bukkit.unloadWorld(pair.getOverworld(), false);
        if (pair.getNether() != null) Bukkit.unloadWorld(pair.getNether(), false);
        if (pair.getEnd() != null) Bukkit.unloadWorld(pair.getEnd(), false);

        // Delete folders off the main server thread
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            deleteWorldFolder(overworldFolder);
            deleteWorldFolder(netherFolder);
            deleteWorldFolder(endFolder);
        });
    }

    private void deleteWorldFolder(File folder) {
        if (folder == null || !folder.exists()) return;

        // Up to 5 retry attempts to handle file locking
        for (int attempt = 1; attempt <= 5; attempt++) {
            try {
                Files.walkFileTree(folder.toPath(), new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        Files.deleteIfExists(file);
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                        Files.deleteIfExists(dir);
                        return FileVisitResult.CONTINUE;
                    }
                });
                plugin.getLogger().info("Factory Pipeline: Deleted world directory " + folder.getName());
                return;
            } catch (Exception e) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ignored) {}
            }
        }
    }

    public boolean hasReadyWorld() { return !readyQueue.isEmpty(); }
    public int getQueueSize() { return readyQueue.size(); }
    public boolean isGenerating() { return isGenerating; }
}
