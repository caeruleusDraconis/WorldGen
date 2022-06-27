// Copyright 2022 - 2022, Caeruleus Draconis and Taiterio
// SPDX-License-Identifier: Apache-2.0

package caeruleusTait.WorldGen.worker;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A slightly altered version of the vanilla ChunkProgressListener to fit our needs.
 */
public class WGChunkProgressListener implements ChunkProgressListener {

    private final Long2ObjectOpenHashMap<ChunkStatus> statuses;
    private final LongSet unloaded;
    private ChunkPos spawnPos;
    private final int fullDiameter;
    private final int radius;
    private final int diameter;

    public WGChunkProgressListener(int _radius, int _chunkTotal) {
        statuses = new Long2ObjectOpenHashMap<>(_chunkTotal);
        unloaded = new LongOpenHashSet(_chunkTotal);
        spawnPos = new ChunkPos(0, 0);
        fullDiameter = _radius * 2 + 1;
        radius = _radius + ChunkStatus.maxDistance();
        diameter = radius * 2 + 1;
    }

    public synchronized boolean isUnloaded(int x, int z) {
        return unloaded.contains(ChunkPos.asLong(x + spawnPos.x - radius, z + spawnPos.z - radius));
    }

    private synchronized boolean isUnloaded(ChunkPos chunkPos) {
        return unloaded.contains(chunkPos.toLong());
    }

    @Override
    public void updateSpawnPos(@NotNull ChunkPos chunkPos) {
        spawnPos = chunkPos;
    }

    @Override
    public synchronized void onStatusChange(ChunkPos chunkPos, @Nullable ChunkStatus chunkStatus) {
        final long posIDX = chunkPos.toLong();
        if (chunkStatus == null) {
            // The chunk got unloaded
            unloaded.add(posIDX);
            return;
        }

        // If we are reloading a chunk, make sure to remove it from the unloaded list
        unloaded.remove(posIDX);
        statuses.put(posIDX, chunkStatus);
    }

    @Override
    public void start() {
        unloaded.clear();
        statuses.clear();
    }

    @Override
    public void stop() {
        // Nothing to do here
    }

    @Nullable
    public synchronized ChunkStatus getStatus(int i, int j) {
        return statuses.get(ChunkPos.asLong(i, j));
    }

    public synchronized LongSet getUnloaded() {
        return new LongOpenHashSet(unloaded);
    }

    public synchronized Long2ObjectMap<ChunkStatus> getStatuses() {
        return new Long2ObjectOpenHashMap<>(statuses);
    }

    public ChunkPos getSpawnPos() {
        return spawnPos;
    }

    public int getRadius() {
        return radius;
    }

    public int getFullDiameter() {
        return fullDiameter;
    }

    public int getDiameter() {
        return diameter;
    }
}
