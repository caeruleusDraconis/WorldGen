// Copyright 2022 - 2022, Caeruleus Draconis and Taiterio
// SPDX-License-Identifier: Apache-2.0

package caeruleusTait.WorldGen.mixin;

import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.storage.RegionFile;
import net.minecraft.world.level.chunk.storage.RegionFileStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.locks.ReentrantLock;

@Mixin(RegionFileStorage.class)
public abstract class RegionFileStorageMixin {

    final private ReentrantLock mutex = new ReentrantLock();

    @Inject(method = "getRegionFile", at = @At("HEAD"))
    private void acquireLock(ChunkPos chunkPos, CallbackInfoReturnable<RegionFile> cir) {
        mutex.lock();
    }

    @Inject(method = "getRegionFile", at = @At("RETURN"))
    private void releaseLock(ChunkPos chunkPos, CallbackInfoReturnable<RegionFile> cir) {
        mutex.unlock();
    }

}
