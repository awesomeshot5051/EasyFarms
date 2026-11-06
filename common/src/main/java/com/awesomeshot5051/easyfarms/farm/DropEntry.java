package com.awesomeshot5051.easyfarms.farm;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.Optional;

/**
 * One line of a recipe's drop table.
 * <ul>
 * <li>min/max/chance: the normal count range and the chance this line drops at all.</li>
 * <li>fortuneMin/fortuneMax and lootingMin/lootingMax: replace the range when the tool has Fortune / Looting
 * (Looting is the sword on Mob farms; the old mob farms jumped from 1-3 to 4-8).</li>
 * <li>silkItem: used with Silk Touch (unless the smelter upgrade is installed).</li>
 * <li>smeltItem: used with the smelter upgrade.</li>
 * <li>fireItem: used when the tool has Fire Aspect (the cooked version of a mob drop).</li>
 * <li>silkOnly: drops only when the tool has Silk Touch (ice, budding amethyst: nothing drops without it).</li>
 * <li>lootTable: instead of an item, roll this vanilla/modded loot table (fishing and chest tables; fishing ones see the farm's tool and its
 * luck). {@code item} is then optional. {@code preview} lists the items it can give, for recipe viewers only (they cannot read loot tables).</li>
 * </ul>
 */
public record DropEntry(ResourceLocation item, int min, int max, float chance,
                        Optional<Integer> fortuneMin, Optional<Integer> fortuneMax,
                        Optional<Integer> lootingMin, Optional<Integer> lootingMax,
                        Optional<ResourceLocation> silkItem, Optional<ResourceLocation> smeltItem,
                        Optional<ResourceLocation> fireItem, boolean silkOnly, Optional<ResourceLocation> lootTable, List<ResourceLocation> preview) {

    public static final Codec<DropEntry> CODEC = RecordCodecBuilder.create(i -> i.group(
            ResourceLocation.CODEC.optionalFieldOf("item", ResourceLocation.withDefaultNamespace("air")).forGetter(DropEntry::item),
            Codec.INT.optionalFieldOf("min", 1).forGetter(DropEntry::min),
            Codec.INT.optionalFieldOf("max", 1).forGetter(DropEntry::max),
            Codec.FLOAT.optionalFieldOf("chance", 1.0F).forGetter(DropEntry::chance),
            Codec.INT.optionalFieldOf("fortune_min").forGetter(DropEntry::fortuneMin),
            Codec.INT.optionalFieldOf("fortune_max").forGetter(DropEntry::fortuneMax),
            Codec.INT.optionalFieldOf("looting_min").forGetter(DropEntry::lootingMin),
            Codec.INT.optionalFieldOf("looting_max").forGetter(DropEntry::lootingMax),
            ResourceLocation.CODEC.optionalFieldOf("silk_item").forGetter(DropEntry::silkItem),
            ResourceLocation.CODEC.optionalFieldOf("smelt_item").forGetter(DropEntry::smeltItem),
            ResourceLocation.CODEC.optionalFieldOf("fire_item").forGetter(DropEntry::fireItem),
            Codec.BOOL.optionalFieldOf("silk_only", false).forGetter(DropEntry::silkOnly),
            ResourceLocation.CODEC.optionalFieldOf("loot_table").forGetter(DropEntry::lootTable),
            ResourceLocation.CODEC.listOf().optionalFieldOf("preview", List.of()).forGetter(DropEntry::preview)
    ).apply(i, DropEntry::new));

    public static DropEntry simple(ResourceLocation item, int min, int max) {
        return new DropEntry(item, min, max, 1F, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), false, Optional.empty(), java.util.List.of());
    }

    public void roll(ServerLevel level, FarmBlockEntity farm, boolean fortune, boolean looting, boolean silk, boolean smelter, boolean fire, List<ItemStack> out) {
        if (silkOnly && !silk) return;
        if (level.random.nextFloat() >= chance) return;
        if (lootTable.isPresent()) {
            rollLootTable(level, farm, out);
            return;
        }
        int lo = min, hi = max;
        if (looting && lootingMax.isPresent()) {
            lo = lootingMin.orElse(min);
            hi = lootingMax.get();
        } else if (fortune && fortuneMax.isPresent()) {
            lo = fortuneMin.orElse(min);
            hi = fortuneMax.get();
        }
        int count = level.random.nextIntBetweenInclusive(lo, Math.max(lo, hi));
        // Silk Touch yields the raw block unless the smelter upgrade is installed (matches the old resource farms).
        ResourceLocation id = item;
        if (silk && !smelter && silkItem.isPresent()) id = silkItem.get();
        else if (smelter && smeltItem.isPresent()) id = smeltItem.get();
        else if (fire && fireItem.isPresent()) id = fireItem.get();
        Item resolved = BuiltInRegistries.ITEM.get(id);
        if (count > 0 && resolved != Items.AIR) out.add(new ItemStack(resolved, count));
    }

    /** Rolls the loot table with the farm's position and tool. Only fishing and chest tables fit; others (entity, block) are skipped. */
    private void rollLootTable(ServerLevel level, FarmBlockEntity farm, List<ItemStack> out) {
        sampleLootTable(level, lootTable.get(), net.minecraft.world.phys.Vec3.atCenterOf(farm.getBlockPos()), farm.getTool().copy(), farm.lootLuck(), out);
    }

    /**
     * One roll of a loot table at a position with a tool and luck. Fishing tables also get a fresh fishing bobber ("in open water", so
     * treasure can drop); chest tables get the position only; other table types (entity, block) are skipped. Also used to preview a
     * table for recipe viewers (see {@code LootPreview}).
     */
    public static void sampleLootTable(ServerLevel level, ResourceLocation table, net.minecraft.world.phys.Vec3 origin, ItemStack tool, float luck, List<ItemStack> out) {
        var key = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE, table);
        var loot = level.getServer().reloadableRegistries().getLootTable(key);
        if (loot == net.minecraft.world.level.storage.loot.LootTable.EMPTY) return;
        var builder = new net.minecraft.world.level.storage.loot.LootParams.Builder(level)
                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN, origin);
        var set = loot.getParamSet();
        if (set == net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.FISHING) {
            builder.withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.TOOL, tool);
            var bobber = net.minecraft.world.entity.EntityType.FISHING_BOBBER.create(level);
            if (bobber != null) {
                bobber.setPos(origin);
                builder.withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.THIS_ENTITY, bobber);
            }
        } else if (set != net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.CHEST) {
            return;
        }
        builder.withLuck(luck);
        loot.getRandomItems(builder.create(set), out::add);
    }
}
