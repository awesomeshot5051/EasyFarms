package com.awesomeshot5051.easyfarms.upgrade;

import com.awesomeshot5051.easyfarms.farm.FarmBlockEntity;
import com.awesomeshot5051.easyfarms.recipe.FarmRecipe;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.Function;

/**
 * One behaviour an upgrade card gives a farm. A card lists its effects in its farm_upgrade JSON:
 * <pre>"effects": [{"type": "easy_farms:cycle_time", "divisor": 8}, {"type": "mymod:my_effect", ...}]</pre>
 * <p>
 * Other mods add their own effect types by registering a {@link MapCodec} in {@link UpgradeEffects#KEY}
 * (registry {@code easy_farms:upgrade_effect_type}) and overriding whichever hooks below they need. Every hook has a
 * "no change" default, so an effect only implements what it changes. Hooks run on both logical sides unless noted.
 */
public interface UpgradeEffect {
    Codec<UpgradeEffect> CODEC = UpgradeEffects.REGISTRY.byNameCodec().dispatch(UpgradeEffect::codec, Function.identity());

    MapCodec<? extends UpgradeEffect> codec();

    /** Adjust the ticks one cycle takes (after tool tier maths). */
    default double modifyCycleTicks(FarmBlockEntity farm, FarmRecipe recipe, double ticks) {
        return ticks;
    }

    /** True while the farm should not work (for example "no redstone signal"). */
    default boolean pausesFarm(FarmBlockEntity farm) {
        return false;
    }

    /** True if drops with a smelted form should use it. */
    default boolean smelts(FarmRecipe recipe) {
        return false;
    }

    /** True if this recipe may run without its required environment. */
    default boolean waivesEnvironment(FarmRecipe recipe) {
        return false;
    }

    /** True if the inputs are never used up. */
    default boolean infiniteInputs() {
        return false;
    }

    /** Multiplier on how long the inputs last. */
    default double inputLifetimeMultiplier() {
        return 1;
    }

    /** Server side: change the drops of a finished cycle before they go into the output slots. */
    default void modifyDrops(FarmBlockEntity farm, ServerLevel level, FarmRecipe recipe, List<ItemStack> drops) {
    }

    /**
     * Client side, for recipe viewers: what this effect would turn a recipe's drop table into. The default changes
     * nothing; an effect that rewrites drops (convert_drops) returns the converted table.
     */
    default List<com.awesomeshot5051.easyfarms.farm.DropEntry> previewDrops(List<com.awesomeshot5051.easyfarms.farm.DropEntry> drops) {
        return drops;
    }
}
