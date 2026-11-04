package com.awesomeshot5051.easyfarms.data;

import com.awesomeshot5051.easyfarms.EasyFarms;
import com.awesomeshot5051.easyfarms.farm.FarmKind;
import com.awesomeshot5051.easyfarms.recipe.HoeTillRecipe;
import com.awesomeshot5051.easyfarms.recipe.ModRecipes;
import com.awesomeshot5051.easyfarms.registry.ModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.data.recipes.SpecialRecipeBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.Tags;
import com.awesomeshot5051.easyfarms.recipe.SizedIngredient;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Crafting recipes for the three farm blocks and the upgrade cards. What a farm does is NOT here: that is the
 * machine recipes under data/easy_farms/recipe/{resource,plant,mob}_farm/.
 */
public class ModRecipeProvider extends RecipeProvider {
    private final java.nio.file.Path resourceRecipes;

    public ModRecipeProvider(HolderLookup.Provider registries, RecipeOutput output, java.nio.file.Path resourceRecipes) {
        super(registries, output);
        this.resourceRecipes = resourceRecipes;
    }

    /** The data provider the generator runs; it builds a {@link ModRecipeProvider} with the right output. */
    public static class Runner extends RecipeProvider.Runner {
        private final java.nio.file.Path resourceRecipes;

        public Runner(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
            super(output, registries);
            this.resourceRecipes = output.getOutputFolder().resolve("../../main/resources/data/easy_farms/recipe/resource_farm").normalize();
        }

        @Override
        protected RecipeProvider createRecipeProvider(HolderLookup.Provider registries, RecipeOutput output) {
            return new ModRecipeProvider(registries, output, resourceRecipes);
        }

        @Override
        public String getName() {
            return "Easy Farms recipes";
        }
    }

    @Override
    protected void buildRecipes() {
        RecipeOutput out = this.output;
        SpecialRecipeBuilder.special(HoeTillRecipe::new).save(out, id("hoe_till"));
        AutoResourceRecipes.generate(out, resourceRecipes);

        // Glass case with the machine's signature item in the middle (pickaxe / hoe / lava bucket) and a base block.
        ShapedRecipeBuilder.shaped(items, RecipeCategory.MISC, EasyFarms.FARM_BLOCKS.get(FarmKind.RESOURCE).get())
                .pattern("###").pattern("#P#").pattern("#I#")
                .define('#', Tags.Items.GLASS_PANES).define('P', ItemTags.PICKAXES).define('I', Items.IRON_BLOCK)
                .unlockedBy("has_iron_block", has(Items.IRON_BLOCK))
                .save(out, id("resource_farm"));
        ShapedRecipeBuilder.shaped(items, RecipeCategory.MISC, EasyFarms.FARM_BLOCKS.get(FarmKind.PLANT).get())
                .pattern("###").pattern("#H#").pattern("#D#")
                .define('#', Tags.Items.GLASS_PANES).define('H', ItemTags.HOES).define('D', Items.DIRT)
                .unlockedBy("has_dirt", has(Items.DIRT))
                .save(out, id("plant_farm"));
        ShapedRecipeBuilder.shaped(items, RecipeCategory.MISC, EasyFarms.FARM_BLOCKS.get(FarmKind.MOB).get())
                .pattern("###").pattern("#L#").pattern("ISI")
                .define('#', Tags.Items.GLASS_PANES).define('L', Items.LAVA_BUCKET).define('I', Items.IRON_INGOT).define('S', ItemTags.SWORDS)
                .unlockedBy("has_lava_bucket", has(Items.LAVA_BUCKET))
                .save(out, id("mob_farm"));

        // Cards (same recipes as the old mods).
        ShapedRecipeBuilder.shaped(items, RecipeCategory.MISC, ModItems.SPEED_UPGRADE.get())
                .pattern("RRR").pattern("RFR").pattern("RNR")
                .define('R', Items.REDSTONE).define('F', Items.FLINT_AND_STEEL).define('N', Items.QUARTZ)
                .unlockedBy("has_flint_and_steel", has(Items.FLINT_AND_STEEL))
                .save(out, id("speed_upgrade"));
        ShapedRecipeBuilder.shaped(items, RecipeCategory.MISC, ModItems.SMELTER_UPGRADE.get())
                .pattern("IBI").pattern("IFI").pattern("INI")
                .define('I', Items.IRON_INGOT).define('F', Items.FLINT_AND_STEEL).define('N', Items.NETHERRACK)
                .define('B', Ingredient.of(Items.BLAZE_POWDER, Items.BLAZE_ROD))
                .unlockedBy("has_netherrack", has(Items.NETHERRACK))
                .save(out, id("smelter_upgrade"));
        ShapedRecipeBuilder.shaped(items, RecipeCategory.MISC, ModItems.REDSTONE_UPGRADE.get())
                .pattern("RRR").pattern("OLO").pattern("RRR")
                .define('R', Items.REDSTONE).define('O', Items.OBSERVER).define('L', Items.LEVER)
                .unlockedBy("has_observer", has(Items.OBSERVER))
                .save(out, id("redstone_upgrade"));
        ShapedRecipeBuilder.shaped(items, RecipeCategory.MISC, ModItems.NETHER_UPGRADE.get())
                .pattern("QQQ").pattern("NNN").pattern("GGG")
                .define('Q', Items.QUARTZ).define('N', Items.NETHERRACK).define('G', Items.GLOWSTONE)
                .unlockedBy("has_netherrack", has(Items.NETHERRACK))
                .save(out, id("nether_upgrade"));
        // Infinity: a nether star in the middle, four eyes of ender and four diamonds. All vanilla, no cards needed.
        // Mid-to-late game: you need a Wither kill (or the Wither farm) plus an End-capable amount of blaze powder and pearls,
        // but nothing that is a one-time boss drop only.
        ShapedRecipeBuilder.shaped(items, RecipeCategory.MISC, ModItems.INFINITY_UPGRADE.get())
                .pattern("DED").pattern("ENE").pattern("DED")
                .define('D', Items.DIAMOND).define('E', Items.ENDER_EYE).define('N', Items.NETHER_STAR)
                .unlockedBy("has_nether_star", has(Items.NETHER_STAR))
                .save(out, id("infinity_upgrade"));
    }

    private static Identifier mc(String path) {
        return Identifier.withDefaultNamespace(path);
    }

    private static net.minecraft.resources.ResourceKey<net.minecraft.world.item.crafting.Recipe<?>> id(String path) {
        return net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE, Identifier.fromNamespaceAndPath(EasyFarms.MODID, path));
    }
}
