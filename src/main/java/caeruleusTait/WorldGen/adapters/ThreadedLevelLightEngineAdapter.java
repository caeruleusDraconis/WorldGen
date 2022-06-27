// Copyright 2022 - 2022, Caeruleus Draconis and Taiterio
// SPDX-License-Identifier: Apache-2.0

package caeruleusTait.WorldGen.adapters;

import caeruleusTait.WorldGen.WorldGen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ThreadedLevelLightEngine;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.level.lighting.LayerLightEventListener;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class ThreadedLevelLightEngineAdapter extends ThreadedLevelLightEngine {
    public ThreadedLevelLightEngineAdapter() {
        super(new LightChunkGetter() {
            @Nullable
            @Override
            public BlockGetter getChunkForLighting(int i, int j) {
                return null;
            }

            @Override
            public BlockGetter getLevel() {
                return null;
            }
        }, null, false, null, null);
    }

    @Override
    public CompletableFuture<ChunkAccess> retainData(ChunkAccess chunkAccess) {
        return CompletableFuture.completedFuture(chunkAccess);
    }

    //  =================
    // === NyI Section ===
    //  =================

    @Override
    public void close() {
        throw new RuntimeException("NyI");
    }

    @Override
    public int runUpdates(int i, boolean bl, boolean bl2) {
        throw new RuntimeException("NyI");
    }

    @Override
    public void onBlockEmissionIncrease(BlockPos blockPos, int i) {
        throw new RuntimeException("NyI");
    }

    @Override
    public void checkBlock(BlockPos blockPos) {
        WorldGen.currentLevel.getLightEngine().checkBlock(blockPos);
    }

    @Override
    protected void updateChunkStatus(ChunkPos chunkPos) {
        throw new RuntimeException("NyI");
    }

    @Override
    public void updateSectionStatus(SectionPos sectionPos, boolean bl) {
        throw new RuntimeException("NyI");
    }

    @Override
    public void enableLightSources(ChunkPos chunkPos, boolean bl) {
        throw new RuntimeException("NyI");
    }

    @Override
    public void queueSectionData(LightLayer lightLayer, SectionPos sectionPos, @Nullable DataLayer dataLayer, boolean bl) {
        throw new RuntimeException("NyI");
    }

    @Override
    public void retainData(ChunkPos chunkPos, boolean bl) {
        throw new RuntimeException("NyI");
    }

    @Override
    public CompletableFuture<ChunkAccess> lightChunk(ChunkAccess chunkAccess, boolean bl) {
        throw new RuntimeException("NyI");
    }

    @Override
    public void tryScheduleUpdate() {
        throw new RuntimeException("NyI");
    }

    @Override
    public void setTaskPerBatch(int i) {
        throw new RuntimeException("NyI");
    }

    @Override
    public boolean hasLightWork() {
        throw new RuntimeException("NyI");
    }

    @Override
    public LayerLightEventListener getLayerListener(LightLayer lightLayer) {
        throw new RuntimeException("NyI");
    }

    @Override
    public String getDebugData(LightLayer lightLayer, SectionPos sectionPos) {
        throw new RuntimeException("NyI");
    }

    @Override
    public int getRawBrightness(BlockPos blockPos, int i) {
        throw new RuntimeException("NyI");
    }

    @Override
    public int getLightSectionCount() {
        throw new RuntimeException("NyI");
    }

    @Override
    public int getMinLightSection() {
        throw new RuntimeException("NyI");
    }

    @Override
    public int getMaxLightSection() {
        throw new RuntimeException("NyI");
    }

    @Override
    public void updateSectionStatus(BlockPos blockPos, boolean bl) {
        throw new RuntimeException("NyI");
    }
}
