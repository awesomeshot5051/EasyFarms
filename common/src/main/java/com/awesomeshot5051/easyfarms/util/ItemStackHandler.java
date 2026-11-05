package com.awesomeshot5051.easyfarms.util;

import net.minecraft.core.NonNullList;
import net.minecraft.world.ItemStackWithSlot;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * A fixed-size stack inventory with the semantics the farms rely on (per-slot validity and stack limit, simulated
 * insert / extract, change callback) and the same NBT layout NeoForge's ItemStackHandler uses. It is also a vanilla
 * {@link Container}, so menus, Fabric's transfer API and hoppers can all work with it.
 */
public class ItemStackHandler implements Container {
    protected NonNullList<ItemStack> stacks;

    public ItemStackHandler(int size) {
        this.stacks = NonNullList.withSize(size, ItemStack.EMPTY);
    }

    // ---- hooks ----

    public boolean isItemValid(int slot, ItemStack stack) {
        return true;
    }

    protected int getStackLimit(int slot, ItemStack stack) {
        return Math.min(getSlotLimit(slot), stack.getMaxStackSize());
    }

    public int getSlotLimit(int slot) {
        return 64;
    }

    protected void onContentsChanged(int slot) {
    }

    // ---- handler API ----

    public int getSlots() {
        return stacks.size();
    }

    public ItemStack getStackInSlot(int slot) {
        return stacks.get(slot);
    }

    public void setStackInSlot(int slot, ItemStack stack) {
        stacks.set(slot, stack);
        onContentsChanged(slot);
    }

    /** Inserts as much as fits; returns the remainder. */
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        if (!isItemValid(slot, stack)) return stack;
        ItemStack existing = stacks.get(slot);
        int limit = getStackLimit(slot, stack);
        if (!existing.isEmpty()) {
            if (!ItemStack.isSameItemSameComponents(stack, existing)) return stack;
            limit -= existing.getCount();
        }
        if (limit <= 0) return stack;
        boolean reachedLimit = stack.getCount() > limit;
        if (!simulate) {
            if (existing.isEmpty()) {
                stacks.set(slot, reachedLimit ? stack.copyWithCount(limit) : stack.copy());
            } else {
                existing.grow(reachedLimit ? limit : stack.getCount());
            }
            onContentsChanged(slot);
        }
        return reachedLimit ? stack.copyWithCount(stack.getCount() - limit) : ItemStack.EMPTY;
    }

    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (amount == 0) return ItemStack.EMPTY;
        ItemStack existing = stacks.get(slot);
        if (existing.isEmpty()) return ItemStack.EMPTY;
        int toExtract = Math.min(amount, existing.getMaxStackSize());
        if (existing.getCount() <= toExtract) {
            if (!simulate) {
                stacks.set(slot, ItemStack.EMPTY);
                onContentsChanged(slot);
                return existing;
            }
            return existing.copy();
        }
        if (!simulate) {
            stacks.set(slot, existing.copyWithCount(existing.getCount() - toExtract));
            onContentsChanged(slot);
        }
        return existing.copyWithCount(toExtract);
    }

    /** Resizes the handler, keeping the stacks that still fit. */
    public void setSize(int size) {
        NonNullList<ItemStack> resized = NonNullList.withSize(size, ItemStack.EMPTY);
        for (int i = 0; i < Math.min(size, stacks.size()); i++) resized.set(i, stacks.get(i));
        stacks = resized;
    }

    /** Inserts into the first slots that take the stack; returns what did not fit. */
    public ItemStack insertItem(ItemStack stack, boolean simulate) {
        ItemStack rest = stack;
        for (int i = 0; i < getSlots() && !rest.isEmpty(); i++) rest = insertItem(i, rest, simulate);
        return rest;
    }

    // ---- persistence (same layout as NeoForge: {Size, Items:[{Slot, ...stack}]}) ----

    public void serialize(ValueOutput output) {
        ValueOutput.TypedOutputList<ItemStackWithSlot> list = output.list("Items", ItemStackWithSlot.CODEC);
        java.util.List<Integer> big = new java.util.ArrayList<>();
        for (int i = 0; i < stacks.size(); i++) {
            ItemStack stack = stacks.get(i);
            if (stack.isEmpty()) continue;
            // ItemStack's codec only allows counts up to 99; bigger stacks (Capacity card) keep the real count beside it.
            list.add(new ItemStackWithSlot(i, stack.copyWithCount(Math.min(stack.getCount(), 99))));
            if (stack.getCount() > 99) {
                big.add(i);
                big.add(stack.getCount());
            }
        }
        output.putInt("Size", stacks.size());
        if (!big.isEmpty()) output.putIntArray("BigCounts", big.stream().mapToInt(Integer::intValue).toArray());
    }

    public void deserialize(ValueInput input) {
        stacks = NonNullList.withSize(input.getIntOr("Size", stacks.size()), ItemStack.EMPTY);
        input.listOrEmpty("Items", ItemStackWithSlot.CODEC).forEach(entry -> {
            if (entry.isValidInContainer(stacks.size())) stacks.set(entry.slot(), entry.stack());
        });
        // Counts above 99 (see serialize) are stored as slot, count pairs.
        int[] big = input.getIntArray("BigCounts").orElse(new int[0]);
        for (int i = 0; i + 1 < big.length; i += 2) {
            if (big[i] >= 0 && big[i] < stacks.size() && !stacks.get(big[i]).isEmpty()) stacks.get(big[i]).setCount(big[i + 1]);
        }
    }

    // ---- vanilla Container view ----

    @Override
    public int getContainerSize() {
        return stacks.size();
    }

    @Override
    public boolean isEmpty() {
        return stacks.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getItem(int slot) {
        return stacks.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        return extractItem(slot, amount, false);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack stack = stacks.get(slot);
        stacks.set(slot, ItemStack.EMPTY);
        return stack;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        setStackInSlot(slot, stack);
    }

    @Override
    public int getMaxStackSize() {
        return 64;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return isItemValid(slot, stack);
    }

    @Override
    public void setChanged() {
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < stacks.size(); i++) stacks.set(i, ItemStack.EMPTY);
    }
}
