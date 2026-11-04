package com.awesomeshot5051.easyfarms.gui;

import com.awesomeshot5051.easyfarms.farm.FarmBlockEntity;
import com.awesomeshot5051.easyfarms.registry.ModItems;
import com.awesomeshot5051.easyfarms.tier.FarmTier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import com.awesomeshot5051.easyfarms.util.HandlerSlot;
import com.awesomeshot5051.easyfarms.util.ItemStackHandler;

/** Main farm GUI: three input slots, the tool slot, four take-only output slots, and a button to the Upgrades screen. */
public class FarmMenu extends AbstractContainerMenu {
    public static final int INPUT_X = 52, INPUT_Y = 20, OUTPUT_Y = 51;
    private static final int TOOL_SLOT = FarmBlockEntity.INPUTS, FIRST_PLAYER = FarmBlockEntity.INPUTS + 1 + FarmBlockEntity.OUTPUTS;

    private final BlockPos pos;
    private final FarmBlockEntity farm;
    private final ContainerData data;

    public FarmMenu(int id, Inventory inventory, BlockPos pos, FarmBlockEntity farm) {
        super(ModMenus.FARM.get(), id);
        this.pos = pos;
        this.farm = farm;
        if (farm != null && inventory.player instanceof net.minecraft.server.level.ServerPlayer) farm.setLastUser(inventory.player.getUUID());
        ItemStackHandler inputs = farm != null ? farm.inputs() : new ItemStackHandler(FarmBlockEntity.INPUTS);
        ItemStackHandler tool = farm != null ? farm.toolSlot() : new ItemStackHandler(1);
        ItemStackHandler outputs = farm != null ? farm.outputs() : new ItemStackHandler(FarmBlockEntity.OUTPUTS);

        for (int i = 0; i < FarmBlockEntity.INPUTS; i++) addSlot(new HandlerSlot(inputs, i, INPUT_X + i * 18, INPUT_Y));
        addSlot(new HandlerSlot(tool, 0, INPUT_X + FarmBlockEntity.INPUTS * 18, INPUT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                // Only things that belong to a tool tier: a tool no tier knows about could never run a recipe.
                return FarmTier.find(inventory.player.level().registryAccess(), stack) != null;
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        for (int i = 0; i < FarmBlockEntity.OUTPUTS; i++) {
            addSlot(new HandlerSlot(outputs, i, INPUT_X + i * 18, OUTPUT_Y) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }
            });
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 82 + row * 18));
        }
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 8 + col * 18, 140));

        this.data = farm != null ? new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> (int) farm.getTimer();
                    case 1 -> (int) Math.ceil(farm.cycleTicks());
                    case 2 -> (int) (farm.lifetimeLeftTicks() / 24000L);
                    case 3 -> farm.hasInfiniteInputs() ? 1 : 0;
                    case 4 -> farm.isRunning() ? 1 : 0;
                    case 6 -> farm.usesTool() ? 1 : 0;
                    default -> farm.environmentOk() ? 1 : 0;
                };
            }

            @Override
            public void set(int index, int value) {
            }

            @Override
            public int getCount() {
                return 7;
            }
        } : new SimpleContainerData(7);
        addDataSlots(data);
    }

    public int progress() {
        return data.get(0);
    }

    public int cycle() {
        return data.get(1);
    }

    public int daysLeft() {
        return data.get(2);
    }

    public boolean usesTool() {
        return data.get(6) == 1;
    }

    public boolean infinite() {
        return data.get(3) == 1;
    }

    public boolean environmentOk() {
        return data.get(5) == 1;
    }

    public boolean running() {
        return data.get(4) == 1;
    }

    /** Button 0: open the Upgrades screen. */
    @Override
    public boolean clickMenuButton(Player player, int button) {
        if (button == 0 && farm != null && player instanceof ServerPlayer server) {
            MenuOpener.openUpgrades(server, pos, farm, Component.translatable("gui.easy_farms.upgrades"));
            return true;
        }
        return false;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index < FIRST_PLAYER) {
            if (!moveItemStackTo(stack, FIRST_PLAYER, slots.size(), true)) return ItemStack.EMPTY;
        } else {
            // Try the tool slot first (it only accepts tier tools), then the two inputs.
            if (!moveItemStackTo(stack, TOOL_SLOT, TOOL_SLOT + 1, false) && !moveItemStackTo(stack, 0, FarmBlockEntity.INPUTS, false)) return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.level().getBlockEntity(pos) instanceof FarmBlockEntity && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64;
    }
}
