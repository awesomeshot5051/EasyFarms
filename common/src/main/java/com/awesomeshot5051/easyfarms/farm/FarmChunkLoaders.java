package com.awesomeshot5051.easyfarms.farm;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Which farms hold their chunk loaded (the Chunk Loader card). It uses vanilla forced chunks, so it works the same on every
 * loader and survives restarts. A chunk is only un-forced if this class forced it: a chunk that /forceload already holds
 * stays forced when the last card goes away.
 */
public class FarmChunkLoaders extends SavedData {
    private static final String NAME = "easy_farms_chunk_loaders";
    private static final Factory<FarmChunkLoaders> FACTORY = new Factory<>(FarmChunkLoaders::new, FarmChunkLoaders::load, null);

    /** chunk -> positions of the farms keeping it loaded. */
    private final Map<Long, Set<Long>> farms = new HashMap<>();
    /** Chunks this class forced itself (and so may release). */
    private final Set<Long> owned = new HashSet<>();

    public static FarmChunkLoaders get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(FACTORY, NAME);
    }

    /** Called by a farm: {@code loading} says whether it has a working Chunk Loader card right now. */
    public static void update(ServerLevel level, BlockPos pos, boolean loading) {
        FarmChunkLoaders data = get(level);
        ChunkPos chunk = new ChunkPos(pos);
        long key = chunk.toLong();
        Set<Long> set = data.farms.get(key);
        if (loading) {
            if (set == null) set = data.farms.computeIfAbsent(key, k -> new HashSet<>());
            if (set.add(pos.asLong())) data.setDirty();
            if (set.size() == 1 && !data.owned.contains(key) && level.setChunkForced(chunk.x, chunk.z, true)) {
                data.owned.add(key);
                data.setDirty();
            }
        } else if (set != null && set.remove(pos.asLong())) {
            data.setDirty();
            if (set.isEmpty()) {
                data.farms.remove(key);
                if (data.owned.remove(key)) level.setChunkForced(chunk.x, chunk.z, false);
            }
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Map.Entry<Long, Set<Long>> e : farms.entrySet()) {
            CompoundTag entry = new CompoundTag();
            entry.putLong("Chunk", e.getKey());
            entry.putBoolean("Owned", owned.contains(e.getKey()));
            entry.putLongArray("Farms", e.getValue().stream().mapToLong(Long::longValue).toArray());
            list.add(entry);
        }
        tag.put("Chunks", list);
        return tag;
    }

    private static FarmChunkLoaders load(CompoundTag tag, HolderLookup.Provider registries) {
        FarmChunkLoaders data = new FarmChunkLoaders();
        for (Tag t : tag.getList("Chunks", Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) t;
            long key = entry.getLong("Chunk");
            Set<Long> set = new HashSet<>();
            for (long l : entry.getLongArray("Farms")) set.add(l);
            if (set.isEmpty()) continue;
            data.farms.put(key, set);
            if (entry.getBoolean("Owned")) data.owned.add(key);
        }
        return data;
    }
}
