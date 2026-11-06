package com.awesomeshot5051.easyfarms.client;

import com.awesomeshot5051.easyfarms.platform.Services;
import com.awesomeshot5051.easyfarms.trigger.SecretRecipes;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

import java.util.Set;

/** Watches the local player's discovered secret recipes and tells JEI / EMI to show newly discovered ones. */
public final class SecretSync {
    private static Set<Identifier> last = Set.of();
    private static int ticks;

    private SecretSync() {
    }

    /** Called by the loader once per client tick. */
    public static void onClientTick() {
        if (++ticks % 20 != 0) return;
        var player = Minecraft.getInstance().player;
        if (player == null) LootPreviewClient.clear();
        Set<Identifier> now = player == null ? Set.of() : Set.copyOf(SecretRecipes.discovered(player));
        if (now.equals(last)) return;
        last = now;
        if (Services.PLATFORM.isModLoaded("jei")) com.awesomeshot5051.easyfarms.compat.jei.EasyFarmsJeiPlugin.refreshSecrets();
    }
}
