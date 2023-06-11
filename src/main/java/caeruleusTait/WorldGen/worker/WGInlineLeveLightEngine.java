// Copyright 2022 - 2022, Caeruleus Draconis and Taiterio
// SPDX-License-Identifier: Apache-2.0

package caeruleusTait.WorldGen.worker;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.lighting.LayerLightEventListener;
import net.minecraft.world.level.lighting.LayerLightSectionStorage;
import net.minecraft.world.level.lighting.LevelLightEngine;
import org.jetbrains.annotations.Nullable;

public class WGInlineLeveLightEngine extends LevelLightEngine {
    public WGInlineLeveLightEngine(
            LightChunkGetter lightChunkGetter,
            boolean lightBlocks,
            boolean lightSky
    ) {
        super(lightChunkGetter, lightBlocks, lightSky);
    }

    private void initChunkData(ChunkAccess chunkAccess) {
        ChunkPos chunkPos = chunkAccess.getPos();
        LevelChunkSection[] levelChunkSections = chunkAccess.getSections();

        for(int i = 0; i < chunkAccess.getSectionsCount(); ++i) {
            LevelChunkSection levelChunkSection = levelChunkSections[i];
            if (!levelChunkSection.hasOnlyAir()) {
                int j = this.levelHeightAccessor.getSectionYFromSectionIndex(i);
                super.updateSectionStatus(SectionPos.of(chunkPos, j), false);
            }
        }
    }

    public synchronized ChunkAccess initializeLight(ChunkAccess chunkAccess) {
        ChunkPos chunkPos = chunkAccess.getPos();
        initChunkData(chunkAccess);

        super.runLightUpdates();

        super.setLightEnabled(chunkPos, false);
        super.retainData(chunkPos, false);
        return chunkAccess;
    }

    public synchronized ChunkAccess lightChunk(ChunkAccess chunkAccess) {
        ChunkPos chunkPos = chunkAccess.getPos();
        chunkAccess.setLightCorrect(false);
        super.propagateLightSources(chunkPos);

        super.runLightUpdates();

        chunkAccess.setLightCorrect(true);
        // this.chunkMap.releaseLightTicket(chunkPos);
        return chunkAccess;
    }

    /**
     * Starting with 1.20, when chunks are loaded from disk, the light data is only
     * loaded in a queue and still must be further processed.
     */
    public synchronized ChunkAccess prepareLoadedChunk(ChunkAccess chunkAccess) {
        if (!chunkAccess.getStatus().isOrAfter(ChunkStatus.INITIALIZE_LIGHT)) {
            return chunkAccess;
        }

        initChunkData(chunkAccess);

        return chunkAccess;
    }

    public synchronized void unloadChunk(ChunkPos chunkPos) {
        super.retainData(chunkPos, false);
        super.setLightEnabled(chunkPos, false);
        for (int i = getMinLightSection(); i < getMaxLightSection(); ++i) {
            super.queueSectionData(LightLayer.BLOCK, SectionPos.of(chunkPos, i), null);
            super.queueSectionData(LightLayer.SKY, SectionPos.of(chunkPos, i), null);
        }
        for (int i = this.levelHeightAccessor.getMinSection(); i < this.levelHeightAccessor.getMaxSection(); ++i) {
            super.updateSectionStatus(SectionPos.of(chunkPos, i), true);
        }
    }

    @Override
    public synchronized void checkBlock(BlockPos blockPos) {
        super.checkBlock(blockPos);
    }

    @Override
    public synchronized boolean hasLightWork() {
        return super.hasLightWork();
    }

    @Override
    public synchronized int runLightUpdates() {
        return super.runLightUpdates();
    }

    @Override
    public synchronized void updateSectionStatus(SectionPos sectionPos, boolean bl) {
        super.updateSectionStatus(sectionPos, bl);
    }

    @Override
    public synchronized void setLightEnabled(ChunkPos chunkPos, boolean bl) {
        super.setLightEnabled(chunkPos, bl);
    }

    @Override
    public synchronized void propagateLightSources(ChunkPos chunkPos) {
        super.propagateLightSources(chunkPos);
    }

    @Override
    public synchronized LayerLightEventListener getLayerListener(LightLayer lightLayer) {
        return super.getLayerListener(lightLayer);
    }

    @Override
    public synchronized String getDebugData(LightLayer lightLayer, SectionPos sectionPos) {
        return super.getDebugData(lightLayer, sectionPos);
    }

    @Override
    public synchronized LayerLightSectionStorage.SectionType getDebugSectionType(LightLayer lightLayer, SectionPos sectionPos) {
        return super.getDebugSectionType(lightLayer, sectionPos);
    }

    @Override
    public synchronized void queueSectionData(LightLayer lightLayer, SectionPos sectionPos, @Nullable DataLayer dataLayer) {
        super.queueSectionData(lightLayer, sectionPos, dataLayer);
    }

    @Override
    public synchronized void retainData(ChunkPos chunkPos, boolean bl) {
        super.retainData(chunkPos, bl);
    }

    @Override
    public synchronized int getRawBrightness(BlockPos blockPos, int i) {
        return super.getRawBrightness(blockPos, i);
    }

    @Override
    public synchronized boolean lightOnInSection(SectionPos sectionPos) {
        return super.lightOnInSection(sectionPos);
    }

    @Override
    public int getLightSectionCount() {
        return super.getLightSectionCount();
    }

    @Override
    public int getMinLightSection() {
        return super.getMinLightSection();
    }

    @Override
    public int getMaxLightSection() {
        return super.getMaxLightSection();
    }

    @Override
    public synchronized void updateSectionStatus(BlockPos blockPos, boolean bl) {
        super.updateSectionStatus(blockPos, bl);
    }
}
