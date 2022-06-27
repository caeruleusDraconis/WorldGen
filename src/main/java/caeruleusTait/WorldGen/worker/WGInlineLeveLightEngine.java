// Copyright 2022 - 2022, Caeruleus Draconis and Taiterio
// SPDX-License-Identifier: Apache-2.0

package caeruleusTait.WorldGen.worker;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.level.lighting.LayerLightEventListener;
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

    public synchronized ChunkAccess lightChunk(ChunkAccess chunkAccess, boolean bl) {
        final ChunkPos chunkPos = chunkAccess.getPos();
        chunkAccess.setLightCorrect(false);
        LevelChunkSection[] levelChunkSections = chunkAccess.getSections();

        for(int i = 0; i < chunkAccess.getSectionsCount(); ++i) {
            LevelChunkSection levelChunkSection = levelChunkSections[i];
            if (!levelChunkSection.hasOnlyAir()) {
                int j = this.levelHeightAccessor.getSectionYFromSectionIndex(i);
                updateSectionStatus(SectionPos.of(chunkPos, j), false);
            }
        }

        super.enableLightSources(chunkPos, true);
        if (!bl) {
            chunkAccess.getLights().forEach((blockPos) -> {
                onBlockEmissionIncrease(blockPos, chunkAccess.getLightEmission(blockPos));
            });
        }

        runUpdates(Integer.MAX_VALUE, true, true);

        chunkAccess.setLightCorrect(true);
        retainData(chunkPos, false);

        return chunkAccess;
    }

    public synchronized void unloadChunk(ChunkPos chunkPos) {
        super.retainData(chunkPos, false);
        super.enableLightSources(chunkPos, false);
        for (int i = getMinLightSection(); i < getMaxLightSection(); ++i) {
            super.queueSectionData(LightLayer.BLOCK, SectionPos.of(chunkPos, i), null, true);
            super.queueSectionData(LightLayer.SKY, SectionPos.of(chunkPos, i), null, true);
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
    public synchronized void onBlockEmissionIncrease(BlockPos blockPos, int i) {
        super.onBlockEmissionIncrease(blockPos, i);
    }

    @Override
    public synchronized boolean hasLightWork() {
        return super.hasLightWork();
    }

    @Override
    public synchronized int runUpdates(int i, boolean bl, boolean bl2) {
        return super.runUpdates(i, bl, bl2);
    }

    @Override
    public synchronized void updateSectionStatus(SectionPos sectionPos, boolean bl) {
        super.updateSectionStatus(sectionPos, bl);
    }

    @Override
    public synchronized void enableLightSources(ChunkPos chunkPos, boolean bl) {
        super.enableLightSources(chunkPos, bl);
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
    public synchronized void queueSectionData(LightLayer lightLayer, SectionPos sectionPos, @Nullable DataLayer dataLayer, boolean bl) {
        super.queueSectionData(lightLayer, sectionPos, dataLayer, bl);
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
