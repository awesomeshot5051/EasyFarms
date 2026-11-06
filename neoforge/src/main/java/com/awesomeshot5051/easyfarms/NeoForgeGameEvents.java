package com.awesomeshot5051.easyfarms;

import com.awesomeshot5051.easyfarms.events.FarmEvents;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = EasyFarms.MODID)
public class NeoForgeGameEvents {
    /** JEI shows what a loot-table drop can give: the server rolls the tables and sends the result (after a reload and on join). */
    @SubscribeEvent
    public static void onDatapackSync(net.neoforged.neoforge.event.OnDatapackSyncEvent event) {
        event.getRelevantPlayers().forEach(player -> com.awesomeshot5051.easyfarms.farm.LootPreview.send(event.getPlayerList().getServer(), player));
    }

    /** Cards and dyes work on a farm while sneaking. */
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (FarmEvents.allowsSneakUse(event.getLevel(), event.getPos(), event.getItemStack())) {
            event.setUseBlock(TriState.TRUE);
        }
    }
}
