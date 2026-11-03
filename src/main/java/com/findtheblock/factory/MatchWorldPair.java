package com.findtheblock.factory;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;

/**
 * Represents the match worlds (Overworld + Nether, and on-demand End) linked for a single match instance.
 */
public class MatchWorldPair {

    private final String matchId;
    private final World overworld;
    private final World nether;
    private final long seed;
    private World end = null;
    private Location spawnLocation;
    private int chunksPreloaded;
    private boolean ready = false;

    public MatchWorldPair(String matchId, World overworld, World nether, long seed) {
        this.matchId = matchId;
        this.overworld = overworld;
        this.nether = nether;
        this.seed = seed;
    }

    public String getMatchId() { return matchId; }
    public World getOverworld() { return overworld; }
    public World getNether() { return nether; }
    public World getEnd() { return end; }
    public long getSeed() { return seed; }

    public boolean containsWorld(World world) {
        if (world == null) return false;
        if (world.equals(overworld) || world.equals(nether)) return true;
        return end != null && world.equals(end);
    }

    /**
     * Lazily creates the End dimension on-demand only if a player enters an End portal.
     */
    public synchronized World getOrCreateEnd() {
        if (end == null) {
            WorldCreator endCreator = new WorldCreator("ftb_" + matchId + "_the_end");
            endCreator.environment(World.Environment.THE_END);
            endCreator.seed(seed);
            end = endCreator.createWorld();
            if (end != null) {
                end.setAutoSave(false);
                end.getWorldBorder().setSize(overworld.getWorldBorder().getSize());
            }
        }
        return end;
    }

    public Location getSpawnLocation() { return spawnLocation; }
    public void setSpawnLocation(Location spawnLocation) { this.spawnLocation = spawnLocation; }

    public int getChunksPreloaded() { return chunksPreloaded; }
    public void setChunksPreloaded(int chunksPreloaded) { this.chunksPreloaded = chunksPreloaded; }

    public boolean isReady() { return ready; }
    public void setReady(boolean ready) { this.ready = ready; }
}
