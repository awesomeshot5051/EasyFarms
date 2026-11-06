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
import net.minecraft.resources.ResourceLocation;
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

    public ModRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
        this.resourceRecipes = output.getOutputFolder().resolve("../../main/resources/data/easy_farms/recipe/resource_farm").normalize();
    }

    @Override
    protected void buildRecipes(RecipeOutput out) {
        SpecialRecipeBuilder.special(HoeTillRecipe::new).save(out, "easy_farms:hoe_till");
        AutoResourceRecipes.generate(out, resourceRecipes);

        // Glass case with the machine's signature item in the middle (pickaxe / hoe / lava bucket) and a base block.
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, EasyFarms.FARM_BLOCKS.get(FarmKind.RESOURCE).get())
                .pattern("###").pattern("#P#").pattern("#I#")
                .define('#', Tags.Items.GLASS_PANES).define('P', ItemTags.PICKAXES).define('I', Items.IRON_BLOCK)
                .unlockedBy("has_iron_block", has(Items.IRON_BLOCK))
                .save(out, id("resource_farm"));
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, EasyFarms.FARM_BLOCKS.get(FarmKind.PLANT).get())
                .pattern("###").pattern("#H#").pattern("#D#")
                .define('#', Tags.Items.GLASS_PANES).define('H', ItemTags.HOES).define('D', Items.DIRT)
                .unlockedBy("has_dirt", has(Items.DIRT))
                .save(out, id("plant_farm"));
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, EasyFarms.FARM_BLOCKS.get(FarmKind.MOB).get())
                .pattern("###").pattern("#L#").pattern("ISI")
                .define('#', Tags.Items.GLASS_PANES).define('L', Items.LAVA_BUCKET).define('I', Items.IRON_INGOT).define('S', ItemTags.SWORDS)
                .unlockedBy("has_lava_bucket", has(Items.LAVA_BUCKET))
                .save(out, id("mob_farm"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, EasyFarms.FARM_BLOCKS.get(FarmKind.LIQUID).get())
                .pattern("#B#").pattern("#D#").pattern("#C#")
                .define('#', Tags.Items.GLASS_PANES).define('B', Items.BUCKET).define('D', Items.POINTED_DRIPSTONE).define('C', Items.CAULDRON)
                .unlockedBy("has_cauldron", has(Items.CAULDRON))
                .save(out, id("liquid_farm"));
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.POWERED_UPGRADE.get())
                .pattern("CRC").pattern("RBR").pattern("CRC")
                .define('C', Items.COPPER_INGOT).define('R', Items.REDSTONE).define('B', Items.REDSTONE_BLOCK)
                .unlockedBy("has_redstone_block", has(Items.REDSTONE_BLOCK))
                .save(out, id("powered_upgrade"));
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.CAPACITY_UPGRADE.get())
                .pattern("III").pattern("ICI").pattern("III")
                .define('I', Items.IRON_INGOT).define('C', Items.CHEST)
                .unlockedBy("has_chest", has(Items.CHEST))
                .save(out, id("capacity_upgrade"));
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.EFFICIENCY_UPGRADE.get())
                .pattern("GRG").pattern("RPR").pattern("GRG")
                .define('G', Items.GOLD_INGOT).define('R', Items.REDSTONE).define('P', ModItems.POWERED_UPGRADE.get())
                .unlockedBy("has_powered_upgrade", has(ModItems.POWERED_UPGRADE.get()))
                .save(out, id("efficiency_upgrade"));
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.CHUNK_LOADER_UPGRADE.get())
                .pattern("OEO").pattern("EDE").pattern("OEO")
                .define('O', Items.OBSIDIAN).define('E', Items.ENDER_PEARL).define('D', Items.ENDER_EYE)
                .unlockedBy("has_ender_eye", has(Items.ENDER_EYE))
                .save(out, id("chunk_loader_upgrade"));
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.VOID_UPGRADE.get())
                .pattern("OPO").pattern("PDP").pattern("OPO")
                .define('O', Items.OBSIDIAN).define('P', Items.ENDER_PEARL).define('D', Items.DIAMOND)
                .unlockedBy("has_diamond", has(Items.DIAMOND))
                .save(out, id("void_upgrade"));
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.FILTER_UPGRADE.get())
                .pattern("GCG").pattern("CHC").pattern("GCG")
                .define('G', Items.GOLD_INGOT).define('C', Items.COMPARATOR).define('H', Items.HOPPER)
                .unlockedBy("has_hopper", has(Items.HOPPER))
                .save(out, id("filter_upgrade"));
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.AUTO_EXPORT_UPGRADE.get())
                .pattern("RHR").pattern("HPH").pattern("RHR")
                .define('R', Items.REDSTONE).define('H', Items.HOPPER).define('P', Items.PISTON)
                .unlockedBy("has_hopper", has(Items.HOPPER))
                .save(out, id("auto_export_upgrade"));
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.EXPERIENCE_UPGRADE.get())
                .pattern("LBL").pattern("BEB").pattern("LBL")
                .define('L', Items.LAPIS_LAZULI).define('B', Items.GLASS_BOTTLE).define('E', Items.EMERALD)
                .unlockedBy("has_emerald", has(Items.EMERALD))
                .save(out, id("experience_upgrade"));
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.MEMORY_CARD.get())
                .pattern("NPN").pattern("PRP").pattern("NPN")
                .define('N', Items.IRON_NUGGET).define('P', Items.PAPER).define('R', Items.REDSTONE)
                .unlockedBy("has_paper", has(Items.PAPER))
                .save(out, id("memory_card"));

        // Cards (same recipes as the old mods).
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.SPEED_UPGRADE.get())
                .pattern("RRR").pattern("RFR").pattern("RNR")
                .define('R', Items.REDSTONE).define('F', Items.FLINT_AND_STEEL).define('N', Items.QUARTZ)
                .unlockedBy("has_flint_and_steel", has(Items.FLINT_AND_STEEL))
                .save(out, id("speed_upgrade"));
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.SMELTER_UPGRADE.get())
                .pattern("IBI").pattern("IFI").pattern("INI")
                .define('I', Items.IRON_INGOT).define('F', Items.FLINT_AND_STEEL).define('N', Items.NETHERRACK)
                .define('B', Ingredient.of(Items.BLAZE_POWDER, Items.BLAZE_ROD))
                .unlockedBy("has_netherrack", has(Items.NETHERRACK))
                .save(out, id("smelter_upgrade"));
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.REDSTONE_UPGRADE.get())
                .pattern("RRR").pattern("OLO").pattern("RRR")
                .define('R', Items.REDSTONE).define('O', Items.OBSERVER).define('L', Items.LEVER)
                .unlockedBy("has_observer", has(Items.OBSERVER))
                .save(out, id("redstone_upgrade"));
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.NETHER_UPGRADE.get())
                .pattern("QQQ").pattern("NNN").pattern("GGG")
                .define('Q', Items.QUARTZ).define('N', Items.NETHERRACK).define('G', Items.GLOWSTONE)
                .unlockedBy("has_netherrack", has(Items.NETHERRACK))
                .save(out, id("nether_upgrade"));
        // Infinity: a nether star in the middle, four eyes of ender and four diamonds. All vanilla, no cards needed.
        // Mid-to-late game: you need a Wither kill (or the Wither farm) plus an End-capable amount of blaze powder and pearls,
        // but nothing that is a one-time boss drop only.
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.INFINITY_UPGRADE.get())
                .pattern("DED").pattern("ENE").pattern("DED")
                .define('D', Items.DIAMOND).define('E', Items.ENDER_EYE).define('N', Items.NETHER_STAR)
                .unlockedBy("has_nether_star", has(Items.NETHER_STAR))
                .save(out, id("infinity_upgrade"));
    }

    private static ResourceLocation mc(String path) {
        return ResourceLocation.withDefaultNamespace(path);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(EasyFarms.MODID, path);
    }
}
