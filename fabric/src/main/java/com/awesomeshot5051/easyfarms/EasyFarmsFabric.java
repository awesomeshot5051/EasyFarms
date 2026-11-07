package com.awesomeshot5051.easyfarms;

import com.awesomeshot5051.easyfarms.events.FarmEvents;
import com.awesomeshot5051.easyfarms.farm.FarmKind;
import com.awesomeshot5051.easyfarms.recipe.ModRecipes;
import com.awesomeshot5051.easyfarms.platform.Services;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.recipe.v1.sync.RecipeSynchronization;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ContainerStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public class EasyFarmsFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        EasyFarms.init();

        // Hoppers and pipes: inputs accept, outputs only give.
        ItemStorage.SIDED.registerForBlockEntity((farm, side) -> ContainerStorage.of(farm.automation(), side), EasyFarms.FARM_BE.get());

        // Clients do not receive custom recipes by default; the JEI category and the farm screens need the machine recipes.
        for (FarmKind kind : FarmKind.values()) {
            RecipeSynchronization.synchronizeRecipeSerializer(ModRecipes.SERIALIZERS_BY_KIND.get(kind).get());
        }

        // Liquid Farm tank: pipes and tanks can take fluid out.
        FluidStorage.SIDED.registerForBlockEntity((farm, side) -> farm.kind() == com.awesomeshot5051.easyfarms.farm.FarmKind.LIQUID ? new FarmFluidStorage(farm) : null, EasyFarms.FARM_BE.get());
        // Powered card: Team Reborn Energy (the standard Fabric energy API) is optional.
        if (Services.PLATFORM.isModLoaded("team_reborn_energy")) FarmEnergyStorage.register();

        // Cards and dyes work on a farm while sneaking: vanilla skips a block's use when you sneak with an item in hand.
        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            if (player.isSpectator() || !player.isSecondaryUseActive()) return InteractionResult.PASS;
            ItemStack held = player.getItemInHand(hand);
            if (!FarmEvents.allowsSneakUse(level, hit.getBlockPos(), held)) return InteractionResult.PASS;
            BlockState state = level.getBlockState(hit.getBlockPos());
            InteractionResult result = state.useItemOn(held, level, player, hand, hit);
            return result.consumesAction() ? result : InteractionResult.PASS;
        });

        // JEI shows what a loot-table drop can give: the server rolls the tables and sends the result (after a reload and on join).
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SYNC_DATA_PACK_CONTENTS.register((player, joined) ->
                com.awesomeshot5051.easyfarms.farm.LootPreview.send(player.level().getServer(), player));

        // Auto Importer cards: the Refined Storage code is only touched when that mod is installed.
        if (Services.PLATFORM.isModLoaded("refinedstorage")) {
            com.awesomeshot5051.easyfarms.farm.network.NetworkImporters.register("refinedstorage",
                    new com.awesomeshot5051.easyfarms.compat.refinedstorage.RefinedStorageImporter());
        }

        FarmEvents.registerDispenserBehaviors();
    }
}
