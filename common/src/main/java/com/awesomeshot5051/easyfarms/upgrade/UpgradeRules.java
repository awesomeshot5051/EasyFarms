package com.awesomeshot5051.easyfarms.upgrade;

import com.awesomeshot5051.easyfarms.recipe.FarmRecipe;
import com.awesomeshot5051.easyfarms.tier.FarmUpgrade;
import com.awesomeshot5051.easyfarms.tier.UpgradeRestriction;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Evaluates the {@link UpgradeRestriction}s of cards: exclusive (with another card or a tool enchantment) and
 * inclusive-required (needs another card). Pure functions of the data, so the server and the client screen agree.
 */
public final class UpgradeRules {
    private UpgradeRules() {
    }

    /** What the rules look at: the farm's current recipe (may be null) and the tool in its tool slot. */
    public record Context(RegistryAccess access, @Nullable FarmRecipe recipe, ItemStack tool,
                          @Nullable net.minecraft.world.level.block.state.BlockState below) {
    }

    /**
     * Why {@code candidate} cannot go with the others, or null if it can.
     *
     * @param exclusionPeers the cards the candidate may not coexist with
     * @param requirePeers   the cards that count as "installed" for its requirements
     */
    @Nullable
    public static Component problem(Context ctx, FarmUpgrade candidate, List<FarmUpgrade> exclusionPeers, List<FarmUpgrade> requirePeers) {
        ResourceLocation candidateId = FarmUpgrade.id(ctx.access(), candidate);
        for (UpgradeRestriction r : candidate.restrictions()) {
            if (!r.appliesTo(ctx.recipe())) continue;
            for (FarmUpgrade other : exclusionPeers) {
                if (r.excludes().contains(FarmUpgrade.id(ctx.access(), other))) {
                    return Component.translatable("gui.easy_farms.restriction.excludes", other.displayName());
                }
            }
            for (ResourceLocation enchantment : r.excludesEnchantments()) {
                if (hasEnchantment(ctx, enchantment)) {
                    return Component.translatable("gui.easy_farms.restriction.excludes_enchantment", enchantmentName(enchantment));
                }
            }
            if (!r.blockBelowOk(ctx.below())) {
                List<Component> names = new ArrayList<>();
                for (String scope : r.requiresBlockBelow()) names.add(blockName(scope));
                return Component.translatable("gui.easy_farms.restriction.requires_block_below",
                        net.minecraft.network.chat.ComponentUtils.formatList(names, Component.literal(", ")));
            }
            for (ResourceLocation required : r.requires()) {
                boolean present = false;
                for (FarmUpgrade other : requirePeers) present |= required.equals(FarmUpgrade.id(ctx.access(), other));
                if (!present) {
                    FarmUpgrade needed = FarmUpgrade.byId(ctx.access(), required);
                    return Component.translatable("gui.easy_farms.restriction.requires",
                            needed != null ? needed.displayName() : Component.literal(required.toString()));
                }
            }
        }
        // Exclusions are mutual: a card already installed may be the one that names the candidate.
        for (FarmUpgrade other : exclusionPeers) {
            for (UpgradeRestriction r : other.restrictions()) {
                if (r.appliesTo(ctx.recipe()) && r.excludes().contains(candidateId)) {
                    return Component.translatable("gui.easy_farms.restriction.excludes", other.displayName());
                }
            }
        }
        return null;
    }

    /** The reason a card cannot be added next to the {@code installed} ones, or null. */
    @Nullable
    public static Component installProblem(Context ctx, FarmUpgrade candidate, List<FarmUpgrade> installed) {
        int catalystSlots = candidate.catalystItems().size();
        for (FarmUpgrade other : installed) catalystSlots += other.catalystItems().size();
        if (catalystSlots > com.awesomeshot5051.easyfarms.farm.FarmBlockEntity.UPGRADE_SLOTS) {
            return Component.translatable("gui.easy_farms.restriction.no_catalyst_slots", com.awesomeshot5051.easyfarms.farm.FarmBlockEntity.UPGRADE_SLOTS);
        }
        return problem(ctx, candidate, installed, installed);
    }

    /**
     * The installed cards that actually work, in slot order. A card whose requirements are missing, or that clashes with a
     * card in an earlier slot or with the tool's enchantments, is switched off (the earlier slot wins).
     */
    public static List<FarmUpgrade> working(Context ctx, List<FarmUpgrade> installed) {
        List<FarmUpgrade> list = new ArrayList<>(installed);
        boolean changed = true;
        while (changed) {
            changed = false;
            for (int i = 0; i < list.size(); i++) {
                List<FarmUpgrade> others = new ArrayList<>(list);
                others.remove(i);
                if (problem(ctx, list.get(i), list.subList(0, i), others) != null) {
                    list.remove(i);
                    changed = true;
                    break;
                }
            }
        }
        return list;
    }

    private static boolean hasEnchantment(Context ctx, ResourceLocation id) {
        if (ctx.tool().isEmpty()) return false;
        var registry = ctx.access().registryOrThrow(Registries.ENCHANTMENT);
        Holder<Enchantment> holder = registry.getHolder(id).orElse(null);
        return holder != null && EnchantmentHelper.getItemEnchantmentLevel(holder, ctx.tool()) > 0;
    }

    /** An item id as its name, or a {@code #tag} as written. */
    public static Component itemName(String scope) {
        if (scope.startsWith("#")) return Component.literal(scope);
        ResourceLocation id = ResourceLocation.tryParse(scope);
        return id == null ? Component.literal(scope)
                : net.minecraft.core.registries.BuiltInRegistries.ITEM.getOptional(id).<Component>map(i -> i.getName(new ItemStack(i))).orElse(Component.literal(scope));
    }

    /** True if the stack is one of the listed items (ids or {@code #tags}). */
    public static boolean matchesAny(List<String> scopes, ItemStack stack) {
        for (String scope : scopes) if (UpgradeEffects.ConvertDrops.Rule.matchesItem(scope, stack)) return true;
        return false;
    }

    /** A block id as its name, or a {@code #tag} as written. */
    public static Component blockName(String scope) {
        if (scope.startsWith("#")) return Component.literal(scope);
        ResourceLocation id = ResourceLocation.tryParse(scope);
        return id == null ? Component.literal(scope)
                : net.minecraft.core.registries.BuiltInRegistries.BLOCK.getOptional(id).<Component>map(b -> b.getName()).orElse(Component.literal(scope));
    }

    private static Component enchantmentName(ResourceLocation id) {
        return Component.translatable(Util.makeDescriptionId("enchantment", id));
    }
}
