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
 * </ul>
 */
public record DropEntry(ResourceLocation item, int min, int max, float chance,
                        Optional<Integer> fortuneMin, Optional<Integer> fortuneMax,
                        Optional<Integer> lootingMin, Optional<Integer> lootingMax,
                        Optional<ResourceLocation> silkItem, Optional<ResourceLocation> smeltItem,
                        Optional<ResourceLocation> fireItem, boolean silkOnly) {

    public static final Codec<DropEntry> CODEC = RecordCodecBuilder.create(i -> i.group(
            ResourceLocation.CODEC.fieldOf("item").forGetter(DropEntry::item),
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
            Codec.BOOL.optionalFieldOf("silk_only", false).forGetter(DropEntry::silkOnly)
    ).apply(i, DropEntry::new));

    public static DropEntry simple(ResourceLocation item, int min, int max) {
        return new DropEntry(item, min, max, 1F, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), false);
    }

    public void roll(ServerLevel level, boolean fortune, boolean looting, boolean silk, boolean smelter, boolean fire, List<ItemStack> out) {
        if (silkOnly && !silk) return;
        if (level.random.nextFloat() >= chance) return;
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
}
