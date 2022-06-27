// Copyright 2022 - 2022, Caeruleus Draconis and Taiterio
// SPDX-License-Identifier: Apache-2.0

package caeruleusTait.WorldGen.mixin;

import caeruleusTait.WorldGen.WorldGen;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.lighting.LevelLightEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ProtoChunk.class)
public class ProtoChunkMixin {

    @Inject(method = "setLightEngine", at = @At("TAIL"))
    private void useOurLevelLightEngine(LevelLightEngine levelLightEngine, CallbackInfo ci) {
        if (levelLightEngine != null && WorldGen.currentLevel != null) {
            ((ProtoChunkAccessor) this).setLightEngine(WorldGen.currentLevel.getLightEngine());
        }
    }

}
