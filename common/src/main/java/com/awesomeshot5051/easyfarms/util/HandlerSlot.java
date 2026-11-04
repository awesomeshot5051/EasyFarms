package com.awesomeshot5051.easyfarms.util;

import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** A menu slot backed by one slot of an {@link ItemStackHandler} (the counterpart of NeoForge's SlotItemHandler). */
public class HandlerSlot extends Slot {
    private final ItemStackHandler handler;
    private final int index;

    public HandlerSlot(ItemStackHandler handler, int index, int x, int y) {
        super(handler, index, x, y);
        this.handler = handler;
        this.index = index;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return !stack.isEmpty() && handler.isItemValid(index, stack);
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return Math.min(getMaxStackSize(), handler.getStackLimit(index, stack));
    }
}
