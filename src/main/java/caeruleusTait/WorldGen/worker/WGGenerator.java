// Copyright 2022 - 2022, Caeruleus Draconis and Taiterio
// SPDX-License-Identifier: Apache-2.0

package caeruleusTait.WorldGen.worker;

import caeruleusTait.WorldGen.mixin.ChunkMapAccessor;
import caeruleusTait.WorldGen.util.SimpleClosable;
import caeruleusTait.WorldGen.worker.storage.WGChunkHolder;
import caeruleusTait.WorldGen.worker.storage.WGChunkIO;
import caeruleusTait.WorldGen.worker.storage.WGChunkStorage;
import com.mojang.datafixers.util.Either;
import it.unimi.dsi.fastutil.longs.*;
import caeruleusTait.WorldGen.WorldGen;
import caeruleusTait.WorldGen.config.WGConfigState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ThreadedLevelLightEngine;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.chunk.storage.ChunkSerializer;
import net.minecraft.world.level.levelgen.structure.StructureStart;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static net.minecraft.core.Registry.BIOME_REGISTRY;

public class WGGenerator {

    final private WGLevel level;
    final private WGMain main;

    final private ChunkMap chunkMap;
    final private Thread cleanup;
    private boolean isRunning;

    final private ChunkMapAccessor chunkMapAccessor;
    final private ThreadedLevelLightEngine threadedLightEngine;
    final public WGChunkStorage chunkStorage;
    final private WGChunkIO chunkIO;
    final public int maxThreads;
    final public int chunksToKeep;
    final private ChunkProgressListener chunkProgressListener;

    final private Object IOLock = new Object();
    final private WGConfigState cfg;

    final private LongSet alreadyGenerated;

    public WGGenerator(WGLevel _level, int maxPossibleThreads) {
        level = _level;
        main = level.main();
        cfg = WGConfigState.get();

        maxThreads = Math.min(cfg.enableThreads ? cfg.maxThreads : 1, maxPossibleThreads);
        chunksToKeep = maxThreads * 650;

        chunkStorage = new WGChunkStorage(chunksToKeep * 2);

        try (SimpleClosable c = level.genServerChunkCacheWhitelist()) {
            threadedLightEngine = level.getChunkSource().getLightEngine();
            chunkMap = level.extractChunkMap();
        }
        chunkMapAccessor = (ChunkMapAccessor) chunkMap;
        chunkProgressListener = main.minecraftServer().chunkProgressListener();

        isRunning = true;
        cleanup = new Thread(this::cleanupWorker);
        cleanup.setName("WG-clean");
        cleanup.start();
        alreadyGenerated = LongSets.synchronize(new LongOpenHashSet());
        chunkIO = new WGChunkIO(level);
    }

    private record CleanupHelper(long accessDiff, long posIDX, boolean isLocked, boolean unsaved) {
    }

