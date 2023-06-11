// Copyright 2022 - 2022, Caeruleus Draconis and Taiterio
// SPDX-License-Identifier: Apache-2.0

package caeruleusTait.WorldGen.worker.storage;

import caeruleusTait.WorldGen.util.SimpleClosable;
import caeruleusTait.WorldGen.worker.WGInlineLeveLightEngine;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.lighting.LevelLightEngine;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class WGChunkHolder {
    private final static AtomicLong accessCounter = new AtomicLong(0);

    private ChunkAccess chunkAccess;
    private final AtomicInteger lockCount;
    private final AtomicLong lastAccess;

    private WGLightEngineData lightData;
    private final LongSet lightLoadedIn;

    public WGChunkHolder(ChunkAccess _chunkAccess) {
        chunkAccess = _chunkAccess;
        lockCount = new AtomicInteger(0);
        lastAccess = new AtomicLong(accessCounter.incrementAndGet());
        lightLoadedIn = new LongOpenHashSet();
    }

    public void lock() {
        lockCount.incrementAndGet();
    }

    public void unlock() {
        lockCount.decrementAndGet();
    }

    public static long maxAccessCounter() {
        return accessCounter.get();
    }

    public ChunkStatus getStatus() {
        return chunkAccess.getStatus();
    }

    public ChunkStatus update(ChunkAccess _chunkAccess) {
        final ChunkStatus oldStatus = chunkAccess.getStatus();
        if (_chunkAccess.getStatus().isOrAfter(chunkAccess.getStatus())) {
            chunkAccess = _chunkAccess;
        }
        return oldStatus;
    }

    public ChunkAccess chunkAccess() {
        if (!isLocked()) {
            throw new RuntimeException("ChunkHolder not locked! " + chunkAccess.getPos());
        }
        lastAccess.set(accessCounter.incrementAndGet());
        return chunkAccess;
    }

    public synchronized void storeLightData(LevelLightEngine engine) {
        lightData = new WGLightEngineData(engine, chunkAccess.getPos());
        lightLoadedIn.add(Thread.currentThread().getId());
    }

    public synchronized void ensureLightDataLoaded(WGInlineLeveLightEngine engine) {
        if (chunkAccess.getStatus().equals(ChunkStatus.INITIALIZE_LIGHT)) {
            engine.initializeLight(chunkAccess);
        }
        if (!chunkAccess.getStatus().isOrAfter(ChunkStatus.LIGHT)) {
            return;
        }
        if (lightLoadedIn.add(Thread.currentThread().getId())) {
            if (lightData != null) {
                lightData.loadLightEngineData(engine);
            }
        }
    }

    @Nullable
    public synchronized Long getValidLightThreadIndex() {
        if (lightData == null) {
            return null;
        }
        return lightLoadedIn.iterator().nextLong();
    }

    public synchronized long[] getValidLightThreadIndexes() {
        return lightLoadedIn.toLongArray();
    }

    public boolean isLocked() {
        return lockCount.get() > 0;
    }

    public long accessDiff() {
        return accessCounter.get() - lastAccess.get();
    }

    public long lastAccess() {
        return lastAccess.get();
    }

    public ChunkPos getPos() {
        return chunkAccess.getPos();
    }

    public boolean isUnsaved() {
        return chunkAccess.isUnsaved();
    }

    public WGChunkLock genLock() {
        return new WGChunkLock();
    }

    public class WGChunkLock implements SimpleClosable {
        public WGChunkLock() {
            lock();
        }

        @Override
        public void close() {
            unlock();
        }

        public ChunkAccess chunkAccess() {
            lastAccess.set(accessCounter.incrementAndGet());
            return chunkAccess;
        }
    }
}
