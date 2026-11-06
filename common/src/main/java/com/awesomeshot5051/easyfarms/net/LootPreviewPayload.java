package com.awesomeshot5051.easyfarms.net;

import com.awesomeshot5051.easyfarms.EasyFarms;
import com.awesomeshot5051.easyfarms.farm.LootPreview;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Server to client: what each loot table used by a farm recipe can drop (see {@link LootPreview}), for JEI. */
public record LootPreviewPayload(Map<ResourceLocation, List<LootPreview.Entry>> tables) implements CustomPacketPayload {
    public static final Type<LootPreviewPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(EasyFarms.MODID, "loot_preview"));

    public static final StreamCodec<RegistryFriendlyByteBuf, LootPreviewPayload> CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeVarInt(payload.tables().size());
                payload.tables().forEach((table, entries) -> {
                    buf.writeResourceLocation(table);
                    buf.writeVarInt(entries.size());
                    for (LootPreview.Entry e : entries) {
                        buf.writeResourceLocation(e.item());
                        buf.writeFloat(e.chance());
                    }
                });
            },
            buf -> {
                Map<ResourceLocation, List<LootPreview.Entry>> tables = new HashMap<>();
                int n = buf.readVarInt();
                for (int i = 0; i < n; i++) {
                    ResourceLocation table = buf.readResourceLocation();
                    int m = buf.readVarInt();
                    List<LootPreview.Entry> entries = new ArrayList<>();
                    for (int j = 0; j < m; j++) entries.add(new LootPreview.Entry(buf.readResourceLocation(), buf.readFloat()));
                    tables.put(table, entries);
                }
                return new LootPreviewPayload(tables);
            });

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
