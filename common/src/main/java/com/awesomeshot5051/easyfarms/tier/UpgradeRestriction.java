package com.awesomeshot5051.easyfarms.tier;

import com.awesomeshot5051.easyfarms.farm.DropEntry;
import com.awesomeshot5051.easyfarms.recipe.FarmRecipe;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * One compatibility rule of an upgrade card, written in its farm_upgrade file under {@code "restrictions"}.
 *
 * @param whenDrops          optional scope: the rule only applies while the farm's recipe drops one of these (item ids or
 *                           {@code #tags}). Empty = always applies.
 * @param requires           upgrade ids (farm_upgrade file ids) that must be installed too, or the card cannot be added and does nothing
 * @param excludes           upgrade ids that cannot be installed together with this card (applies both ways)
 * @param excludesEnchantments enchantment ids on the farm's tool that make the card unusable
 */
public record UpgradeRestriction(List<String> whenDrops, List<ResourceLocation> requires, List<ResourceLocation> excludes,
                                 List<ResourceLocation> excludesEnchantments) {
    public static final Codec<UpgradeRestriction> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.listOf().optionalFieldOf("when_drops", List.of()).forGetter(UpgradeRestriction::whenDrops),
            ResourceLocation.CODEC.listOf().optionalFieldOf("requires", List.of()).forGetter(UpgradeRestriction::requires),
            ResourceLocation.CODEC.listOf().optionalFieldOf("excludes", List.of()).forGetter(UpgradeRestriction::excludes),
            ResourceLocation.CODEC.listOf().optionalFieldOf("excludes_enchantments", List.of()).forGetter(UpgradeRestriction::excludesEnchantments)
    ).apply(i, UpgradeRestriction::new));

    /** True if the rule is in force for this recipe (a scoped rule never applies while the farm has no recipe). */
    public boolean appliesTo(@Nullable FarmRecipe recipe) {
        if (whenDrops.isEmpty()) return true;
        if (recipe == null) return false;
        for (DropEntry drop : recipe.drops()) {
            Item item = BuiltInRegistries.ITEM.get(drop.item());
            for (String scope : whenDrops) {
                if (scope.startsWith("#")) {
                    ResourceLocation tag = ResourceLocation.tryParse(scope.substring(1));
                    if (tag != null && item.builtInRegistryHolder().is(TagKey.create(Registries.ITEM, tag))) return true;
                } else if (scope.equals(drop.item().toString())) {
                    return true;
                }
            }
        }
        return false;
    }
}
