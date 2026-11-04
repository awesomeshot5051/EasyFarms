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
    }
}
