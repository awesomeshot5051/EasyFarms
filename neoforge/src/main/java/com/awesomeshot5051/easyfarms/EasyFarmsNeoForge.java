package com.awesomeshot5051.easyfarms;

import com.awesomeshot5051.easyfarms.compat.top.TopCompat;
import com.awesomeshot5051.easyfarms.events.FarmEvents;
import com.awesomeshot5051.easyfarms.platform.NeoForgePlatform;
import com.awesomeshot5051.easyfarms.platform.Services;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.InterModComms;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.event.lifecycle.InterModEnqueueEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.items.wrapper.SidedInvWrapper;

@Mod(EasyFarms.MODID)
public class EasyFarmsNeoForge {
    public EasyFarmsNeoForge(IEventBus modBus) {
        EasyFarms.init();
        NeoForgePlatform.bind(modBus);
        modBus.addListener(EasyFarmsNeoForge::registerCapabilities);
        modBus.addListener(EasyFarmsNeoForge::onCommonSetup);
        modBus.addListener(EasyFarmsNeoForge::enqueueCompat);
    }

    /** Hoppers and pipes: inputs accept, outputs only give. */
    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, EasyFarms.FARM_BE.get(),
                (farm, side) -> new SidedInvWrapper(farm.automation(), side));
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
}
