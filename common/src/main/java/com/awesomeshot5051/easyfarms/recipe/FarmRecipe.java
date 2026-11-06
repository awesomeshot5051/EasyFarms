package com.awesomeshot5051.easyfarms.recipe;

import com.awesomeshot5051.easyfarms.farm.DropEntry;
import com.awesomeshot5051.easyfarms.farm.FarmKind;
import com.awesomeshot5051.easyfarms.tier.FarmTier;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import com.awesomeshot5051.easyfarms.recipe.SizedIngredient;

import java.util.List;
import java.util.Optional;

/**
 * A machine recipe: what a Resource / Plant / Mob farm does with the items in its GUI.
 *
 * @param inputs      one to three ingredients with counts; any slot order, except Plant farms (soil first)
 * @param tool        item tag the tool must be in; absent = the farm needs no tool (speed upgrade then applies)
 * @param minTierRank the tool's tier must have at least this rank
 * @param visual      what the block shows while working
 * @param baseTicks   cycle time before tool tier, in ticks
 * @param visualTicks how long at the end of a cycle the mob / plant is visible
 * @param smeltable   the smelter upgrade applies
 * @param nether      the Nether upgrade lets this recipe run anywhere, replacing its environment requirement
 * @param environment where the farm must be (dimension, block under it) when it has no Nether upgrade
 * @param inscriber   what the Inscriber card changes: an optional press that must be installed, and the drops that replace the normal ones
 * @param secret      hidden from recipe viewers (JEI, EMI); the Wither farm is one
 * @param xp          experience dropped per cycle (0 = none; Mob farms only give it with the Experience card)
 * @param fluidInput  fluid taken from the farm's tank each cycle (the farm needs it, e.g. from a Liquid Farm through a pipe)
 * @param dyeable     the farm can be dyed (by hand or dispenser); drops and visual whose id starts with "white_" take the dye colour
 */
