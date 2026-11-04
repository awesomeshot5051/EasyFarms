package com.awesomeshot5051.easyfarms.client;

import com.awesomeshot5051.easyfarms.farm.FarmBlock;
import com.awesomeshot5051.easyfarms.farm.FarmFloor;
import com.awesomeshot5051.easyfarms.registry.UpgradeCardItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.GrassColor;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** Client setup that is the same on every loader. */
public final class ClientCommon {
    private ClientCommon() {
    }

    /** Gives the blank datapack card access to the synced farm_upgrade registry (null outside a world). */
    public static void init() {
        UpgradeCardItem.registryAccess = () -> {
            var level = Minecraft.getInstance().level;
            return level == null ? null : level.registryAccess();
        };
    }

    /** Grass floors are tinted with the biome's grass colour; every other floor is left untinted. */
    public static int farmTint(BlockState state, @Nullable BlockAndTintGetter level, @Nullable BlockPos pos, int tintIndex) {
        if (tintIndex != 0 || state.getValue(FarmBlock.FLOOR) != FarmFloor.GRASS) return 0xFFFFFF;
        return level != null && pos != null ? BiomeColors.getAverageGrassColor(level, pos) : GrassColor.getDefaultColor();
    }
}
