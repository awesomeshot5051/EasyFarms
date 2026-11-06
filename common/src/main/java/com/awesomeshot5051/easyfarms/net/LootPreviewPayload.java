package com.awesomeshot5051.easyfarms.net;

import com.awesomeshot5051.easyfarms.EasyFarms;
import com.awesomeshot5051.easyfarms.farm.LootPreview;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Server to client: what each loot table used by a farm recipe can drop (see {@link LootPreview}), for JEI. */
public record LootPreviewPayload(Map<Identifier, List<LootPreview.Entry>> tables) implements CustomPacketPayload {
    public static final Type<LootPreviewPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(EasyFarms.MODID, "loot_preview"));

    public static final StreamCodec<RegistryFriendlyByteBuf, LootPreviewPayload> CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeVarInt(payload.tables().size());
                payload.tables().forEach((table, entries) -> {
                    buf.writeIdentifier(table);
                    buf.writeVarInt(entries.size());
                    for (LootPreview.Entry e : entries) {
                        buf.writeIdentifier(e.item());
                        buf.writeFloat(e.chance());
                    }
                });
            },
            buf -> {
                Map<Identifier, List<LootPreview.Entry>> tables = new HashMap<>();
                int n = buf.readVarInt();
                for (int i = 0; i < n; i++) {
                    Identifier table = buf.readIdentifier();
                    int m = buf.readVarInt();
                    List<LootPreview.Entry> entries = new ArrayList<>();
                    for (int j = 0; j < m; j++) entries.add(new LootPreview.Entry(buf.readIdentifier(), buf.readFloat()));
                    tables.put(table, entries);
                }
                return new LootPreviewPayload(tables);
            });

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
