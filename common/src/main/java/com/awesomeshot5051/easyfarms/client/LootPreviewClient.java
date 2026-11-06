package com.awesomeshot5051.easyfarms.client;

import com.awesomeshot5051.easyfarms.farm.LootPreview;
import com.awesomeshot5051.easyfarms.net.LootPreviewPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Map;

/** The loot previews the server sent (see {@link LootPreview}); read by the JEI category. */
public final class LootPreviewClient {
    private static volatile Map<ResourceLocation, List<LootPreview.Entry>> tables = Map.of();

    private LootPreviewClient() {
    }

    public static void receive(LootPreviewPayload payload) {
        tables = Map.copyOf(payload.tables());
    }

    /** What the table can drop, most likely first, or an empty list if the server sent nothing for it. */
    public static List<LootPreview.Entry> get(ResourceLocation table) {
        return tables.getOrDefault(table, List.of());
    }

    public static void clear() {
        tables = Map.of();
    }
}
