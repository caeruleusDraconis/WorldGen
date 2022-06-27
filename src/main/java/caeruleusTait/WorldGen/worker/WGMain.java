// Copyright 2022 - 2022, Caeruleus Draconis and Taiterio
// SPDX-License-Identifier: Apache-2.0

package caeruleusTait.WorldGen.worker;

import caeruleusTait.WorldGen.gui.screens.WGLoadingScreen;
import caeruleusTait.WorldGen.util.WGChunkListGenerator;
import caeruleusTait.WorldGen.worker.storage.WGChunkHolder;
import com.mojang.datafixers.DataFixer;
import caeruleusTait.WorldGen.WorldGen;
import caeruleusTait.WorldGen.config.WGConfigState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.WorldStem;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.FolderRepositorySource;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.repository.ServerPacksSource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.WorldData;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

public class WGMain {

    private final Minecraft minecraft;
    private final String levelID;

    final private LevelStorageSource.LevelStorageAccess levelStorageAccess;
    final private WorldStem worldStem;
    final private PackRepository packRepository;
    final private Thread mainThread;

    private WGMinecraftServer minecraftServer;
    private WGLevel level;

    private final Map<Integer, ExecutorService> executorServices;

    private final AtomicInteger genCount;
    private final WGConfigState cfg;

    private WGChunkGenWorkHost activeWorkHost;

    public WGMain(Minecraft _minecraft, String _levelID) throws Exception {
        minecraft = _minecraft;
        levelID = _levelID;

        cfg = WGConfigState.get();
        this.genCount = new AtomicInteger();

        levelStorageAccess = levelSource().createAccess(levelID);
        // See Minecraft.createPackRepository() --> No static invoker for hot reloads [UPGRADE_WATCH]
        packRepository = new PackRepository(
                PackType.SERVER_DATA,
                new ServerPacksSource(),
                new FolderRepositorySource(levelStorageAccess.getLevelPath(LevelResource.DATAPACK_DIR).toFile(), PackSource.WORLD)
        );
        worldStem = minecraft.createWorldOpenFlows().loadWorldStem(levelStorageAccess, true);

        /*
        // Do NOT forget this call!
        worldStem.updateGlobals();
        // Seriously, I spent hours debugging and jumping through the
        // Minecraft source code to figure out that the registry was
        // not fully initialized!
         */

        mainThread = new Thread();
        executorServices = new HashMap<>();
        activeWorkHost = null;
    }

    public void load(WGConfigState cfg, ChunkProgressListener listener, int maxPossibleThreads) {
        minecraftServer = new WGMinecraftServer(this, listener);
        level = new WGLevel(this, cfg.dimension, maxPossibleThreads);
        executorServices.clear();
        genCount.set(0);
    }

    public WGLevel getWGLevel() {
        return level;
    }

    public int getGenCount() {
        return genCount.get();
    }


    public void generateChunk(ChunkPos pos) {
        try {
            WGChunkHolder ch = level.generator().loadOrGen(pos, cfg.maxStatus, false);
            if (!ch.getStatus().isOrAfter(cfg.maxStatus)) {
                WorldGen.LOGGER.error("FAILED TO GENERATE: {} -- {} NOT FULL", ch.getPos(), ch.getStatus());
            }
        } catch (Throwable c) {
            c.printStackTrace();
            executorServices.values().forEach(ExecutorService::shutdown);
        }
        genCount.incrementAndGet();
    }

    public void startWork(ChunkPos start, ChunkPos end, Predicate<? super ChunkPos> filter, Screen lastScreen, Screen selectWorldScreen) {
        activeWorkHost = new WGChunkGenWorkHost(this, cfg.maxThreads);
        List<WGChunkWorkUnit> workList = WGChunkListGenerator.generateChunkWorkList(activeWorkHost, WGConfigState.BLOCK_SIZE, cfg.maxThreads, start, end, filter);


        int totalCount = 0;

        for(WGChunkWorkUnit workUnit : workList) {
            totalCount += workUnit.getChunkPositions().size();
        }

        int radius = Math.max(end.z - start.z, end.x - start.x) / 2;

        WGChunkProgressListener cpl = new WGChunkProgressListener(radius, totalCount);

        load(cfg, cpl, WGChunkListGenerator.maxPossibleThreads(WGConfigState.BLOCK_SIZE, start, end, cfg.maxThreads));

        cpl.start();
        cpl.updateSpawnPos(cfg.center); // The Spawn Pos is just relevant for updating the progress bar
        final Screen loadingScreen = new WGLoadingScreen(lastScreen, selectWorldScreen, cpl, this, totalCount);

        this.minecraft.setScreen(loadingScreen);

        activeWorkHost.submitWork(workList);
    }

    public void abortWork() {
        if(activeWorkHost != null) {
            activeWorkHost.abortWork();
            activeWorkHost = null;
        }
    }

    public void close() {
        abortWork();
        executorServices.values().forEach(ExecutorService::shutdown);
        executorServices.values().forEach(x -> {
            try {
                // Do nothing if this fails since this should be very unlikely. Additionally,
                // we don't want to lose or corrupt data by throwing an Exception.
                x.awaitTermination(60, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        executorServices.clear();

        if (level != null) {
            try {
                level.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        if (minecraftServer != null) {
            minecraftServer.chunkProgressListener().stop();
            minecraftServer.close();
        }
        if (worldStem != null) {
            worldStem.close();
        }
        if (levelStorageAccess != null) {
            try {
                levelStorageAccess.close();
            } catch (IOException iOException) {
                WorldGen.LOGGER.error("Failed to unlock level {} ({})", levelID, iOException);
            }
        }
    }

    public RegistryAccess registryAccess() {
        return worldStem.registryAccess();
    }

    public LevelStorageSource.LevelStorageAccess levelStorageAccess() {
        return levelStorageAccess;
    }

    // Convenient access functions
    public WorldData worldData() {
        return worldStem.worldData();
    }

    public ResourceManager resourceManager() {
        return worldStem.resourceManager();
    }

    public WorldGenSettings worldGenSettings() {
        return worldData().worldGenSettings();
    }

    public LevelStorageSource levelSource() {
        return minecraft.getLevelSource();
    }

    public DataFixer dataFixer() {
        return minecraft.getFixerUpper();
    }

    public WorldStem worldStem() {
        return worldStem;
    }

    public PackRepository packRepository() {
        return packRepository;
    }

    public Minecraft minecraft() {
        return minecraft;
    }

    public WGMinecraftServer minecraftServer() {
        return minecraftServer;
    }

    public Thread mainThread() {
        return mainThread;
    }

}
