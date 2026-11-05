package com.awesomeshot5051.easyfarms.farm;

import net.minecraft.util.StringRepresentable;

/** What the bottom of the glass case looks like. Resource = stone, Mob = lava + stone, Plant follows its soil slot. */
public enum FarmFloor implements StringRepresentable {
    STONE("minecraft:block/stone", "minecraft:block/stone"),
    LAVA("minecraft:block/lava_still", "minecraft:block/stone"),
    GRASS("minecraft:block/grass_block_top", "minecraft:block/grass_block_top"),
    FARMLAND("easy_farms:block/waterstill", "minecraft:block/farmland"),
    SOUL_SAND("minecraft:block/soul_sand", "minecraft:block/soul_sand");

    /** Texture of the front half and the back half of the floor. */
    public final String front;
    public final String back;

    FarmFloor(String front, String back) {
        this.front = front;
        this.back = back;
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase();
    }

    public static FarmFloor defaultFor(FarmKind kind) {
        return switch (kind) {
            case RESOURCE -> STONE;
            case MOB -> LAVA;
            case PLANT -> GRASS;
            case LIQUID -> STONE;
        };
    }
}
