package com.awesomeshot5051.easyfarms.crafting;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;

/**
 * Knows how to craft with one kind of grid. This is the code half of the Crafting card's extension point: another mod
 * registers an instance in {@link CraftingGridTypes#REGISTRY} (key {@link CraftingGridTypes#KEY}), and a
 * {@code data/<ns>/easy_farms/crafting_grid/*.json} file then puts a grid of that type (with its size) into the card.
 * The type does the matching, so it can use any recipe type or custom rules (Create's 5x5 mechanical crafting, for example).
 */
public interface CraftingGridType {
    /**
     * Crafts {@code target} as many times as the items allow, up to {@code maxCrafts}.
     *
     * @param available the items the farm can spend (merged by item and components; do not modify them)
     * @return what was made and what it cost, or empty if the target cannot be crafted from {@code available} in this grid
     */
    Optional<Crafted> craft(ServerLevel level, CraftingGrid grid, ItemStack target, List<ItemStack> available, int maxCrafts);

    /** True if some recipe in this grid makes {@code target}, whatever the farm holds right now (the targets screen shows it). */
    boolean canCraft(ServerLevel level, CraftingGrid grid, ItemStack target);

    /** False if this type cannot work in this installation (for example its mod is missing); its grids are then ignored. */
    default boolean available() {
        return true;
    }

    /**
     * The outcome of crafting.
     *
     * @param results  everything produced: the crafted items plus recipe leftovers such as empty buckets (merged, not over-stacked)
     * @param consumed the items used up (merged)
     * @param crafts   how many times the recipe ran
     */
    record Crafted(List<ItemStack> results, List<ItemStack> consumed, int crafts) {
    }
}
