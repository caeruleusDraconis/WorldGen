// Copyright 2022 - 2022, Caeruleus Draconis and Taiterio
// SPDX-License-Identifier: Apache-2.0

package caeruleusTait.WorldGen.worker.storage;

import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.lighting.LayerLightEventListener;
import net.minecraft.world.level.lighting.LevelLightEngine;

public class WGLightEngineData {

    private final ChunkPos chunkPos;
    private final DataLayer[] skyLayers;
    private final DataLayer[] blockLayers;

    private final int min;
    private final int max;
    private final int count;

    public WGLightEngineData(LevelLightEngine engine, ChunkPos pos) {
        chunkPos = pos;
        min = engine.getMinLightSection();
        max = engine.getMaxLightSection();
        count = max - min;
        skyLayers = new DataLayer[count];
        blockLayers = new DataLayer[count];

        final LayerLightEventListener skyListener = engine.getLayerListener(LightLayer.SKY);
        final LayerLightEventListener blockListener = engine.getLayerListener(LightLayer.BLOCK);

        for (int i = min; i < max; ++i) {
            final SectionPos sPos = SectionPos.of(chunkPos, i);
            skyLayers[i - min] = skyListener.getDataLayerData(sPos);
            blockLayers[i - min] = blockListener.getDataLayerData(sPos);
        }
    }

    public void loadLightEngineData(LevelLightEngine engine) {
        for (int i = min; i < max; ++i) {
            final SectionPos sPos = SectionPos.of(chunkPos, i);
            final DataLayer sky = skyLayers[i - min];
            final DataLayer block = blockLayers[i - min];

            if (sky != null && !sky.isEmpty()) {
                engine.queueSectionData(LightLayer.SKY, sPos, sky, true);
            }

            if (block != null && !block.isEmpty()) {
                engine.queueSectionData(LightLayer.BLOCK, sPos, block, true);
            }
        }
    }

}
