package com.awesomeshot5051.easyfarms;

import com.awesomeshot5051.easyfarms.client.ClientCommon;
import com.awesomeshot5051.easyfarms.client.ConfigScreen;
import com.awesomeshot5051.easyfarms.client.FarmRenderer;
import com.awesomeshot5051.easyfarms.client.FarmScreen;
import com.awesomeshot5051.easyfarms.client.FarmCatalystScreen;
import com.awesomeshot5051.easyfarms.client.FarmUpgradeScreen;
import com.awesomeshot5051.easyfarms.gui.ModMenus;
import net.minecraft.world.level.block.Block;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

import java.util.List;

// This class will not load on dedicated servers. Accessing client side code from here is safe.
@Mod(value = EasyFarms.MODID, dist = Dist.CLIENT)
public class EasyFarmsNeoForgeClient {
    public EasyFarmsNeoForgeClient(IEventBus modBus, ModContainer container) {
        ClientCommon.init();
        container.registerExtensionPoint(IConfigScreenFactory.class, (mod, parent) -> new ConfigScreen(parent));
        modBus.addListener(EasyFarmsNeoForgeClient::registerRenderers);
        modBus.addListener(EasyFarmsNeoForgeClient::registerScreens);
        modBus.addListener(EasyFarmsNeoForgeClient::registerColors);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(EasyFarms.FARM_BE.get(), FarmRenderer::new);
    }

    private static void registerColors(RegisterColorHandlersEvent.BlockTintSources event) {
        event.register(List.of(ClientCommon.FLOOR_TINT), EasyFarms.FARM_BLOCKS.values().stream().map(b -> (Block) b.get()).toArray(Block[]::new));
    }

    private static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.FARM.get(), FarmScreen::new);
        event.register(ModMenus.FARM_UPGRADES.get(), FarmUpgradeScreen::new);
        event.register(ModMenus.FARM_CATALYSTS.get(), FarmCatalystScreen::new);
        event.register(ModMenus.FARM_FILTER.get(), com.awesomeshot5051.easyfarms.client.FarmFilterScreen::new);
    }
}
