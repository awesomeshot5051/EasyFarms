package com.awesomeshot5051.easyfarms.events;

import com.awesomeshot5051.easyfarms.farm.FarmBlock;
import com.awesomeshot5051.easyfarms.farm.FarmBlockEntity;
import com.awesomeshot5051.easyfarms.tier.FarmUpgrade;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;

/**
 * The old farms' special interactions: shift-use with a card or dye, and dispensers with dye.
 * The loaders hook {@link #allowsSneakUse} into their right-click event and call {@link #registerDispenserBehaviors()}.
 */
public final class FarmEvents {
    private FarmEvents() {
    }

    /**
     * Vanilla skips a block's use when you sneak with an item in hand. Cards and dyes should still work while sneaking,
     * so the farm is allowed to handle them (the old farms did the same for an empty hand).
     */
    public static boolean allowsSneakUse(Level level, BlockPos pos, ItemStack held) {
        if (!(level.getBlockState(pos).getBlock() instanceof FarmBlock)) return false;
        return held.getItem() instanceof DyeItem || FarmUpgrade.isCard(level.registryAccess(), held);
    }

    public static void registerDispenserBehaviors() {
        for (DyeColor color : DyeColor.values()) {
            DispenserBlock.registerBehavior(DyeItem.byColor(color), new DyeDispenseBehavior());
        }
    }

    /** A dispenser facing a farm with dye in it recolours the farm. Otherwise it dispenses the dye as usual. */
    static class DyeDispenseBehavior extends DefaultDispenseItemBehavior {
        @Override
        protected ItemStack execute(BlockSource source, ItemStack stack) {
            Direction facing = source.state().getValue(DispenserBlock.FACING);
            BlockPos target = source.pos().relative(facing);
            if (stack.getItem() instanceof DyeItem dye && source.level().getBlockEntity(target) instanceof FarmBlockEntity farm && farm.canDye()) {
                farm.setColor(dye.getDyeColor());
                stack.shrink(1);
                return stack;
            }
            return super.execute(source, stack);
        }
    }
}
