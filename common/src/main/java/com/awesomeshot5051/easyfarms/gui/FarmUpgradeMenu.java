package com.awesomeshot5051.easyfarms.gui;

import com.awesomeshot5051.easyfarms.farm.FarmBlockEntity;
import com.awesomeshot5051.easyfarms.farm.FarmKind;
import com.awesomeshot5051.easyfarms.tier.FarmUpgrade;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import com.awesomeshot5051.easyfarms.util.HandlerSlot;
import com.awesomeshot5051.easyfarms.util.ItemStackHandler;

import java.util.List;

/** The Upgrades sub-GUI: one slot per upgrade card that applies to this kind of farm, each taking only its own card. */
public class FarmUpgradeMenu extends AbstractContainerMenu {
    private final BlockPos pos;
    private final FarmBlockEntity farm;
    private final FarmKind kind;
    private final net.minecraft.world.level.Level level;
    private final List<FarmUpgrade> upgrades;

    public FarmUpgradeMenu(int id, Inventory inventory, BlockPos pos, FarmBlockEntity farm) {
        this(id, inventory, pos, farm, farm.kind());
    }

    /** Client side ({@code farm} null): the contents arrive through normal slot syncing. */
    public FarmUpgradeMenu(int id, Inventory inventory, BlockPos pos, FarmBlockEntity farm, FarmKind kind) {
        super(ModMenus.FARM_UPGRADES.get(), id);
        this.pos = pos;
        this.farm = farm;
        if (farm != null && inventory.player instanceof net.minecraft.server.level.ServerPlayer) farm.setLastUser(inventory.player.getUUID());
        this.kind = kind;
        this.level = inventory.player.level();
        this.upgrades = FarmUpgrade.forKind(inventory.player.level().registryAccess(), kind);
        ItemStackHandler handler = farm != null ? farm.upgrades() : new ItemStackHandler(FarmBlockEntity.UPGRADE_SLOTS);

        int count = FarmBlockEntity.UPGRADE_SLOTS;
        for (int i = 0; i < count; i++) {
            final int index = i;
            addSlot(new HandlerSlot(handler, i, 8 + (i % 9) * 18, 18 + (i / 9) * 18) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    // Any card this farm kind accepts, in any free slot, once. The server also checks the farm's other slots.
                    if (farm != null) return farm.canInstall(stack, index);
                    return upgrades.stream().anyMatch(u -> u.matches(stack)) && restrictionProblem(stack, index) == null;
                }

                @Override
                public int getMaxStackSize() {
                    return 1;
                }
            });
        }
        if (hasPressSlot()) {
            int p = count;
            addSlot(new HandlerSlot(farm != null ? farm.pressSlot() : new ItemStackHandler(1), 0, 8 + (p % 9) * 18, 18 + (p / 9) * 18) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return stack.is(FarmBlockEntity.PRESS_TAG);
                }

                @Override
                public int getMaxStackSize() {
                    return 1;
                }
            });
        }
        int top = playerInventoryTop();
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, top + row * 18));
        }
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 8 + col * 18, top + 58));
    }

    public int rows() {
        return Math.max(1, (int) Math.ceil((FarmBlockEntity.UPGRADE_SLOTS + (hasPressSlot() ? 1 : 0)) / 9.0));
    }

    /** Y of the first player inventory row, as in a vanilla chest GUI with this many rows. */
    public int playerInventoryTop() {
        return 31 + rows() * 18;
    }

    /**
     * Why a card's restrictions (exclusive cards, required cards, tool enchantments) forbid it in this slot, or null.
     * Reads the cards from the menu's own slots, so it is right on the client while the screen is open.
     */
    public Component restrictionProblem(ItemStack stack, int slot) {
        FarmUpgrade upgrade = FarmUpgrade.find(level.registryAccess(), stack);
        FarmBlockEntity entity = farm != null ? farm : level.getBlockEntity(pos) instanceof FarmBlockEntity be ? be : null;
        if (upgrade == null || entity == null) return null;
        List<FarmUpgrade> installed = new java.util.ArrayList<>();
        for (int i = 0; i < FarmBlockEntity.UPGRADE_SLOTS; i++) {
            if (i == slot) continue;
            FarmUpgrade other = FarmUpgrade.find(level.registryAccess(), slots.get(i).getItem());
            if (other != null) installed.add(other);
        }
        return com.awesomeshot5051.easyfarms.upgrade.UpgradeRules.installProblem(entity.rulesContext(), upgrade, installed);
    }

    public List<FarmUpgrade> upgrades() {
        return upgrades;
    }

    /** The Resource Farm has one more slot after the cards: the press for the Inscriber card. */
    public boolean hasPressSlot() {
        return kind == FarmKind.RESOURCE;
    }

    public int upgradeSlotCount() {
        return FarmBlockEntity.UPGRADE_SLOTS;
    }

    /** Button 0: back to the main farm GUI. */
    @Override
    public boolean clickMenuButton(Player player, int button) {
        if (button == 0 && farm != null && player instanceof ServerPlayer server) {
            MenuOpener.openFarm(server, pos, farm, Component.translatable(farm.getBlockState().getBlock().getDescriptionId()));
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
        int firstPlayer = upgradeSlotCount();
        if (index < firstPlayer) {
            if (!moveItemStackTo(stack, firstPlayer, slots.size(), true)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, 0, firstPlayer, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.level().getBlockEntity(pos) instanceof FarmBlockEntity && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64;
    }

    public FarmKind kind() {
        return kind;
    }
}
