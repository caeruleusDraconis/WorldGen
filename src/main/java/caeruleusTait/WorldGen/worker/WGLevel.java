// Copyright 2022 - 2022, Caeruleus Draconis and Taiterio
// SPDX-License-Identifier: Apache-2.0

package caeruleusTait.WorldGen.worker;

import caeruleusTait.WorldGen.WorldGen;
import caeruleusTait.WorldGen.adapters.SynchronizedPoiManager;
import caeruleusTait.WorldGen.mixin.PersistentEntitySectionManagerAccessor;
import caeruleusTait.WorldGen.mixin.ServerLevelMixinAccessor;
import caeruleusTait.WorldGen.util.SimpleClosable;
import caeruleusTait.WorldGen.util.Utils;
import caeruleusTait.WorldGen.worker.storage.WGChunkHolder;
import caeruleusTait.WorldGen.worker.storage.WGLightChunkGetter;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.RegistryLayer;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.RandomSequences;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.CustomSpawner;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraft.world.level.storage.DerivedLevelData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.minecraft.world.level.storage.ServerLevelData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public class WGLevel extends ServerLevel {
    final private static boolean isDebug = false;
    final private static boolean tickTime = false;
    final private static List<CustomSpawner> customSpawners = new ArrayList<>();

    final private WGMain main;
    final private WGGenerator generator;
    final private Long2ObjectMap<WGInlineLeveLightEngine> levelLightEngines;

    final private DimensionDataStorage dimensionDataStorage;
    final private PoiManager poiManager;
    final private ChunkGenerator chunkGenerator;
    final private ServerLevelMixinAccessor serverLevelMixinAccessor;
    final private PersistentEntitySectionManager<Entity> extractedEntityManager;
    final private RandomState randomState;

    WGInlineLeveLightEngine tmpEngine;

    private AtomicInteger allowServerChunkCacheAccess = new AtomicInteger(0);

    public WGLevel(
            WGMain main,
            ResourceKey<Level> dimension,
            int maxPossibleThreads
    ) {
        super(
                main.minecraftServer(),
                main.minecraftServer().executor(),
                main.levelStorageAccess(),
                genServerLevelData(main, dimension),
                dimension,
                genLevelStem(main, dimension),
                main.minecraftServer().chunkProgressListener(),
                isDebug,
                BiomeManager.obfuscateSeed(main.worldData().worldGenOptions().seed()), // MinecraftServer.createLevels
                customSpawners,
                tickTime,
                new RandomSequences(main.worldData().worldGenOptions().seed())
        );

        final ServerChunkCache serverChunkCache = super.getChunkSource();
        chunkGenerator = serverChunkCache.getGenerator();
        poiManager = new SynchronizedPoiManager(serverChunkCache.getPoiManager(), main.levelStorageAccess().getDimensionPath(dimension));
        dimensionDataStorage = serverChunkCache.getDataStorage();
        levelLightEngines = Long2ObjectMaps.synchronize(new Long2ObjectOpenHashMap<>());
        tmpEngine = new WGInlineLeveLightEngine(
                new WGLightChunkGetter(this),
                true,
                dimensionType().hasSkyLight()
        );

        WorldGen.currentLevel = this;

        this.main = main;
        this.generator = new WGGenerator(this, maxPossibleThreads);

        this.serverLevelMixinAccessor = (ServerLevelMixinAccessor) this;
        this.extractedEntityManager = serverLevelMixinAccessor.getEntityManager();
        this.randomState = serverChunkCache.randomState();
    }

    public static Holder<DimensionType> dimensionTypeHolder(WGMain _wgMain, ResourceKey<Level> _dimension) {
        ResourceKey<LevelStem> dimensionStemKey = Utils.levelToLevelStem(_dimension);
        LevelStem dimensionStem = _wgMain.worldStem()
                .registries()
                .getLayer(RegistryLayer.DIMENSIONS)
                .registryOrThrow(Registries.LEVEL_STEM)
                .get(dimensionStemKey);
        return dimensionStem.type();
    }

    public static ServerLevelData genServerLevelData(WGMain main, ResourceKey<Level> dimensionKey) {
        ServerLevelData overworldData = main.worldData().overworldData();
        if (dimensionKey.equals(Level.OVERWORLD)) {
            return overworldData;
        }
        return new DerivedLevelData(main.worldData(), overworldData);
    }

    public static ChunkGenerator genChunkGenerator(WGMain main, ResourceKey<Level> dimensionKey) {
        ResourceKey<LevelStem> dimensionStemKey = Utils.levelToLevelStem(dimensionKey);
        LevelStem dimensionStem = main.worldStem()
                .registries()
                .getLayer(RegistryLayer.DIMENSIONS)
                .registryOrThrow(Registries.LEVEL_STEM)
                .get(dimensionStemKey);
        return dimensionStem.generator();
    }

    public static LevelStem genLevelStem(WGMain main, ResourceKey<Level> dimensionKey) {
        ResourceKey<LevelStem> dimensionStemKey = Utils.levelToLevelStem(dimensionKey);
        return main.worldStem()
                .registries()
                .getLayer(RegistryLayer.DIMENSIONS)
                .registryOrThrow(Registries.LEVEL_STEM)
                .get(dimensionStemKey);
    }

    public void safeEntitiesInChunk(long posIDX) {
        synchronized (extractedEntityManager) {
            ((PersistentEntitySectionManagerAccessor) extractedEntityManager).callProcessChunkUnload(posIDX);
        }
    }

    @Override
    public void close() throws IOException {
        generator.close();
        try (SimpleClosable c = genServerChunkCacheWhitelist()) {
            super.close();
        }
        WorldGen.currentLevel = null;
    }

    public WGMain main() {
        return main;
    }

    public WGGenerator generator() {
        return generator;
    }

    public SimpleClosable genServerChunkCacheWhitelist() {
        allowServerChunkCacheAccess.incrementAndGet();
        return () -> allowServerChunkCacheAccess.decrementAndGet();
    }

    public ChunkMap extractChunkMap() {
        return super.getChunkSource().chunkMap;
    }

    public ChunkGenerator chunkGenerator() {
        return chunkGenerator;
    }

    //  ======================
    // === Override section ===
    //  ======================

    @NotNull
    @Override
    public WGMinecraftServer getServer() {
        return (WGMinecraftServer) super.getServer();
    }

    @Override
    public DimensionDataStorage getDataStorage() {
        return dimensionDataStorage == null ? super.getChunkSource().getDataStorage() : dimensionDataStorage;
    }

    @Override
    public WGLevel getLevel() {
        return this;
    }

    @Override
    public WGInlineLeveLightEngine getLightEngine() {
        return levelLightEngines.computeIfAbsent(
                Thread.currentThread().getId(),
                x -> {
                    WorldGen.LOGGER.debug("Creating new light engine for thread {}", Thread.currentThread().getName());
                    return new WGInlineLeveLightEngine(
                            new WGLightChunkGetter(this),
                            true,
                            dimensionType().hasSkyLight()
                    );
                }
        );
    }

    public WGInlineLeveLightEngine getLightEngineFor(WGChunkHolder chunkHolder) {
        Long idx = chunkHolder.getValidLightThreadIndex();
        if (idx == null) {
            return getLightEngine();
        }
        return levelLightEngines.get((long) idx);
    }

    public List<WGInlineLeveLightEngine> getAllLightEnginesFor(WGChunkHolder chunkHolder) {
        List<WGInlineLeveLightEngine> res = new ArrayList<>();
        for (long idx : chunkHolder.getValidLightThreadIndexes()) {
            res.add(levelLightEngines.get(idx));
        }
        return res.stream().filter(Objects::nonNull).toList();
    }

    @Override
    public PoiManager getPoiManager() {
        return poiManager;
    }

    public RandomState getRandomState() {
        return randomState;
    }

    @Override
    public Holder<Biome> getUncachedNoiseBiome(int i, int j, int k) {
        return chunkGenerator.getBiomeSource().getNoiseBiome(i, j, k, randomState.sampler());
    }

    @Override
    public ServerChunkCache getChunkSource() {
        if (allowServerChunkCacheAccess.get() > 0) {
            return super.getChunkSource();
        }
        throw new RuntimeException("FORBIDDEN!");
    }

    // Chunk access
    @Override
    public LevelChunk getChunkAt(BlockPos blockPos) {
        final int x = SectionPos.blockToSectionCoord(blockPos.getX());
        final int z = SectionPos.blockToSectionCoord(blockPos.getZ());
        return (LevelChunk) getChunk(x, z, ChunkStatus.FULL, true);
    }

    @Override
    public LevelChunk getChunk(int x, int z) {
        return (LevelChunk) getChunk(x, z, ChunkStatus.FULL, true);
    }

    @Override
    public ChunkAccess getChunk(BlockPos blockPos) {
        final int x = SectionPos.blockToSectionCoord(blockPos.getX());
        final int z = SectionPos.blockToSectionCoord(blockPos.getZ());
        return getChunk(x, z, ChunkStatus.FULL, true);
    }

    @Override
    public ChunkAccess getChunk(int x, int z, ChunkStatus chunkStatus) {
        return getChunk(x, z, chunkStatus, true);
    }

    @Nullable
    @Override
    public ChunkAccess getChunk(int x, int z, ChunkStatus chunkStatus, boolean bl) {
        return generator.chunkStorage.getExistingChunk(ChunkPos.asLong(x, z), chunkStatus);
    }

    // Entity stuff
    @Override
    public void addWorldGenChunkEntities(Stream<Entity> stream) {
        synchronized (extractedEntityManager) {
            extractedEntityManager.addWorldGenChunkEntities(stream);
        }
    }

    @Override
    public void onBlockStateChange(BlockPos blockPos, BlockState blockState, BlockState blockState2) {
    }

    @Override
    public void onStructureStartsAvailable(ChunkAccess chunkAccess) {
        // super.onStructureStartsAvailable(chunkAccess);
    }

    // Etc.
    @Override
    public boolean isClientSide() {
        return true;
    }
}
