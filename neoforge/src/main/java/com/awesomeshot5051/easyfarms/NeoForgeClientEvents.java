package com.awesomeshot5051.easyfarms;

import com.awesomeshot5051.easyfarms.client.CardTooltips;
import com.awesomeshot5051.easyfarms.client.ClientRecipes;
import com.awesomeshot5051.easyfarms.client.SecretSync;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RecipesReceivedEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

@EventBusSubscriber(modid = EasyFarms.MODID, value = Dist.CLIENT)
public class NeoForgeClientEvents {
    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        CardTooltips.addTo(event.getItemStack(), event.getToolTip());
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        SecretSync.onClientTick();
    }

    @SubscribeEvent
    public static void onRecipesReceived(RecipesReceivedEvent event) {
        ClientRecipes.set(event.getRecipeMap().values());
    }
}
