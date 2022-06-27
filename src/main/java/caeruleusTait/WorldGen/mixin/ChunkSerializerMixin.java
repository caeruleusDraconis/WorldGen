// Copyright 2022 - 2022, Caeruleus Draconis and Taiterio
// SPDX-License-Identifier: Apache-2.0

package caeruleusTait.WorldGen.mixin;

import caeruleusTait.WorldGen.WorldGen;
import net.minecraft.world.level.chunk.storage.ChunkSerializer;
import net.minecraft.world.level.lighting.LevelLightEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ChunkSerializer.class)
public class ChunkSerializerMixin {

    @ModifyVariable(method = "read", at = @At("STORE"))
    private static LevelLightEngine readMixin(LevelLightEngine orig) {
        if (orig != null && WorldGen.currentLevel != null) {
            return WorldGen.currentLevel.getLightEngine();
        }
        return orig;
    }

    @ModifyVariable(method = "write", at = @At("STORE"))
    private static LevelLightEngine writeMixin(LevelLightEngine orig) {
        if (orig != null && WorldGen.currentLevel != null) {
            // Always try to return the light engine that already has
            // the light data stored. Since this happened in another thread
            // in 99% of cases, the getLightEngine() call is not enough,
            // since that one only returns the engine for the current thread.
            return WorldGen.writeLightEngine.get();
        }
        return orig;
    }

}
