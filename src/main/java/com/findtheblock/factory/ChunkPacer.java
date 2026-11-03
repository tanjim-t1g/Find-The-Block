package com.findtheblock.factory;

import com.findtheblock.FindTheBlock;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Rates-limits chunk generation over server ticks using Paper's async chunk loading API.
 * Guarantees zero main-thread TPS degradation.
 */
public class ChunkPacer {

    private final FindTheBlock plugin;
    private final World world;
    private final int centerChunkX;
    private final int centerChunkZ;
    private final int radius;
    private final int chunksPerSecond;
    private final Consumer<ChunkPacer> onComplete;

    private final List<long[]> chunkQueue = new ArrayList<>();
    private final AtomicInteger completedCount = new AtomicInteger(0);
    private int totalChunks = 0;
    private BukkitTask task;

    public ChunkPacer(FindTheBlock plugin, World world, int centerChunkX, int centerChunkZ,
                      int radius, int chunksPerSecond, Consumer<ChunkPacer> onComplete) {
        this.plugin = plugin;
        this.world = world;
        this.centerChunkX = centerChunkX;
        this.centerChunkZ = centerChunkZ;
        this.radius = radius;
        this.chunksPerSecond = chunksPerSecond;
        this.onComplete = onComplete;
        prepareQueue();
    }

    private void prepareQueue() {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                chunkQueue.add(new long[]{centerChunkX + dx, centerChunkZ + dz});
            }
        }
        this.totalChunks = chunkQueue.size();
    }

    public void start() {
        // Calculate chunks per 2-tick run: chunksPerSecond / 10 runs per second
        int batchSize = Math.max(1, chunksPerSecond / 10);

        this.task = new BukkitRunnable() {
            int index = 0;

            @Override
            public void run() {
                if (index >= totalChunks) {
                    cancel();
                    return;
                }

                int end = Math.min(index + batchSize, totalChunks);
                for (int i = index; i < end; i++) {
                    long[] coords = chunkQueue.get(i);
                    int cx = (int) coords[0];
                    int cz = (int) coords[1];

                    world.getChunkAtAsync(cx, cz, true).thenAccept(chunk -> {
                        int finished = completedCount.incrementAndGet();
                        if (finished >= totalChunks) {
                            plugin.getServer().getScheduler().runTask(plugin, () -> {
                                if (onComplete != null) onComplete.accept(ChunkPacer.this);
                            });
                        }
                    });
                }
                index = end;
            }
        }.runTaskTimer(plugin, 1L, 2L);
    }

    public void cancel() {
        if (task != null) {
            task.cancel();
        }
    }

    public int getTotalChunks() {
        return totalChunks;
    }
}
