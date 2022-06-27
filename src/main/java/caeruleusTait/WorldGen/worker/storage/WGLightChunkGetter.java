// Copyright 2022 - 2022, Caeruleus Draconis and Taiterio
// SPDX-License-Identifier: Apache-2.0

package caeruleusTait.WorldGen.worker.storage;

import caeruleusTait.WorldGen.worker.WGLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.level.material.FluidState;
import org.jetbrains.annotations.Nullable;

public class WGLightChunkGetter implements LightChunkGetter {
    private final WGLevel level;
    private final WGBlockGetter blockGetter;

    public WGLightChunkGetter(WGLevel _level) {
        level = _level;
        blockGetter = new WGBlockGetter();
    }

    @Nullable
    @Override
    public BlockGetter getChunkForLighting(int x, int z) {
        return level.getChunk(x, z, ChunkStatus.FEATURES);
    }

    @Override
    public BlockGetter getLevel() {
        return blockGetter;
    }

    @Override
    public void onLightUpdate(LightLayer lightLayer, SectionPos sectionPos) {
        // TODO: Is this required or can it be ignored?
    }

    public class WGBlockGetter implements BlockGetter {

        @Nullable
        @Override
        public BlockEntity getBlockEntity(BlockPos blockPos) {
            throw new RuntimeException("NyI");
        }

        @Override
        public BlockState getBlockState(BlockPos blockPos) {
            throw new RuntimeException("NyI");
        }

        @Override
        public FluidState getFluidState(BlockPos blockPos) {
            throw new RuntimeException("NyI");
        }

        @Override
        public int getHeight() {
            return level.getHeight();
        }

        @Override
        public int getMinBuildHeight() {
            return level.getMinBuildHeight();
        }
    }
}
