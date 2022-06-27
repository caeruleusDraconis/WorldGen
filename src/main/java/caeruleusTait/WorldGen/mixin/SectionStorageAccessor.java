package caeruleusTait.WorldGen.mixin;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.world.level.chunk.storage.SectionStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Optional;

@Mixin(SectionStorage.class)
public interface SectionStorageAccessor<R> {

    @Accessor
    Long2ObjectMap<Optional<R>> getStorage();
}
