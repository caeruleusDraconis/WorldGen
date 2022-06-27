// Copyright 2022 - 2022, Caeruleus Draconis and Taiterio
// SPDX-License-Identifier: Apache-2.0

package caeruleusTait.WorldGen.config;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkStatus;

public class WGConfigState {

    private static WGConfigState INSTANCE = null;

    // The size of individual lines
    public static final int BLOCK_SIZE = 8;

    /**
     * Common settings
     */
    public PregenSizeMode pregenMode = PregenSizeMode.SQUARE;
    public ResourceKey<Level> dimension = Level.OVERWORLD;
    // public int cachedChunks = 4096;

    /**
     * Advanced settings
     */
    public boolean enableThreads = true;
    public int maxThreads;
    public boolean enableProgress = true;
    public ChunkStatus maxStatus = ChunkStatus.FULL;

    /**
     * Settings for {@link PregenSizeMode#SQUARE} and {@link PregenSizeMode#CIRCLE}
     */
    public int radius = 64;
    public ChunkPos center = new ChunkPos(0, 0);

    /**
     * Settings for {@link PregenSizeMode#RECTANGLE}
     */
    public int hSize = 32;
    public int vSize = 32;

    /**
     * Settings for {@link PregenSizeMode#COORDINATES}
     */
    public ChunkPos upperLeft = new ChunkPos(-16, -16);
    public ChunkPos lowerRight = new ChunkPos(16, 16);

    /**
     * DANGER ZONE
     */
    public boolean enableWorldBreakingOptions = false;
    public boolean overrideExistingChunks = false;
    public ChunkStatus fakeStatus = null;

    private WGConfigState() {
        final double nThreads = Runtime.getRuntime().availableProcessors();

        // Fancy math to have some spare threads for writing chunks
        maxThreads = (int) Math.max(1., nThreads - Math.min(1. + (nThreads / 8.), Math.max(1. + Math.log(nThreads), 1.)));
    }

    public static WGConfigState get() {
        if (INSTANCE == null) {
            INSTANCE = new WGConfigState();
        }
        return INSTANCE;
    }

}