public record FarmRecipe(FarmKind kind, List<SizedIngredient> inputs, Optional<TagKey<Item>> tool, int minTierRank,
                         Visual visual, int baseTicks, int visualTicks, boolean smeltable, boolean nether,
                         Optional<Inscriber> inscriber, boolean dyeable, boolean secret, Optional<Environment> environment,
                         List<DropEntry> drops, Optional<FluidOutput> fluid, float xp, Optional<FluidInput> fluidInput) implements Recipe<FarmRecipeInput> {

    /**
     * AE2-style inscribing: with the Inscriber card (and the press in the Upgrades screen, when one is named) the drops
     * below replace the recipe's normal drops.
     *
     * @param press an item that must sit in the farm's press slot, or empty when the recipe needs none
     */
    public record Inscriber(Optional<Identifier> press, List<DropEntry> drops) {
        public static final Codec<Inscriber> CODEC = RecordCodecBuilder.create(i -> i.group(
                Identifier.CODEC.optionalFieldOf("press").forGetter(Inscriber::press),
                DropEntry.CODEC.listOf().fieldOf("drops").forGetter(Inscriber::drops)
        ).apply(i, Inscriber::new));
    }

    /**
     * Where a farm must stand to run this recipe.
     *
     * @param dimension  required dimension, e.g. minecraft:the_nether (absent = any)
     * @param blockBelow the block directly under the farm must be one of these: ids, or "#namespace:tag" (empty = anything)
     */
    public record Environment(Optional<Identifier> dimension, List<String> blockBelow) {
        public static final Codec<Environment> CODEC = RecordCodecBuilder.create(i -> i.group(
                Identifier.CODEC.optionalFieldOf("dimension").forGetter(Environment::dimension),
                Codec.STRING.listOf().optionalFieldOf("block_below", List.of()).forGetter(Environment::blockBelow)
        ).apply(i, Environment::new));

        /** True if the farm at pos in this level meets the requirement. */
        public boolean satisfiedAt(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos) {
            if (dimension.isPresent() && !level.dimension().identifier().equals(dimension.get())) return false;
            if (blockBelow.isEmpty()) return true;
            var below = level.getBlockState(pos.below());
            for (String entry : blockBelow) {
                if (entry.startsWith("#")) {
                    var tag = net.minecraft.tags.TagKey.create(Registries.BLOCK, Identifier.parse(entry.substring(1)));
                    if (below.is(tag)) return true;
                } else if (net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(below.getBlock()).equals(Identifier.parse(entry))) return true;
            }
            return false;
        }
    }

    /**
     * What a Liquid Farm makes each cycle instead of items.
     *
     * @param fluid  fluid id, e.g. minecraft:water
     * @param amount millibuckets added to the tank per cycle
     */
    public record FluidOutput(Identifier fluid, int amount) {
        public static final Codec<FluidOutput> CODEC = RecordCodecBuilder.create(i -> i.group(
                Identifier.CODEC.fieldOf("fluid").forGetter(FluidOutput::fluid),
                Codec.intRange(1, 1000000).optionalFieldOf("amount", 1).forGetter(FluidOutput::amount)
        ).apply(i, FluidOutput::new));
    }

    /**
     * A fluid a recipe uses up from the farm's tank every cycle.
     *
     * @param fluid  fluid id, e.g. minecraft:water
     * @param amount millibuckets used per cycle
     */
    public record FluidInput(Identifier fluid, int amount) {
        public static final Codec<FluidInput> CODEC = RecordCodecBuilder.create(i -> i.group(
                Identifier.CODEC.fieldOf("fluid").forGetter(FluidInput::fluid),
                Codec.intRange(1, 1000000).optionalFieldOf("amount", 1).forGetter(FluidInput::amount)
        ).apply(i, FluidInput::new));
    }

    /** What the block shows: "block" (ore/soil with a swinging tool), "plant" (growing), or "mob". */
    public record Visual(String type, Identifier target) {
        public static final Codec<Visual> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("type").forGetter(Visual::type),
                Identifier.CODEC.fieldOf("target").forGetter(Visual::target)
        ).apply(i, Visual::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, Visual> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, Visual::type,
                Identifier.STREAM_CODEC, Visual::target,
                Visual::new);
    }

    /** A copy with a different drop table (recipe viewers show what an upgrade card turns the drops into). */
    public FarmRecipe withDrops(List<DropEntry> newDrops) {
        return new FarmRecipe(kind, inputs, tool, minTierRank, visual, baseTicks, visualTicks, smeltable, nether,
                inscriber, dyeable, secret, environment, newDrops, fluid, xp, fluidInput);
    }

    public boolean toolless() {
        return tool.isEmpty();
    }

    /**
     * Which input slot feeds which recipe ingredient (result[i] = slot for ingredient i), or null when the slots do not
     * satisfy the recipe. Each ingredient also checks the stack size, so "4 soul sand" needs 4 in one slot.
     * Plant farms need the slot order to match the recipe (soil first); other farms accept any order.
     */
    public int[] assignInputs(List<ItemStack> slots) {
        int n = inputs.size();
        if (n > slots.size()) return null;
        int[] result = new int[n];
        if (kind == FarmKind.PLANT) {
            for (int i = 0; i < n; i++) {
                if (!inputs.get(i).test(slots.get(i))) return null;
                result[i] = i;
            }
            return result;
        }
        return assign(0, slots, new boolean[slots.size()], result) ? result : null;
    }

    private boolean assign(int ingredient, List<ItemStack> slots, boolean[] used, int[] result) {
        if (ingredient == inputs.size()) return true;
        for (int slot = 0; slot < slots.size(); slot++) {
            if (used[slot] || !inputs.get(ingredient).test(slots.get(slot))) continue;
            used[slot] = true;
            result[ingredient] = slot;
            if (assign(ingredient + 1, slots, used, result)) return true;
            used[slot] = false;
        }
        return false;
    }

    /**
     * Exact match on the input slots (the tool is not looked at): every filled slot must belong to this recipe. Otherwise a
     * small recipe (wither skeleton: skulls) would also match a bigger one's items (wither: soul sand + nether stars +
     * skulls) and win by registration order.
     */
    public boolean matchesInputs(List<ItemStack> slots) {
        if (assignInputs(slots) == null) return false;
        int filled = 0;
        for (ItemStack s : slots) if (!s.isEmpty()) filled++;
        return filled == inputs.size();
    }

    @Override
    public boolean matches(FarmRecipeInput input, Level level) {
        if (!matchesInputs(input.inputs())) return false;
        if (tool.isEmpty()) return true;
        ItemStack stack = input.tool();
        if (!stack.is(tool.get())) return false;
        FarmTier tier = FarmTier.find(level.registryAccess(), stack);
        return tier != null && tier.rank() >= minTierRank;
    }

    @Override
    public ItemStack assemble(FarmRecipeInput input) {
        return ItemStack.EMPTY;
    }

    /** Machine recipes are not crafting-table recipes: keep them out of the recipe book. */
    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public boolean showNotification() {
        return false;
    }

    @Override
    public String group() {
        return "";
    }

    @Override
    public PlacementInfo placementInfo() {
        return PlacementInfo.NOT_PLACEABLE;
    }

    @Override
    public RecipeBookCategory recipeBookCategory() {
        return RecipeBookCategories.CRAFTING_MISC;
    }

    @Override
    public RecipeSerializer<FarmRecipe> getSerializer() {
        return ModRecipes.SERIALIZERS_BY_KIND.get(kind).get();
    }

    @Override
    public RecipeType<FarmRecipe> getType() {
        return ModRecipes.TYPES_BY_KIND.get(kind).get();
    }

    /** Builds the serializer for one farm kind. */
    public static RecipeSerializer<FarmRecipe> serializer(FarmKind kind) {
        MapCodec<FarmRecipe> codec = RecordCodecBuilder.mapCodec(i -> i.group(
                SizedIngredient.NESTED_CODEC.listOf(1, 3).fieldOf("inputs").forGetter(FarmRecipe::inputs),
                TagKey.codec(Registries.ITEM).optionalFieldOf("tool").forGetter(FarmRecipe::tool),
                Codec.INT.optionalFieldOf("min_tier_rank", 0).forGetter(FarmRecipe::minTierRank),
                Visual.CODEC.fieldOf("visual").forGetter(FarmRecipe::visual),
                Codec.INT.fieldOf("base_ticks").forGetter(FarmRecipe::baseTicks),
                Codec.INT.optionalFieldOf("visual_ticks", 80).forGetter(FarmRecipe::visualTicks),
                Codec.BOOL.optionalFieldOf("smeltable", false).forGetter(FarmRecipe::smeltable),
                Codec.BOOL.optionalFieldOf("nether", false).forGetter(FarmRecipe::nether),
                Inscriber.CODEC.optionalFieldOf("inscriber").forGetter(FarmRecipe::inscriber),
                Codec.BOOL.optionalFieldOf("dyeable", false).forGetter(FarmRecipe::dyeable),
                Codec.BOOL.optionalFieldOf("secret", false).forGetter(FarmRecipe::secret),
                Environment.CODEC.optionalFieldOf("environment").forGetter(FarmRecipe::environment),
                DropEntry.CODEC.listOf().optionalFieldOf("drops", List.of()).forGetter(FarmRecipe::drops),
                FluidOutput.CODEC.optionalFieldOf("fluid").forGetter(FarmRecipe::fluid),
                Codec.FLOAT.optionalFieldOf("xp", 0f).forGetter(FarmRecipe::xp),
                FluidInput.CODEC.optionalFieldOf("fluid_input").forGetter(FarmRecipe::fluidInput)
        ).apply(i, (inputs, tool, rank, visual, base, vis, smelt, nether, ins, dye, secret, env, drops, fluid, xp, fluidIn) ->
                new FarmRecipe(kind, inputs, tool, rank, visual, base, vis, smelt, nether, ins, dye, secret, env, drops, fluid, xp, fluidIn)));
        // The network format is just the JSON codec: recipes are small and sync once per join/reload.
        return new RecipeSerializer<>(codec, ByteBufCodecs.fromCodecWithRegistries(codec.codec()));
    }
}
