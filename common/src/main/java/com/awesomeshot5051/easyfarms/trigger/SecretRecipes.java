package com.awesomeshot5051.easyfarms.trigger;

import com.awesomeshot5051.easyfarms.platform.Services;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.HashSet;
import java.util.Set;

/** Which secret recipes a player knows. Secret recipes still run for everyone; "discovered" only decides whether the recipe viewers list them. */
public final class SecretRecipes {
    private SecretRecipes() {
    }

    public static boolean isDiscovered(Player player, Identifier recipeId) {
        return player != null && Services.PLATFORM.discoveredSecrets(player).contains(recipeId);
    }

    public static Set<Identifier> discovered(Player player) {
        return player == null ? Set.of() : Services.PLATFORM.discoveredSecrets(player);
    }

    /** Marks the recipe discovered (once) and fires the advancement trigger. */
    public static void discover(ServerPlayer player, Identifier recipeId) {
        Set<Identifier> known = Services.PLATFORM.discoveredSecrets(player);
        if (known.contains(recipeId)) return;
        Set<Identifier> updated = new HashSet<>(known);
        updated.add(recipeId);
        Services.PLATFORM.setDiscoveredSecrets(player, updated);
        ModTriggers.SECRET_RECIPE.get().trigger(player, recipeId);
    }
}
