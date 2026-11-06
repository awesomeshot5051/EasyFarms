package com.awesomeshot5051.easyfarms.farm;

import com.awesomeshot5051.easyfarms.EasyFarms;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Which farms hold their chunk loaded (the Chunk Loader card). It uses vanilla forced chunks, so it works the same on every
 * loader and survives restarts. A chunk is only un-forced if this class forced it: a chunk that /forceload already holds
 * stays forced when the last card goes away.
 */
public class FarmChunkLoaders extends SavedData {
    private record Entry(long chunk, boolean owned, List<Long> farms) {
        static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.LONG.fieldOf("chunk").forGetter(Entry::chunk),
                Codec.BOOL.optionalFieldOf("owned", false).forGetter(Entry::owned),
                Codec.LONG.listOf().optionalFieldOf("farms", List.of()).forGetter(Entry::farms)
        ).apply(i, Entry::new));
    }

    private static final Codec<FarmChunkLoaders> CODEC = RecordCodecBuilder.create(i -> i.group(
            Entry.CODEC.listOf().optionalFieldOf("chunks", List.of()).forGetter(FarmChunkLoaders::entries)
    ).apply(i, FarmChunkLoaders::fromEntries));

    private static final SavedDataType<FarmChunkLoaders> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(EasyFarms.MODID, "chunk_loaders"), FarmChunkLoaders::new, CODEC, DataFixTypes.SAVED_DATA_FORCED_CHUNKS);

    /** chunk -> positions of the farms keeping it loaded. */
    private final Map<Long, Set<Long>> farms = new HashMap<>();
    /** Chunks this class forced itself (and so may release). */
    private final Set<Long> owned = new HashSet<>();

    public static FarmChunkLoaders get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    /** Called by a farm: {@code loading} says whether it has a working Chunk Loader card right now. */
    public static void update(ServerLevel level, BlockPos pos, boolean loading) {
        FarmChunkLoaders data = get(level);
        ChunkPos chunk = ChunkPos.containing(pos);
        long key = chunk.pack();
        Set<Long> set = data.farms.get(key);
        if (loading) {
            if (set == null) set = data.farms.computeIfAbsent(key, k -> new HashSet<>());
            if (set.add(pos.asLong())) data.setDirty();
            if (set.size() == 1 && !data.owned.contains(key) && level.setChunkForced(chunk.x(), chunk.z(), true)) {
                data.owned.add(key);
                data.setDirty();
            }
        } else if (set != null && set.remove(pos.asLong())) {
            data.setDirty();
            if (set.isEmpty()) {
                data.farms.remove(key);
                if (data.owned.remove(key)) level.setChunkForced(chunk.x(), chunk.z(), false);
            }
        }
    }

    private List<Entry> entries() {
        List<Entry> list = new ArrayList<>();
        for (Map.Entry<Long, Set<Long>> e : farms.entrySet()) list.add(new Entry(e.getKey(), owned.contains(e.getKey()), new ArrayList<>(e.getValue())));
        return list;
    }

    private static FarmChunkLoaders fromEntries(List<Entry> entries) {
        FarmChunkLoaders data = new FarmChunkLoaders();
        for (Entry entry : entries) {
            if (entry.farms().isEmpty()) continue;
            data.farms.put(entry.chunk(), new HashSet<>(entry.farms()));
            if (entry.owned()) data.owned.add(entry.chunk());
        }
        return data;
    }
}
