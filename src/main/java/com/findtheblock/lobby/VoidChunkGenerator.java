package com.findtheblock.lobby;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Random;

/**
 * Pure void chunk generator for the lobby world.
 * Generates zero blocks, allowing for a completely clean void environment.
 */
public class VoidChunkGenerator extends ChunkGenerator {

    @Override
    public boolean canSpawn(@NotNull World world, int x, int z) {
        return true;
    }

    @Override
    public @Nullable Location getFixedSpawnLocation(@NotNull World world, @NotNull Random random) {
        return new Location(world, 0.5, 101.0, 0.5);
    }
}
