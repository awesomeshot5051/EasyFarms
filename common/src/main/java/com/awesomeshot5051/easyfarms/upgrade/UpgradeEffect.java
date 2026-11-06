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

    /** False if this effect cannot work in this installation (the card is then refused). Power needs an energy API. */
    default boolean available() {
        return true;
    }

    /** Why {@link #available()} is false (shown in the card's tooltip), or null. */
    default net.minecraft.network.chat.Component unavailableReason() {
        return null;
    }

    /** Lets a card replace the tool tier the cycle time is computed from (for example the Powered card's diamond speed). */
    default com.awesomeshot5051.easyfarms.tier.FarmTier overrideTier(FarmBlockEntity farm, com.awesomeshot5051.easyfarms.tier.FarmTier current) {
        return current;
    }

    /** Server side, once per tick in which the farm really works (not paused, blocked or idle). */
    default void onWorkTick(FarmBlockEntity farm) {
    }

    /** Base energy capacity (ME) this card gives the farm; 0 for cards that need no power. */
    default long energyCapacity() {
        return 0;
    }

    /** True if this card destroys items that do not fit the output slots, so the farm never stalls on a full output (Void card). */
    default boolean voidsOverflow() {
        return false;
    }

    /** True if this card makes the farm drop the recipe's experience (Experience card on Mob farms). */
    default boolean grantsExperience(FarmBlockEntity farm) {
        return false;
    }

    /** True if this card lets the farm hold a per-recipe output filter (Filter card). */
    default boolean filtersOutputs() {
        return false;
    }

    /** True if this card lets the farm push its outputs to neighbouring containers (Auto-Export card). */
    default boolean exportsOutputs() {
        return false;
    }

    /** True if this card keeps the farm's chunk loaded (Chunk Loader card). */
    default boolean loadsChunk() {
        return false;
    }

    /** Multiplies the energy a Powered farm uses per working tick (Efficiency card; below 1 = cheaper). */
    default double energyUseMultiplier() {
        return 1;
    }

    /** Multiplies the energy capacity (Capacity card). */
    default double energyCapacityMultiplier() {
        return 1;
    }

    /** Multiplies the liquid tank capacity (Capacity card). */
    default double fluidCapacityMultiplier() {
        return 1;
    }

    /** Multiplies the stack size of the output slots (Capacity card). */
    default double itemStackMultiplier() {
        return 1;
    }

    /** Output stack size for items that normally do not stack, at a multiplier of 1x (Capacity card gives 8, so one card = 16). 0 = none. */
    default int unstackableBase() {
        return 0;
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
