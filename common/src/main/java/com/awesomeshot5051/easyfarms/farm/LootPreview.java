package com.awesomeshot5051.easyfarms.farm;

import com.awesomeshot5051.easyfarms.net.LootPreviewPayload;
import com.awesomeshot5051.easyfarms.platform.Services;
import com.awesomeshot5051.easyfarms.recipe.FarmRecipe;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * What a loot table can drop, for recipe viewers. Loot tables only exist on the server, so the server rolls every loot table a farm
 * recipe uses many times (no luck, a fishing rod as tool) and sends the items with how often they came up. Rolling instead of reading
 * the table works for nested tables, tags and modded entries, and follows datapack changes on /reload.
 */
public final class LootPreview {
    /** Rolls per table. Items rarer than about 1 in this many can be missed. */
    private static final int SAMPLES = 4000;

    private LootPreview() {
    }

    /** An item and the chance (0 to 1) that one roll of the table includes it. */
    public record Entry(ResourceLocation item, float chance) {
    }

    /** Computes the preview for every loot table used by a farm recipe. */
    public static Map<ResourceLocation, List<Entry>> compute(MinecraftServer server) {
        Set<ResourceLocation> tables = new HashSet<>();
        for (var holder : server.getRecipeManager().getRecipes()) {
            if (!(holder.value() instanceof FarmRecipe recipe)) continue;
            for (DropEntry drop : recipe.drops()) drop.lootTable().ifPresent(tables::add);
            recipe.inscriber().ifPresent(i -> i.drops().forEach(d -> d.lootTable().ifPresent(tables::add)));
        }
        Map<ResourceLocation, List<Entry>> result = new HashMap<>();
        ServerLevel level = server.overworld();
        Vec3 origin = Vec3.atCenterOf(level.getSharedSpawnPos());
        for (ResourceLocation table : tables) {
            Map<ResourceLocation, Integer> counts = new HashMap<>();
            for (int i = 0; i < SAMPLES; i++) {
                List<ItemStack> rolled = new ArrayList<>();
                DropEntry.sampleLootTable(level, table, origin, new ItemStack(Items.FISHING_ROD), 0f, rolled);
                Set<ResourceLocation> seen = new HashSet<>();
                for (ItemStack stack : rolled) seen.add(BuiltInRegistries.ITEM.getKey(stack.getItem()));
                for (ResourceLocation id : seen) counts.merge(id, 1, Integer::sum);
            }
            List<Entry> entries = new ArrayList<>();
            counts.forEach((id, n) -> entries.add(new Entry(id, n / (float) SAMPLES)));
            entries.sort((a, b) -> Float.compare(b.chance(), a.chance()));
            result.put(table, entries);
        }
        return result;
    }

    /** Sends the preview to one player (called after a datapack sync and on join). */
    public static void send(MinecraftServer server, ServerPlayer player) {
        Map<ResourceLocation, List<Entry>> preview = compute(server);
        if (!preview.isEmpty()) Services.PLATFORM.sendToPlayer(player, new LootPreviewPayload(preview));
    }
}
