package com.awesomeshot5051.easyfarms;

import com.awesomeshot5051.easyfarms.client.CardTooltips;
import com.awesomeshot5051.easyfarms.client.ClientCommon;
import com.awesomeshot5051.easyfarms.client.FarmItemRenderer;
import com.awesomeshot5051.easyfarms.client.FarmRenderer;
import com.awesomeshot5051.easyfarms.client.FarmScreen;
import com.awesomeshot5051.easyfarms.client.FarmCatalystScreen;
import com.awesomeshot5051.easyfarms.client.FarmUpgradeScreen;
import com.awesomeshot5051.easyfarms.client.SecretSync;
import com.awesomeshot5051.easyfarms.gui.ModMenus;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.world.level.block.Block;

import java.util.function.Supplier;

/** Client side. The settings screen comes from the Configured mod / Mod Menu (see compat.configured). */
@Environment(EnvType.CLIENT)
public class EasyFarmsFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientCommon.init();
        com.awesomeshot5051.easyfarms.platform.FabricPlatform.registerClientReceivers();
        BlockEntityRenderers.register(EasyFarms.FARM_BE.get(), FarmRenderer::new);
        MenuScreens.register(ModMenus.FARM.get(), FarmScreen::new);
        MenuScreens.register(ModMenus.FARM_UPGRADES.get(), FarmUpgradeScreen::new);
        MenuScreens.register(ModMenus.FARM_CATALYSTS.get(), FarmCatalystScreen::new);
        MenuScreens.register(ModMenus.FARM_FILTER.get(), com.awesomeshot5051.easyfarms.client.FarmFilterScreen::new);
        MenuScreens.register(ModMenus.FARM_CRAFTING.get(), com.awesomeshot5051.easyfarms.client.FarmCraftingScreen::new);

        Block[] farms = EasyFarms.FARM_BLOCKS.values().stream().map(Supplier::get).toArray(Block[]::new);
        // The farm model's "render_type": "cutout" is a NeoForge model feature; Fabric registers it per block.
        BlockRenderLayerMap.INSTANCE.putBlocks(RenderType.cutout(), farms);
        ColorProviderRegistry.BLOCK.register(ClientCommon::farmTint, farms);

        for (Block farm : farms) {
            BuiltinItemRendererRegistry.INSTANCE.register(farm.asItem(),
                    (stack, context, pose, buffer, light, overlay) -> FarmItemRenderer.render(stack, pose, buffer, light, overlay));
        }

        ItemTooltipCallback.EVENT.register((stack, context, type, lines) -> CardTooltips.addTo(stack, lines));
        ClientTickEvents.END_CLIENT_TICK.register(client -> SecretSync.onClientTick());
    }
}
