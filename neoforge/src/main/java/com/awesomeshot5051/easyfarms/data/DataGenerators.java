package com.awesomeshot5051.easyfarms.data;

import com.awesomeshot5051.easyfarms.EasyFarms;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/** Every generated file is derived from the farm definitions, so adding a farm JSON needs no other edits. */
@EventBusSubscriber(modid = EasyFarms.MODID)
public class DataGenerators {
    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        DataGenerator gen = event.getGenerator();
        PackOutput output = gen.getPackOutput();
        ExistingFileHelper helper = event.getExistingFileHelper();
        CompletableFuture<HolderLookup.Provider> lookup = event.getLookupProvider();

        gen.addProvider(event.includeClient(), new ModBlockStateProvider(output, helper));
        gen.addProvider(event.includeClient(), new ModItemModelProvider(output, helper));
        gen.addProvider(event.includeClient(), new ModLanguageProvider(output));
        gen.addProvider(event.includeServer(), new ModRecipeProvider(output, lookup));
        gen.addProvider(event.includeServer(), new LootTableProvider(output, Set.of(),
                List.of(new LootTableProvider.SubProviderEntry(ModBlockLoot::new, LootContextParamSets.BLOCK)), lookup));
        gen.addProvider(event.includeServer(), new MachineRecipeValidator(output));
        gen.addProvider(event.includeServer(), new ModBlockTagsProvider(output, lookup, helper));
    }
}
