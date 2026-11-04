package com.awesomeshot5051.easyfarms.data;

import com.awesomeshot5051.easyfarms.EasyFarms;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.List;
import java.util.Set;

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

    @SubscribeEvent
    public static void gatherServerData(GatherDataEvent.Server event) {
        PackOutput output = event.getGenerator().getPackOutput();
        var lookup = event.getLookupProvider();
        event.addProvider(new ModRecipeProvider.Runner(output, lookup));
        event.addProvider(new LootTableProvider(output, Set.of(),
                List.of(new LootTableProvider.SubProviderEntry(ModBlockLoot::new, LootContextParamSets.BLOCK)), lookup));
        event.addProvider(new MachineRecipeValidator(output, lookup));
        event.addProvider(new ModBlockTagsProvider(output, lookup));
    }
}
