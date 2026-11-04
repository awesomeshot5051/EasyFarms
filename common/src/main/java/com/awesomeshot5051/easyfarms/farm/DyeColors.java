package com.awesomeshot5051.easyfarms.farm;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;

/** Dyeable recipes name their white variant ("minecraft:white_wool"); a dyed farm swaps in its own colour. */
public final class DyeColors {
    private DyeColors() {
    }

    public static ResourceLocation recolor(ResourceLocation id, DyeColor color) {
        String path = id.getPath();
        return path.startsWith("white_") ? id.withPath(color.getName() + path.substring("white".length())) : id;
    }
}
