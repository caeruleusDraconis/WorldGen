// Copyright 2022 - 2022, Caeruleus Draconis and Taiterio
// SPDX-License-Identifier: Apache-2.0

package caeruleusTait.WorldGen.worker.storage;

import it.unimi.dsi.fastutil.longs.*;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;

import java.util.Arrays;

public class WGChunkStorage {
    public final Long2ObjectMap<WGChunkHolder> chunkAccessMap;
    public final StatusStorage[] storageByChunkStatus;

    public class StatusStorage {
        public final ChunkStatus chunkStatus;
        public final LongSet isValid;

        public StatusStorage(ChunkStatus _chunkStatus, int expected) {
            chunkStatus = _chunkStatus;
            isValid = new LongOpenHashSet(expected);
        }

        public WGChunkHolder get(long posIDX) {
            synchronized (WGChunkStorage.this) {
                if (isValid.contains(posIDX)) {
                    return chunkAccessMap.get(posIDX);
                }
                return null;
            }
        }
    }

    public WGChunkStorage(int expected) {
        chunkAccessMap = new Long2ObjectOpenHashMap<>(expected);
        storageByChunkStatus = new StatusStorage[ChunkStatus.FULL.getIndex() + 1];
        ChunkStatus curr = ChunkStatus.FULL;
        storageByChunkStatus[curr.getIndex()] = new StatusStorage(curr, expected);
        while (curr != curr.getParent()) {
            curr = curr.getParent();
            storageByChunkStatus[curr.getIndex()] = new StatusStorage(curr, expected);
        }
    }

    public record MaybeExistingChunk(WGChunkHolder existing, boolean exactMatch) {
    }

    public synchronized MaybeExistingChunk getMaxChunkHolder(long posIDX, ChunkStatus expected, boolean locked) {
        WGChunkHolder existingChunk;

        // Check for an exact match
        StatusStorage exact = storageByChunkStatus[expected.getIndex()];
        existingChunk = exact.get(posIDX);
        if (existingChunk != null) {
            if (locked) {
                existingChunk.lock();
            }
            return new MaybeExistingChunk(existingChunk, true);
        }

        // Check for previous chunks
        for(int i = expected.getIndex() - 1; i >= 0 && existingChunk == null; --i) {
            existingChunk = storageByChunkStatus[i].get(posIDX);
        }

        if (locked && existingChunk != null) {
            existingChunk.lock();
        }
        return new MaybeExistingChunk(existingChunk, false);
    }

    public synchronized ChunkAccess getExistingChunk(long posIDX, ChunkStatus status) {
        StatusStorage curr = storageByChunkStatus[status.getIndex()];
        if (!curr.isValid.contains(posIDX)) {
            return null;
        }

        final WGChunkHolder holder = chunkAccessMap.get(posIDX);
        try (WGChunkHolder.WGChunkLock lock = holder.genLock()) {
            return lock.chunkAccess();
        }
    }

    public synchronized WGChunkHolder markChunkLoaded(ChunkAccess chunkAccess) {
        final long posIDX = chunkAccess.getPos().toLong();
        WGChunkHolder chunkHolder = chunkAccessMap.get(posIDX);
        if (chunkHolder == null) {
            chunkHolder = new WGChunkHolder(chunkAccess);
            chunkAccessMap.put(posIDX, chunkHolder);
        } else {
            chunkHolder.update(chunkAccess);
        }

        final StatusStorage[] workArray = Arrays.copyOfRange(
                storageByChunkStatus,
                0,
                chunkAccess.getStatus().getIndex() + 1
        );
        for (StatusStorage i : workArray) {
            i.isValid.add(posIDX);
        }

        return chunkHolder;
    }

    public synchronized int numLoaded() {
        return chunkAccessMap.size();
    }
}
