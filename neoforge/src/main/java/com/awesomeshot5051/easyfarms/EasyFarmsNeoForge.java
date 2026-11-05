package com.awesomeshot5051.easyfarms;

import com.awesomeshot5051.easyfarms.compat.top.TopCompat;
import com.awesomeshot5051.easyfarms.events.FarmEvents;
import com.awesomeshot5051.easyfarms.platform.NeoForgePlatform;
import com.awesomeshot5051.easyfarms.platform.Services;
import com.awesomeshot5051.easyfarms.recipe.ModRecipes;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.InterModComms;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.event.lifecycle.InterModEnqueueEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.transfer.item.WorldlyContainerWrapper;

@Mod(EasyFarms.MODID)
public class EasyFarmsNeoForge {
    public EasyFarmsNeoForge(IEventBus modBus) {
        EasyFarms.init();
        NeoForgePlatform.bind(modBus);
        modBus.addListener(EasyFarmsNeoForge::registerCapabilities);
        modBus.addListener(EasyFarmsNeoForge::onCommonSetup);
        modBus.addListener(EasyFarmsNeoForge::enqueueCompat);
        NeoForge.EVENT_BUS.addListener(EasyFarmsNeoForge::syncRecipes);
    }

    /** Hoppers and pipes: inputs accept, outputs only give. */
    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.Item.BLOCK, EasyFarms.FARM_BE.get(),
                (farm, side) -> new WorldlyContainerWrapper(farm.automation(), side));
        // Energy in (Powered card) and fluid out (Liquid Farm only, so tank displays stay off the other farms).
        event.registerBlockEntity(Capabilities.Energy.BLOCK, EasyFarms.FARM_BE.get(), (farm, side) -> new FarmEnergyAdapter(farm));
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, EasyFarms.FARM_BE.get(),
                (farm, side) -> farm.kind() == com.awesomeshot5051.easyfarms.farm.FarmKind.LIQUID ? new FarmFluidAdapter(farm) : null);
    }

    private static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(FarmEvents::registerDispenserBehaviors);
    }

    /** The One Probe finds its integrations through an IMC message; only sent when it is installed. */
    private static void enqueueCompat(InterModEnqueueEvent event) {
        if (Services.PLATFORM.isModLoaded("theoneprobe")) {
            InterModComms.sendTo("theoneprobe", "getTheOneProbe", TopCompat::new);
        }
    }

    /** Clients do not receive custom recipes by default; the JEI category and the farm screens need the machine recipes. */
    private static void syncRecipes(OnDatapackSyncEvent event) {
        ModRecipes.TYPES_BY_KIND.values().forEach(h -> event.sendRecipes(h.get()));
    }
}
