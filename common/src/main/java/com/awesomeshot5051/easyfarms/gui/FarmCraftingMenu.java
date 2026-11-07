package com.awesomeshot5051.easyfarms.gui;

import com.awesomeshot5051.easyfarms.crafting.AutoCrafter;
import com.awesomeshot5051.easyfarms.farm.FarmBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * The Crafting card's screen: {@link FarmBlockEntity#CRAFT_TARGETS} ghost slots for the items the farm should craft. Click a slot
 * with an item on the cursor to set it, right-click (or click with an empty hand) to clear, or shift-click an inventory item to
 * put it in the first free slot. Nothing is ever moved: the slots only remember which item. The server tells the screen which
 * targets have a recipe in any usable grid (2 bits each: 0 empty, 1 craftable, 2 no recipe).
 */
public class FarmCraftingMenu extends AbstractContainerMenu {
    public static final int TARGET_X = 35, TARGET_Y = 30;

    private final BlockPos pos;
    private final FarmBlockEntity farm;
    private final SimpleContainer targets = new SimpleContainer(FarmBlockEntity.CRAFT_TARGETS);
    private final ContainerData data;
    private int status;

    /** Client side: {@code farm} is null; the status arrives through the data slot. */
    public FarmCraftingMenu(int id, Inventory inventory, BlockPos pos, FarmBlockEntity farm) {
        super(ModMenus.FARM_CRAFTING.get(), id);
        this.pos = pos;
        this.farm = farm;
        for (int i = 0; i < FarmBlockEntity.CRAFT_TARGETS; i++) addSlot(new GhostSlot(targets, i, TARGET_X + i * 18, TARGET_Y));
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
        }
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 8 + col * 18, 142));
        this.data = farm != null ? new ContainerData() {
            @Override
            public int get(int index) {
                return status;
            }

            @Override
            public void set(int index, int value) {
            }

            @Override
            public int getCount() {
                return 1;
            }
        } : new SimpleContainerData(1);
        addDataSlots(data);
        if (farm != null) load();
    }

    /** 0 = no target, 1 = a usable grid has a recipe for it, 2 = none does. */
    public int statusOf(int target) {
        return (data.get(0) >> (target * 2)) & 3;
    }

    /** Copies the farm's targets into the slots and recomputes which have a recipe (server side). */
    private void load() {
        List<ResourceLocation> ids = farm.craftTargets();
        for (int i = 0; i < FarmBlockEntity.CRAFT_TARGETS; i++) {
            targets.setItem(i, i < ids.size() ? new ItemStack(BuiltInRegistries.ITEM.get(ids.get(i))) : ItemStack.EMPTY);
        }
        int bits = 0;
        if (farm.getLevel() instanceof ServerLevel level) {
            var gridIds = List.<ResourceLocation>of();
            for (int i = 0; i < ids.size(); i++) {
                bits |= (AutoCrafter.craftable(level, gridIds, ids.get(i)) ? 1 : 2) << (i * 2);
            }
        }
        status = bits;
    }

    /** Writes the slots to the farm, then reloads (the farm drops duplicates and air). */
    private void save() {
        if (farm == null) return;
        List<ResourceLocation> ids = new ArrayList<>();
        for (int i = 0; i < FarmBlockEntity.CRAFT_TARGETS; i++) {
            ItemStack stack = targets.getItem(i);
            if (!stack.isEmpty()) ids.add(BuiltInRegistries.ITEM.getKey(stack.getItem()));
        }
        farm.setCraftTargets(ids);
        load();
    }

    @Override
    public void clicked(int slotIndex, int button, ClickType input, Player player) {
        if (slotIndex >= 0 && slotIndex < FarmBlockEntity.CRAFT_TARGETS) {
            ItemStack carried = getCarried();
            targets.setItem(slotIndex, carried.isEmpty() || button == 1 ? ItemStack.EMPTY : carried.copyWithCount(1));
            save();
            return;
        }
        super.clicked(slotIndex, button, input, player);
    }

    /** Shift-click: remember the item in the first free target slot. */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index >= FarmBlockEntity.CRAFT_TARGETS && index < slots.size()) {
            ItemStack stack = slots.get(index).getItem();
            if (!stack.isEmpty()) {
                for (int i = 0; i < FarmBlockEntity.CRAFT_TARGETS; i++) {
                    if (ItemStack.isSameItem(targets.getItem(i), stack)) break;
                    if (targets.getItem(i).isEmpty()) {
                        targets.setItem(i, stack.copyWithCount(1));
                        save();
                        break;
                    }
                }
            }
        }
        return ItemStack.EMPTY;
    }

    /** Button 0: back to the farm. */
    @Override
    public boolean clickMenuButton(Player player, int button) {
        if (button == 0 && farm != null && player instanceof ServerPlayer server) {
            MenuOpener.openFarm(server, pos, farm, Component.translatable(farm.getBlockState().getBlock().getDescriptionId()));
            return true;
        }
        return false;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.level().getBlockEntity(pos) instanceof FarmBlockEntity && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64;
    }

    /** A slot that shows an item but never holds or gives one. */
    private static class GhostSlot extends Slot {
        GhostSlot(SimpleContainer container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public boolean mayPickup(Player player) {
            return false;
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }
}
