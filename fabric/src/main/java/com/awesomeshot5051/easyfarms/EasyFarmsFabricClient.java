package com.awesomeshot5051.easyfarms;

import com.awesomeshot5051.easyfarms.client.CardTooltips;
import com.awesomeshot5051.easyfarms.client.ClientCommon;
import com.awesomeshot5051.easyfarms.client.ClientRecipes;
import com.awesomeshot5051.easyfarms.client.FarmRenderer;
import com.awesomeshot5051.easyfarms.client.FarmScreen;
import com.awesomeshot5051.easyfarms.client.FarmCatalystScreen;
import com.awesomeshot5051.easyfarms.client.FarmUpgradeScreen;
import com.awesomeshot5051.easyfarms.client.SecretSync;
import com.awesomeshot5051.easyfarms.gui.ModMenus;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.recipe.v1.sync.ClientRecipeSynchronizedEvent;
import net.fabricmc.fabric.api.client.rendering.v1.BlockColorRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.world.level.block.Block;

import java.util.List;
import java.util.function.Supplier;

/** Client side. The settings screen is shown through Mod Menu (see compat.modmenu). */
@Environment(EnvType.CLIENT)
public class EasyFarmsFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientCommon.init();
        com.awesomeshot5051.easyfarms.platform.FabricPlatform.registerClientReceivers();
        BlockEntityRendererRegistry.register(EasyFarms.FARM_BE.get(), FarmRenderer::new);
        MenuScreens.register(ModMenus.FARM.get(), FarmScreen::new);
        MenuScreens.register(ModMenus.FARM_UPGRADES.get(), FarmUpgradeScreen::new);
        MenuScreens.register(ModMenus.FARM_CATALYSTS.get(), FarmCatalystScreen::new);
        MenuScreens.register(ModMenus.FARM_FILTER.get(), com.awesomeshot5051.easyfarms.client.FarmFilterScreen::new);
        MenuScreens.register(ModMenus.FARM_CRAFTING.get(), com.awesomeshot5051.easyfarms.client.FarmCraftingScreen::new);

        Block[] farms = EasyFarms.FARM_BLOCKS.values().stream().map(Supplier::get).toArray(Block[]::new);
        BlockColorRegistry.register(List.of(ClientCommon.FLOOR_TINT), farms);

        ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> CardTooltips.addTo(stack, lines));
        ClientTickEvents.END_CLIENT_TICK.register(client -> SecretSync.onClientTick());
        ClientRecipeSynchronizedEvent.EVENT.register((client, recipes) -> ClientRecipes.set(recipes.recipes()));
    }
}
