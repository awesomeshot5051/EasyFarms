package com.awesomeshot5051.easyfarms;

import com.awesomeshot5051.easyfarms.events.FarmEvents;
import com.awesomeshot5051.easyfarms.farm.FarmKind;
import com.awesomeshot5051.easyfarms.recipe.ModRecipes;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.recipe.v1.sync.RecipeSynchronization;
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

        // Cards and dyes work on a farm while sneaking: vanilla skips a block's use when you sneak with an item in hand.
        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            if (player.isSpectator() || !player.isSecondaryUseActive()) return InteractionResult.PASS;
            ItemStack held = player.getItemInHand(hand);
            if (!FarmEvents.allowsSneakUse(level, hit.getBlockPos(), held)) return InteractionResult.PASS;
            BlockState state = level.getBlockState(hit.getBlockPos());
            InteractionResult result = state.useItemOn(held, level, player, hand, hit);
            return result.consumesAction() ? result : InteractionResult.PASS;
        });

        FarmEvents.registerDispenserBehaviors();
    }
}
