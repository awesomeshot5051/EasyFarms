package com.awesomeshot5051.easyfarms.crafting;

import com.awesomeshot5051.easyfarms.farm.FarmBlockEntity;
import com.awesomeshot5051.easyfarms.util.ItemStackHandler;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The Crafting card's work: after a cycle's drops are rolled, crafts each of the farm's target items from the new drops plus
 * what is already in the output slots, using the first grid that can make it. Ingredients are taken from the drops first, then
 * the output slots; the crafted items (and recipe leftovers) join the drops.
 */
public final class AutoCrafter {
    private AutoCrafter() {
    }

    public static void run(FarmBlockEntity farm, ServerLevel level, List<ResourceLocation> gridIds, int maxCrafts, List<ItemStack> drops) {
        List<ResourceLocation> targets = farm.craftTargets();
        if (targets.isEmpty()) return;
        List<CraftingGrid> grids = CraftingGrid.usable(level.registryAccess(), gridIds);
        if (grids.isEmpty()) return;
        for (ResourceLocation id : targets) {
            Item item = BuiltInRegistries.ITEM.get(id);
            if (item == Items.AIR) continue;
            ItemStack target = new ItemStack(item);
            List<ItemStack> available = pool(farm.outputs(), drops);
            if (available.isEmpty()) return;
            for (CraftingGrid grid : grids) {
                Optional<CraftingGridType.Crafted> crafted = grid.typeInstance().flatMap(t -> t.craft(level, grid, target, available, maxCrafts));
                if (crafted.isEmpty()) continue;
                for (ItemStack used : crafted.get().consumed()) take(farm.outputs(), drops, used);
                for (ItemStack made : crafted.get().results()) addSplit(drops, made);
                break;
            }
        }
    }

    /** Whether any usable grid has a recipe for this item (the targets screen shows a warning if not). */
    public static boolean craftable(ServerLevel level, List<ResourceLocation> gridIds, ResourceLocation id) {
        Item item = BuiltInRegistries.ITEM.get(id);
        if (item == Items.AIR) return false;
        ItemStack target = new ItemStack(item);
        for (CraftingGrid grid : CraftingGrid.usable(level.registryAccess(), gridIds)) {
            if (grid.typeInstance().map(t -> t.canCraft(level, grid, target)).orElse(false)) return true;
        }
        return false;
    }

    /** The drops and the output slots, merged by item and components. */
    private static List<ItemStack> pool(ItemStackHandler outputs, List<ItemStack> drops) {
        List<ItemStack> pool = new ArrayList<>();
        for (ItemStack drop : drops) VanillaCraftingGrid.merge(pool, drop);
        for (int slot = 0; slot < outputs.getSlots(); slot++) VanillaCraftingGrid.merge(pool, outputs.getStackInSlot(slot));
        return pool;
    }

    /** Removes {@code stack}'s worth of items: from the drops first, then the output slots. */
    private static void take(ItemStackHandler outputs, List<ItemStack> drops, ItemStack stack) {
        int left = stack.getCount();
        for (int i = 0; i < drops.size() && left > 0; i++) {
            ItemStack drop = drops.get(i);
            if (!ItemStack.isSameItemSameComponents(drop, stack)) continue;
            int n = Math.min(left, drop.getCount());
            drop.shrink(n);
            left -= n;
        }
        drops.removeIf(ItemStack::isEmpty);
        for (int slot = 0; slot < outputs.getSlots() && left > 0; slot++) {
            ItemStack held = outputs.getStackInSlot(slot);
            if (!ItemStack.isSameItemSameComponents(held, stack)) continue;
            left -= outputs.extractItem(slot, left, false).getCount();
        }
    }

    /** Adds to the drops in stacks no bigger than the item's normal stack size. */
    private static void addSplit(List<ItemStack> drops, ItemStack stack) {
        int max = Math.max(1, stack.getMaxStackSize());
        int left = stack.getCount();
        while (left > 0) {
            int n = Math.min(left, max);
            drops.add(stack.copyWithCount(n));
            left -= n;
        }
    }
}