    private void cleanupWorker() {
        int savedLast = -1;
        while (isRunning) {
            if (savedLast < 64) {
                try {
                    Thread.sleep(250);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

            final Long2ObjectMap<WGChunkHolder> allHolders = allHolders();

            if (allHolders.size() <= chunksToKeep) {
                savedLast = 0;
                continue;
            }

            List<CleanupHelper> helpers = allHolders().long2ObjectEntrySet()
                    .stream()
                    .map(x -> new CleanupHelper(
                            x.getValue().accessDiff(),
                            x.getLongKey(),
                            x.getValue().isLocked(),
                            x.getValue().isUnsaved()
                    ))
                    .sorted(Comparator.comparingLong(CleanupHelper::accessDiff).reversed())
                    .limit(allHolders.size() - chunksToKeep)
                    .filter(x -> !x.isLocked())
                    .toList();

            // First unload chunks that don't need saving
            List<Long> posList = helpers.stream().filter(x -> !x.unsaved).map(CleanupHelper::posIDX).toList();
            posList.forEach(this::unload);

            // Next save chunks with changes
            posList = helpers.stream().filter(x -> x.unsaved).map(CleanupHelper::posIDX).toList();
            posList = new ArrayList<>(posList);
            Collections.shuffle(posList);
            posList.forEach(this::unload);

            // Remember how many we have saved.
            savedLast = helpers.size();
        }
    }

    private WGChunkHolder ensureLightStored(WGChunkHolder chunkHolder) {
        if (chunkHolder.getStatus().isOrAfter(ChunkStatus.LIGHT)) {
            chunkHolder.storeLightData(level.getLightEngine());
        }
        return chunkHolder;
    }

    public WGChunkHolder loadOrGen(ChunkPos chunkPos, ChunkStatus chunkStatus, boolean lock) {
        long posIDX = chunkPos.toLong();

        WGChunkStorage.MaybeExistingChunk maybeExistingFuture = chunkStorage.getMaxChunkHolder(posIDX, chunkStatus, lock);

        // Avoid out-of-memory scenarios by delaying loading / generating new chunks
        while (chunkStorage.numLoaded() > (chunksToKeep * 2.5)) {
            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        // We may have an existing chunk at or below the required level
        WGChunkHolder chunkHolder;
        if (maybeExistingFuture.exactMatch()) {
            chunkHolder = maybeExistingFuture.existing();
            chunkHolder.ensureLightDataLoaded(level.getLightEngine());
            return chunkHolder;
        } else if (maybeExistingFuture.existing() != null) {
            chunkHolder = maybeExistingFuture.existing();
        } else {
            // We need to load or generate one
            ChunkAccess chunkAccess = load(chunkPos);
            chunkHolder = chunkStorage.markChunkLoaded(chunkAccess);
            if (lock) {
                chunkHolder.lock();
            }
            chunkProgressListener.onStatusChange(chunkPos, chunkAccess.getStatus());
        }

        // Do we have the desired state?
        ChunkAccess chunkAccess;
        if (chunkHolder.getStatus().isOrAfter(chunkStatus)) {
            if (chunkHolder.getStatus().isOrAfter(ChunkStatus.LIGHT)) {
                chunkHolder.storeLightData(level.getLightEngine());
            }
            return chunkHolder;
        } else {
            // No? Then generate until we have it!
            try (WGChunkHolder.WGChunkLock c = chunkHolder.genLock()) {
                chunkAccess = c.chunkAccess();
                chunkAccess = generate(chunkAccess, chunkStatus);
            }
        }

        if (cfg.enableWorldBreakingOptions && cfg.fakeStatus != null) {
            if (chunkAccess.getStatus().isOrAfter(cfg.maxStatus)) {
                if (cfg.fakeStatus == ChunkStatus.FULL) {
                    try {
                        chunkAccess = wgProtoChunkToLevelChunk(chunkAccess).get().left().get();
                    } catch (InterruptedException | ExecutionException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    if (chunkAccess instanceof ProtoChunk protoChunk) {
                        protoChunk.setStatus(cfg.fakeStatus);
                    }
                }
            }
        }

        // We have loaded (or generated) a matching chunk!
        chunkProgressListener.onStatusChange(chunkPos, chunkAccess.getStatus());
        chunkHolder = chunkStorage.markChunkLoaded(chunkAccess);
        if (chunkAccess.getStatus().isOrAfter(ChunkStatus.LIGHT)) {
            chunkHolder.storeLightData(level.getLightEngine());
        }
        return chunkHolder;
    }

    public int unloadedChunks() {
        synchronized (chunkStorage) {
            return chunkStorage.chunkAccessMap.size();
        }
    }

    private Long2ObjectMap<WGChunkHolder> allHolders() {
        Long2ObjectMap<WGChunkHolder> holders;

        synchronized (chunkStorage) {
            holders = new Long2ObjectOpenHashMap<>(chunkStorage.chunkAccessMap);
        }

        return holders;
    }

    private void unloadAll() {
        List<Long> longList = allHolders()
                .long2ObjectEntrySet()
                .stream()
                .map(Long2ObjectMap.Entry::getLongKey)
                .toList();

        longList = new ArrayList<>(longList);
        Collections.shuffle(longList);
        longList.forEach(this::unload);

        //chunkMap.flushWorker();
    }

    private void unload(long posIDX) {
        WGChunkHolder chunkHolder;
        synchronized (chunkStorage) {
            chunkHolder = chunkStorage.chunkAccessMap.remove(posIDX);
            if (chunkHolder == null) {
                return;
            }

            if (chunkHolder.isLocked()) {
                chunkStorage.chunkAccessMap.put(posIDX, chunkHolder);
                return;
            }

            for (WGChunkStorage.StatusStorage storage : chunkStorage.storageByChunkStatus) {
                storage.isValid.remove(posIDX);
            }
        }

        try (WGChunkHolder.WGChunkLock lock = chunkHolder.genLock()) {
            // Save the chunk to disk
            save(lock.chunkAccess(), chunkHolder);

            // Light data is stored separately --> It must be unloaded separately AFTER the chunk is saved
            List<WGInlineLeveLightEngine> engines = level.getAllLightEnginesFor(chunkHolder);
            engines.forEach(x -> x.unloadChunk(chunkHolder.getPos()));
        }

        alreadyGenerated.add(posIDX);
        chunkProgressListener.onStatusChange(new ChunkPos(posIDX), null);
    }

    public ChunkAccess generate(ChunkAccess chunkAccess, ChunkStatus chunkStatus) {
        if (chunkStatus.equals(ChunkStatus.EMPTY)) {
            // This should never happen, but why not be safe?
            return chunkAccess;
        }

        // Now ensure that all the previous steps ran on the required chunk range (recursion!!!)
        List<WGChunkHolder> chunkSquare = getChunkSquare(chunkAccess.getPos(), chunkStatus);

        try {
            if (chunkStatus.equals(ChunkStatus.LIGHT)) {
                // Custom logic because we don't want to use the ThreadedLevelLightEngine
                chunkAccess.setLightCorrect(false);

                if (!chunkAccess.getStatus().isOrAfter(ChunkStatus.LIGHT)) {
                    ((ProtoChunk) chunkAccess).setStatus(ChunkStatus.LIGHT);
                }

                final boolean bl = chunkAccess.getStatus().isOrAfter(ChunkStatus.LIGHT) && chunkAccess.isLightCorrect();
                chunkAccess = level.getLightEngine().lightChunk(chunkAccess, bl);

            } else {
                chunkAccess = chunkStatus.generate(
                        Runnable::run,
                        level,
                        chunkMapAccessor.getGenerator(),
                        chunkMapAccessor.getStructureManager(),
                        threadedLightEngine,
                        this::wgProtoChunkToLevelChunk,
                        chunkSquare.stream().map(WGChunkHolder::chunkAccess).toList(),
                        false
                ).get().left().get();
            }

        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        } finally {
            synchronized (chunkStorage) {
                chunkSquare.forEach(WGChunkHolder::unlock);
            }
        }
        return chunkAccess;
    }

    private List<WGChunkHolder> getChunkSquare(ChunkPos chunkPos, ChunkStatus status) {
        final List<WGChunkHolder> res = new ArrayList<>();
        final int radius = status.getRange();
        final int origX = chunkPos.x;
        final int origZ = chunkPos.z;

        for (int z = -radius; z <= radius; ++z) {
            for (int x = -radius; x <= radius; ++x) {
                final int distance = Math.max(Math.abs(x), Math.abs(z));
                final ChunkPos currPos = new ChunkPos(origX + x, origZ + z);
                ChunkStatus newStatus = distance == 0 ? status.getParent() : ChunkStatus.getStatusAroundFullChunk(
                        ChunkStatus.getDistance(status) + distance);
                WGChunkHolder ch = loadOrGen(currPos, newStatus, true);
                res.add(ch);
                if (!ch.getStatus().isOrAfter(newStatus)) {
                    WorldGen.LOGGER.error(
                            "Invalid loaded status of {}: {} ({} required)",
                            ch.getPos(),
                            ch.getStatus(),
                            newStatus
                    );
                    throw new RuntimeException("Invalid loaded chunk");
                }
            }
        }

        return res;
    }

    private CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> wgProtoChunkToLevelChunk(ChunkAccess chunkAccess) {
        final ProtoChunk protoChunk = (ProtoChunk) chunkAccess;
        final LevelChunk levelChunk;
        boolean shouldSafeEntities = false;
        if (protoChunk instanceof ImposterProtoChunk) {
            levelChunk = ((ImposterProtoChunk) protoChunk).getWrapped();
        } else {
            // Besides creating the final chunk, this also has logic for
            // preparing to store the entities.
            final List<CompoundTag> wgEntities = protoChunk.getEntities();
            shouldSafeEntities = !wgEntities.isEmpty();
            levelChunk = new LevelChunk(level, protoChunk, x -> {
                if (!wgEntities.isEmpty()) {
                    level.addWorldGenChunkEntities(EntityType.loadEntitiesRecursive(wgEntities, level));
                }
            });
        }

        levelChunk.setFullStatus(() -> ChunkHolder.FullChunkStatus.TICKING);
        levelChunk.runPostLoad();
        levelChunk.setLoaded(true);

        // Entities are stored separately and must ONLY be saved when the corresponding
        // generation stage us run!
        if (shouldSafeEntities) {
            synchronized (IOLock) {
                level.safeEntitiesInChunk(chunkAccess.getPos().toLong());
            }
        }

        return CompletableFuture.completedFuture(Either.left(levelChunk));
    }

    public void close() {
        isRunning = false;
        try {
            cleanup.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        unloadAll();
        chunkIO.flush();
        chunkIO.close();
    }


    //  ========
    // === IO ===
    //  ========

    private void save(ChunkAccess chunkAccess, WGChunkHolder chunkHolder) {
        // level.getPoiManager().flush(chunkAccess.getPos());
        if (!chunkAccess.isUnsaved()) {
            return;
        }
        chunkAccess.setUnsaved(false);
        final ChunkPos chunkPos = chunkAccess.getPos();
        final ChunkStatus chunkStatus = chunkAccess.getStatus();
        if (chunkStatus.getChunkType() != ChunkStatus.ChunkType.LEVELCHUNK) {
            if (chunkStatus == ChunkStatus.EMPTY && chunkAccess.getAllStarts().values().stream().noneMatch(
                    StructureStart::isValid)) {
                return;
            }
        }
        // For light engine "multithreading" store the current chunk pos
        // for the ChunkSerializerMixin. NOTE: There is no synchronization
        // required on the light engine because the methods called from
        // ChunkSerializer.write only access already synchronized fields.
        // TODO update: Ensure this also is the case in future versions!
        WorldGen.writeLightEngine.set(level.getLightEngineFor(chunkHolder));

        final CompoundTag compoundTag;
        try (SimpleClosable c = level.genServerChunkCacheWhitelist()) {
            compoundTag = ChunkSerializer.write(level, chunkAccess);
        }
        chunkIO.write(chunkPos, compoundTag);
    }

    private ChunkAccess load(ChunkPos chunkPos) {
        if (cfg.enableWorldBreakingOptions && cfg.overrideExistingChunks && !alreadyGenerated.contains(chunkPos.toLong())) {
            return new ProtoChunk(
                    chunkPos,
                    UpgradeData.EMPTY,
                    level,
                    level.registryAccess().registryOrThrow(BIOME_REGISTRY),
                    null
            );
        }

        final CompoundTag compoundTag = chunkIO.read(chunkPos);
        if (compoundTag == null) {
            return new ProtoChunk(
                    chunkPos,
                    UpgradeData.EMPTY,
                    level,
                    level.registryAccess().registryOrThrow(BIOME_REGISTRY),
                    null
            );
        }

        try (SimpleClosable c = level.genServerChunkCacheWhitelist()) {
            return ChunkSerializer.read(level, level.getPoiManager(), chunkPos, compoundTag);
        }
    }

}
