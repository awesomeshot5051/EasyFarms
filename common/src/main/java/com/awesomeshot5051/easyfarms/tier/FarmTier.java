package com.awesomeshot5051.easyfarms.tier;

import com.awesomeshot5051.easyfarms.EasyFarms;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.Optional;

/**
 * A tool tier, loaded from data/&lt;ns&gt;/easy_farms/farm_tier/*.json, so other mods and datapacks can add their own.
 *
 * @param tool                   items that count as this tier
 * @param rank                   ordering; recipes can require a minimum rank
 * @param speedDivisor           the recipe's base time is divided by this
 * @param breakMultiplier        extra break time for pickaxe and shovel recipes = this x 20 ticks (x 10 with Efficiency)
 * @param weaponBreakMultiplier  the same for sword, axe and hoe recipes (the old mob and plant farms used a different curve)
 * @param inputLifetimeMultiplier how long the tool lasts = base days (config) x this
 * @param miningTier             optional: the "incorrect_for_..._tool" block tag that identifies this tier for other mods' tools

 * @param priority               when several tiers list the same tool, the highest priority wins (then the highest rank): the way a datapack overrides which tier an item belongs to
 * @param toolDurability         optional: the tool durability this tier stands for (wood 59 ... netherite 2031), used to place modded tools that are in no list and have no tool-component speed
 * @param miningSpeed            optional: the vanilla material mining speed, used to place a modded tool whose tag matches no tier (0 = unused)
 */
public record FarmTier(Ingredient tool, int rank, float speedDivisor, int breakMultiplier, float weaponBreakMultiplier,
                       int inputLifetimeMultiplier, Optional<Identifier> miningTier, float miningSpeed,
                       int priority, Optional<Integer> toolDurability) {
    public static final ResourceKey<Registry<FarmTier>> REGISTRY_KEY =
            ResourceKey.createRegistryKey(Identifier.fromNamespaceAndPath(EasyFarms.MODID, "farm_tier"));

    public static final Codec<FarmTier> CODEC = RecordCodecBuilder.create(i -> i.group(
            Ingredient.CODEC.fieldOf("tool").forGetter(FarmTier::tool),
            Codec.INT.fieldOf("rank").forGetter(FarmTier::rank),
            Codec.FLOAT.optionalFieldOf("speed_divisor", 1.0F).forGetter(FarmTier::speedDivisor),
            Codec.INT.optionalFieldOf("break_multiplier", 0).forGetter(FarmTier::breakMultiplier),
            Codec.FLOAT.optionalFieldOf("weapon_break_multiplier", 0F).forGetter(FarmTier::weaponBreakMultiplier),
            Codec.INT.optionalFieldOf("input_lifetime_multiplier", 1).forGetter(FarmTier::inputLifetimeMultiplier),
            Identifier.CODEC.optionalFieldOf("mining_tier").forGetter(FarmTier::miningTier),
            Codec.FLOAT.optionalFieldOf("mining_speed", 0F).forGetter(FarmTier::miningSpeed),
            Codec.INT.optionalFieldOf("priority", 0).forGetter(FarmTier::priority),
            Codec.INT.optionalFieldOf("tool_durability").forGetter(FarmTier::toolDurability)
    ).apply(i, FarmTier::new));

    /**
     * Which tier a tool belongs to:
     * <ol>
     * <li>a tier whose tool list contains it (highest priority, then highest rank, wins);</li>
     * <li>for a tool with a tool component: the tier with the nearest mining speed, preferring the tier that has the
     * component's "incorrect blocks" tag when two are equally near;</li>
     * <li>any other damageable item: the tier with the nearest {@code tool_durability}.</li>
     * </ol>
     * Returns null when none applies. To give a mod's tools their own tier, list them in a tier file; to move them,
     * list them in a tier with a higher priority.
     */
    public static FarmTier find(RegistryAccess access, ItemStack tool) {
        if (tool.isEmpty()) return null;
        FarmTier best = null;
        for (FarmTier tier : access.lookupOrThrow(REGISTRY_KEY)) {
            if (!tier.tool().test(tool)) continue;
            if (best == null || tier.priority() > best.priority() || (tier.priority() == best.priority() && tier.rank() > best.rank())) best = tier;
        }
        if (best != null) return best;
        var toolData = tool.get(net.minecraft.core.component.DataComponents.TOOL);
        if (toolData != null) {
            FarmTier byMaterial = byMaterial(access, toolData);
            if (byMaterial != null) return byMaterial;
        }
        return tool.isDamageableItem() ? byDurability(access, tool.getMaxDamage()) : null;
    }

    /** Nearest mining speed; the tier carrying the component's tag wins a tie, then the higher rank. */
    private static FarmTier byMaterial(RegistryAccess access, net.minecraft.world.item.component.Tool material) {
        Identifier tag = null;
        float speed = material.defaultMiningSpeed();
        for (var rule : material.rules()) {
            if (rule.correctForDrops().isPresent() && !rule.correctForDrops().get()
                    && rule.blocks() instanceof net.minecraft.core.HolderSet.Named<net.minecraft.world.level.block.Block> named) {
                tag = named.key().location();
            }
            if (rule.speed().isPresent()) speed = Math.max(speed, rule.speed().get());
        }
        FarmTier nearest = null;
        for (FarmTier tier : access.lookupOrThrow(REGISTRY_KEY)) {
            if (tier.miningSpeed() <= 0) continue;
            if (nearest == null) {
                nearest = tier;
                continue;
            }
            float a = Math.abs(tier.miningSpeed() - speed), b = Math.abs(nearest.miningSpeed() - speed);
            boolean tagA = tag != null && tier.miningTier().filter(tag::equals).isPresent();
            boolean tagB = tag != null && nearest.miningTier().filter(tag::equals).isPresent();
            if (a < b || (a == b && (tagA && !tagB || tagA == tagB && tier.rank() > nearest.rank()))) nearest = tier;
        }
        return nearest;
    }

    private static FarmTier byDurability(RegistryAccess access, int durability) {
        FarmTier nearest = null;
        for (FarmTier tier : access.lookupOrThrow(REGISTRY_KEY)) {
            if (tier.toolDurability().isEmpty()) continue;
            if (nearest == null || Math.abs(tier.toolDurability().get() - durability) < Math.abs(nearest.toolDurability().get() - durability)) nearest = tier;
        }
        return nearest;
    }
}
