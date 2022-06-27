package caeruleusTait.WorldGen.mixin;

import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.lighting.LevelLightEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ProtoChunk.class)
public interface ProtoChunkAccessor {

    @Accessor
    LevelLightEngine getLightEngine();

    @Accessor
    void setLightEngine(LevelLightEngine lightEngine);

}
