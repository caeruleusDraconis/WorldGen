package caeruleusTait.WorldGen.mixin;

import net.minecraft.client.gui.screens.worldselection.WorldOpenFlows;
import net.minecraft.server.WorldStem;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(WorldOpenFlows.class)
public interface WorldOpenFlowsAccessor {

    @Invoker
    WorldStem callLoadWorldStem(LevelStorageSource.LevelStorageAccess levelStorageAccess, boolean bl, PackRepository packRepository) throws Exception;

}
