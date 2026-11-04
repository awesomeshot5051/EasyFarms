package com.awesomeshot5051.easyfarms.client;

import com.awesomeshot5051.easyfarms.tier.FarmUpgrade;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Any item that is an upgrade card (built in or from a datapack) says which farms take it and what its recipe needs. */
public final class CardTooltips {
    private CardTooltips() {
    }

    /** Appends the card lines to {@code tooltip}; the loader calls this from its item tooltip event. */
    public static void addTo(ItemStack stack, List<Component> tooltip) {
        var level = Minecraft.getInstance().level;
        if (level == null || stack.isEmpty()) return;
        FarmUpgrade upgrade = FarmUpgrade.find(level.registryAccess(), stack);
        if (upgrade == null) return;
        upgrade.description().ifPresent(d -> tooltip.add(d.copy().withStyle(ChatFormatting.GRAY)));
        List<Component> kinds = upgrade.kinds().stream()
                .map(k -> (Component) Component.translatable("farm_kind.easy_farms." + k.getSerializedName())).toList();
        tooltip.add(Component.translatable("tooltip.easy_farms.card_kinds", ComponentUtils.formatList(kinds, Component.literal(", "))).withStyle(ChatFormatting.GRAY));
        if (!upgrade.requires().equals("any")) {
            tooltip.add(Component.translatable("gui.easy_farms.requires." + upgrade.requires()).withStyle(ChatFormatting.DARK_GRAY));
        }
        List<String> catalyst = upgrade.catalystItems();
        if (!catalyst.isEmpty()) {
            List<Component> names = catalyst.stream().map(com.awesomeshot5051.easyfarms.upgrade.UpgradeRules::itemName).toList();
            tooltip.add(Component.translatable("tooltip.easy_farms.card_catalyst", ComponentUtils.formatList(names, Component.literal(", "))).withStyle(ChatFormatting.AQUA));
        }
        addRestrictions(upgrade, level.registryAccess(), tooltip);
    }

    private static void addRestrictions(FarmUpgrade upgrade, net.minecraft.core.RegistryAccess access, List<Component> tooltip) {
        for (var r : upgrade.restrictions()) {
            if (r.requires().isEmpty() && r.excludes().isEmpty() && r.excludesEnchantments().isEmpty() && r.requiresBlockBelow().isEmpty()) continue;
            if (!r.whenDrops().isEmpty()) {
                List<Component> drops = r.whenDrops().stream().map(CardTooltips::dropName).toList();
                tooltip.add(Component.translatable("tooltip.easy_farms.card_when", ComponentUtils.formatList(drops, Component.literal(", "))).withStyle(ChatFormatting.DARK_GRAY));
            }
            if (!r.requiresBlockBelow().isEmpty()) {
                List<Component> names = r.requiresBlockBelow().stream().map(com.awesomeshot5051.easyfarms.upgrade.UpgradeRules::blockName).toList();
                tooltip.add(Component.translatable("gui.easy_farms.restriction.requires_block_below",
                        ComponentUtils.formatList(names, Component.literal(", "))).withStyle(ChatFormatting.YELLOW));
            }
            for (var id : r.requires()) {
                FarmUpgrade other = FarmUpgrade.byId(access, id);
                tooltip.add(Component.translatable("gui.easy_farms.restriction.requires", other != null ? other.displayName() : Component.literal(id.toString())).withStyle(ChatFormatting.YELLOW));
            }
            for (var id : r.excludes()) {
                FarmUpgrade other = FarmUpgrade.byId(access, id);
                tooltip.add(Component.translatable("gui.easy_farms.restriction.excludes", other != null ? other.displayName() : Component.literal(id.toString())).withStyle(ChatFormatting.RED));
            }
            for (var id : r.excludesEnchantments()) {
                tooltip.add(Component.translatable("gui.easy_farms.restriction.excludes_enchantment",
                        Component.translatable(net.minecraft.util.Util.makeDescriptionId("enchantment", id))).withStyle(ChatFormatting.RED));
            }
        }
    }

    private static Component dropName(String scope) {
        if (scope.startsWith("#")) return Component.literal(scope);
        var id = net.minecraft.resources.Identifier.tryParse(scope);
        return id == null ? Component.literal(scope) : new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(id)).getHoverName();
    }
}
