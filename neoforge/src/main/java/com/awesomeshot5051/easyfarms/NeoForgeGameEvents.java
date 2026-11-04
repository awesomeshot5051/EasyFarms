package com.awesomeshot5051.easyfarms;

import com.awesomeshot5051.easyfarms.events.FarmEvents;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = EasyFarms.MODID)
public class NeoForgeGameEvents {
    /** Cards and dyes work on a farm while sneaking. */
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (FarmEvents.allowsSneakUse(event.getLevel(), event.getPos(), event.getItemStack())) {
            event.setUseBlock(TriState.TRUE);
        }
    }
}
