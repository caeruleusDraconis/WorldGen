// Copyright 2022 - 2022, Caeruleus Draconis and Taiterio
// SPDX-License-Identifier: Apache-2.0

package caeruleusTait.WorldGen.worker.storage;

import caeruleusTait.WorldGen.mixin.ChunkStorageAccessor;
import caeruleusTait.WorldGen.mixin.IOWorkerAccessor;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import caeruleusTait.WorldGen.WorldGen;
import caeruleusTait.WorldGen.mixin.RegionFileStorageAccessor;
import caeruleusTait.WorldGen.worker.WGLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.storage.RegionFile;
import net.minecraft.world.level.chunk.storage.RegionFileStorage;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

import static java.lang.Thread.MAX_PRIORITY;

public class WGChunkIO {
    public static final int MAX_REGION_FUTURES = 16;
    public static final int FLUSH_EVERY = 256;

    private final WGLevel level;
    private final RegionFileStorage storage;
    private final RegionFileStorageAccessor storageAccessor;

    private final ResourceKey<Level> dimension;
    private final ChunkGenerator chunkGenerator;

    private final Long2ObjectMap<RegionData> regionDataMap;

    private final AtomicLong regionAccessCounter;

    public record RegionData(RegionPos pos,
                             Executor executor,
                             List<CompletableFuture<ChunkPos>> futures,
                             Long2ObjectMap<CompoundTag> toWriteTags,
                             AtomicLong lastFlush,
                             AtomicLong lastAccess) {
    }

    private record CleanupHelper(long accessDiff, long rposIDX, boolean hasNoFutures) {
    }

    public record RegionPos(int rx, int rz) {
        public long toLong() {
            return ChunkPos.asLong(rx, rz);
        }

        public static long asLong(int x, int z) {
            return ChunkPos.asLong(x, z);
        }

        public static RegionPos fromChunkPos(ChunkPos cpos) {
            return new RegionPos(cpos.getRegionX(), cpos.getRegionZ());
        }
    }

    public WGChunkIO(WGLevel _level) {
        level = _level;
        storage = ((IOWorkerAccessor) ((ChunkStorageAccessor) _level.extractChunkMap()).getWorker()).getStorage();
        storageAccessor = (RegionFileStorageAccessor) (Object) storage;

        dimension = level.dimension();
        chunkGenerator = level.chunkGenerator();

        regionDataMap = new Long2ObjectOpenHashMap<>();
        regionAccessCounter = new AtomicLong(0);
    }

    private synchronized RegionData regionDataFor(ChunkPos pos) {
        final RegionPos rpos = RegionPos.fromChunkPos(pos);
        RegionData rdata = regionDataMap.computeIfAbsent(rpos.toLong(), x -> {
            BasicThreadFactory factory = new BasicThreadFactory.Builder().namingPattern("WG-IO-r" + rpos.rx + "." + rpos.rz)
                    .priority(MAX_PRIORITY)
                    .build();

            return new RegionData(
                    rpos,
                    Executors.newSingleThreadExecutor(factory),
                    new ArrayList<>(),
                    new Long2ObjectOpenHashMap<>(),
                    new AtomicLong(0),
                    new AtomicLong(0)
            );
        });

        rdata.lastAccess.set(regionAccessCounter.incrementAndGet());

        return rdata;
    }

    public void write(ChunkPos pos, CompoundTag tag) {
        final RegionData rData = regionDataFor(pos);
        CompletableFuture<ChunkPos> future;
        synchronized (rData) {
            future = CompletableFuture.supplyAsync(() -> {
                try {
                    storageAccessor.callWrite(pos, tag);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                synchronized (rData) {
                    rData.toWriteTags.remove(pos.toLong());
                }

                // Flush
                if (rData.lastFlush.incrementAndGet() >= FLUSH_EVERY) {
                    RegionFile rf = storageAccessor.getRegionCache().get(rData.pos.toLong());
                    if (rf == null) {
                        WorldGen.LOGGER.warn("Unable to flush region file for [{}, {}]", rData.pos.rx, rData.pos.rz);
                    } else {
                        try {
                            rf.flush();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    rData.lastFlush.set(0);
                }

                // Do not do this because it significantly decreases save performance
                // ((SynchronizedPoiManager) level.getPoiManager()).flushAndRemove(pos);

                return pos;
            }, rData.executor);

            rData.futures.add(future);
            if (rData.futures.size() > MAX_REGION_FUTURES) {
                future = rData.futures.get(0);
            } else {
                future = null;
            }
        }

        if (future != null) {
            try {
                future.get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public CompoundTag read(ChunkPos pos) {
        final RegionData rData = regionDataFor(pos);
        CompoundTag tag;
        synchronized (rData) {
            tag = rData.toWriteTags.get(pos.toLong());
        }
        if (tag != null) {
            return tag;
        }
        try {
            tag = storage.read(pos);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (tag == null) {
            return null;
        }
        return level.extractChunkMap().upgradeChunkTag(dimension, level::getDataStorage, tag, chunkGenerator.getTypeNameForDataFixer());
    }

    public synchronized void close() {
        regionDataMap.values().stream().flatMap(x -> x.futures.stream()).forEach(x -> {
            try {
                x.get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        });

        flush();
        regionDataMap.clear();
    }

    public void flush() {
        List<CompletableFuture<Void>> futures = regionDataMap.long2ObjectEntrySet().stream().map(x -> {
            RegionData data = x.getValue();
            synchronized (data) {
                RegionFile rf = storageAccessor.getRegionCache().get(x.getLongKey());
                if (rf == null) {
                    return CompletableFuture.completedFuture((Void) null);
                }
                return CompletableFuture.runAsync(() -> {
                    try {
                        rf.flush();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
            }
        }).toList();

        futures.forEach(x -> {
            try {
                x.get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        });
    }

}
