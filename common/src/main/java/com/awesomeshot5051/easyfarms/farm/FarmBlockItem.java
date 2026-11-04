package com.awesomeshot5051.easyfarms.farm;

import com.awesomeshot5051.easyfarms.registry.ModDataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

import java.util.function.Consumer;

/** The farm's item: shows what a Silk Touch pickup carries. */
public class FarmBlockItem extends BlockItem {
    public FarmBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        FarmContents contents = stack.get(ModDataComponents.FARM_CONTENTS.get());
        if (contents != null) tooltip.accept(Component.translatable("tooltip.easy_farms.contents", contents.stackCount()));
    }
}
