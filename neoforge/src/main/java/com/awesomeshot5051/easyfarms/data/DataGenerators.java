package com.awesomeshot5051.easyfarms.data;

import com.awesomeshot5051.easyfarms.EasyFarms;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.data.event.GatherDataRegistryEntriesEvent;

import java.util.List;
import java.util.Set;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Every generated file is derived from the farm definitions, so adding a farm JSON needs no other edits.
 * Models, blockstates and item definitions are plain JSON in src/main/resources/assets.
 */
@EventBusSubscriber(modid = EasyFarms.MODID)
public class DataGenerators {
    @SubscribeEvent
    public static void gatherClientData(GatherDataEvent.Client event) {
        PackOutput output = event.getGenerator().getPackOutput();
        event.addProvider(new ModLanguageProvider(output));
    }

    /** Recipes and loot tables are now bootstrapped into the data pack registries. */
    @SubscribeEvent
    public static void gatherRegistries(GatherDataRegistryEntriesEvent event) {
        Path resourceRecipes = findResourceRecipes();
        event.add(net.minecraft.data.recipes.RecipeProvider.asBootstrap((recipes, advancements) -> new ModRecipeProvider(recipes, advancements, resourceRecipes)));
        event.add(Registries.LOOT_TABLE, new LootTableProvider(Set.of(),
                List.of(new LootTableProvider.SubProviderEntry(ModBlockLoot::new, LootContextParamSets.BLOCK))));
    }

    @SubscribeEvent
    public static void gatherServerData(GatherDataEvent.Server event) {
        PackOutput output = event.getGenerator().getPackOutput();
        var lookup = event.getReloadableLookupProvider();
        event.addProvider(new MachineRecipeValidator(output, lookup));
        event.addProvider(new ModBlockTagsProvider(output, lookup));
    }

    /** The hand-written resource-farm recipes, found by walking up from the run directory to the repository root. */
    private static Path findResourceRecipes() {
        Path dir = Path.of("").toAbsolutePath();
        while (dir != null) {
            Path candidate = dir.resolve("common/src/main/resources/data/easy_farms/recipe/resource_farm");
            if (Files.isDirectory(candidate)) return candidate.normalize();
            dir = dir.getParent();
        }
        return Path.of("common/src/main/resources/data/easy_farms/recipe/resource_farm").toAbsolutePath().normalize();
    }
}
