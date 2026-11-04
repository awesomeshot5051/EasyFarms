package com.awesomeshot5051.easyfarms.client;

import com.awesomeshot5051.easyfarms.farm.FarmBlock;
import com.awesomeshot5051.easyfarms.farm.FarmFloor;
import com.awesomeshot5051.easyfarms.registry.UpgradeCardItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.core.BlockPos;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.world.level.GrassColor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.Set;

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
    public static final BlockTintSource FLOOR_TINT = new BlockTintSource() {
        @Override
        public int color(BlockState state) {
            return state.getValue(FarmBlock.FLOOR) == FarmFloor.GRASS ? GrassColor.getDefaultColor() : 0xFFFFFFFF;
        }

        @Override
        public int colorInWorld(BlockState state, BlockAndTintGetter level, BlockPos pos) {
            return state.getValue(FarmBlock.FLOOR) == FarmFloor.GRASS ? BiomeColors.getAverageGrassColor(level, pos) : 0xFFFFFFFF;
        }

        @Override
        public Set<Property<?>> relevantProperties() {
            return Set.of(FarmBlock.FLOOR);
        }
    };
}
