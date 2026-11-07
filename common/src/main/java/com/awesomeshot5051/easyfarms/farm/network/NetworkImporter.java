package com.awesomeshot5051.easyfarms.farm.network;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

/**
 * Pushes items into one kind of storage network (AE2, Refined Storage, Dim Storage). Implementations live in the loader module that
 * can see the other mod's API and are registered in {@link NetworkImporters} only while that mod is loaded, so the main mod never
 * touches an API that is not there.
 */
public interface NetworkImporter {
    /** True if the block at the position is part of this kind of storage network (so a card can be linked to it). */
    default boolean canLink(ServerLevel level, BlockPos pos) {
        return true;
    }

    /**
     * Offers a stack to the network a farm is connected to.
     *
     * @param farm  the farm's position (a network next to the farm is joined by touching it)
     * @param card  the Auto Importer card in the farm: its {@code easy_farms:link} (a block of the network) or {@code easy_farms:frequency} (DimStorage)
     * @param stack the items to store (not modified)
     * @return what the network did not take (empty when everything fit)
     */
    ItemStack insert(ServerLevel level, BlockPos farm, ItemStack card, ItemStack stack);
}
