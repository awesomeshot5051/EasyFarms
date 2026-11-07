package com.awesomeshot5051.easyfarms.tier;

import com.awesomeshot5051.easyfarms.EasyFarms;
import com.awesomeshot5051.easyfarms.farm.FarmKind;
import com.awesomeshot5051.easyfarms.upgrade.UpgradeEffect;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentExactPredicate;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.Identifier;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A tool that gives the farm effects by itself, with no card: when the tool in the farm's tool slot has the given data components
 * (for example a mod's "smelter upgrade installed" flag), the farm gets {@code effects} as if the matching cards were installed.
 * Data: {@code data/<ns>/easy_farms/tool_ability/*.json}, so any mod's tool upgrades can be supported with one file.
 * <pre>{
 *   "name": "Smelter upgrade",
 *   "components": {"some_mod:smelter_installed": true},
 *   "unless": [{"some_mod:smelter_toggle": false}, {"some_mod:tool_enabled": false}],
 *   "display_tools": "#my_pack:smelter_tools",
 *   "kinds": ["resource", "plant"],
 *   "effects": [{"type": "easy_farms:smelt", "xp": false}]
 * }</pre>
 *
 * @param name         optional display name (recipe viewers)
 * @param components   data components the tool must carry, with these exact values
 * @param unless       optional: the ability does not apply when the tool matches ANY of these (for a switch that defaults to on, so a tool
 *                     without the component still counts: {@code {"some_mod:toggle": false}} means "unless switched off")
 * @param displayTools optional: tools to show in recipe viewers (they cannot find tools by components); not used when the farm runs
 * @param kinds        farm kinds it applies to (all when omitted)
 * @param effects      what it does; see {@link UpgradeEffect}
 */
public record ToolAbility(Optional<Component> name, DataComponentExactPredicate components, List<DataComponentExactPredicate> unless, Optional<HolderSet<Item>> displayTools,
                          List<FarmKind> kinds, List<UpgradeEffect> effects) {
    public static final ResourceKey<Registry<ToolAbility>> REGISTRY_KEY =
            ResourceKey.createRegistryKey(Identifier.fromNamespaceAndPath(EasyFarms.MODID, "tool_ability"));

    public static final Codec<ToolAbility> CODEC = RecordCodecBuilder.create(i -> i.group(
            ComponentSerialization.CODEC.optionalFieldOf("name").forGetter(ToolAbility::name),
            DataComponentExactPredicate.CODEC.fieldOf("components").forGetter(ToolAbility::components),
            DataComponentExactPredicate.CODEC.listOf().optionalFieldOf("unless", List.of()).forGetter(ToolAbility::unless),
            RegistryCodecs.homogeneousList(Registries.ITEM).optionalFieldOf("display_tools").forGetter(ToolAbility::displayTools),
            FarmKind.CODEC.listOf().optionalFieldOf("kinds", List.of(FarmKind.values())).forGetter(ToolAbility::kinds),
            UpgradeEffect.CODEC.listOf().fieldOf("effects").forGetter(ToolAbility::effects)
    ).apply(i, ToolAbility::new));

    /** True if this ability applies to the tool on a farm of this kind. */
    public boolean appliesTo(FarmKind kind, ItemStack tool) {
        return !tool.isEmpty() && kinds.contains(kind) && components.test(tool) && unless.stream().noneMatch(p -> p.test(tool));
    }

    /** Tools to show in recipe viewers: the listed display tools, each with the required components applied. */
    public List<ItemStack> displayStacks() {
        List<ItemStack> out = new ArrayList<>();
        displayTools.ifPresent(set -> set.forEach(holder -> {
            ItemStack stack = new ItemStack(holder);
            stack.applyComponents(components.asPatch());
            out.add(stack);
        }));
        return out;
    }

    /** The abilities a tool has on this kind of farm (from the synced registry; empty for no tool). */
    public static List<ToolAbility> of(RegistryAccess access, FarmKind kind, ItemStack tool) {
        if (tool.isEmpty()) return List.of();
        List<ToolAbility> list = new ArrayList<>();
        access.lookup(REGISTRY_KEY).ifPresent(registry -> registry.listElements().forEach(holder -> {
            if (holder.value().appliesTo(kind, tool)) list.add(holder.value());
        }));
        return list;
    }
}
