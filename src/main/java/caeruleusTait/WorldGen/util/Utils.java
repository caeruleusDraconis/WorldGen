package caeruleusTait.WorldGen.util;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.LevelStem;

public final class Utils {

    public static ResourceKey<Level> levelStemToLevel(ResourceKey<LevelStem> level) {
        return ResourceKey.create(Registries.DIMENSION, level.location());
    }

    public static ResourceKey<LevelStem> levelToLevelStem(ResourceKey<Level> level) {
        return ResourceKey.create(Registries.LEVEL_STEM, level.location());
    }

}
