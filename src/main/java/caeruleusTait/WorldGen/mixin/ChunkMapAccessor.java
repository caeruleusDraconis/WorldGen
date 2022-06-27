// Copyright 2022 - 2022, Caeruleus Draconis and Taiterio
// SPDX-License-Identifier: Apache-2.0

package caeruleusTait.WorldGen.mixin;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureManager;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.io.IOException;

@Mixin(ChunkMap.class)
public interface ChunkMapAccessor {

    @Accessor
    StructureManager getStructureManager();

    @Accessor
    ChunkGenerator getGenerator();

    @Invoker
    ChunkHolder callUpdateChunkScheduling(long l, int i, @Nullable ChunkHolder chunkHolder, int j);

    @Invoker
    CompoundTag callReadChunk(ChunkPos chunkPos) throws IOException;
}
