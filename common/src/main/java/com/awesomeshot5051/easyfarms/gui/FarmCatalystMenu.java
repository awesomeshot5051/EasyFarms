package com.awesomeshot5051.easyfarms.gui;

import com.awesomeshot5051.easyfarms.farm.FarmBlockEntity;
import com.awesomeshot5051.easyfarms.tier.FarmUpgrade;
import com.awesomeshot5051.easyfarms.util.HandlerSlot;
import com.awesomeshot5051.easyfarms.util.ItemStackHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * The Catalysts screen, opened from the arrow under the Upgrades arrow (which only exists while an installed card needs a
 * catalyst). A card gets one slot per catalyst ingredient it uses, out of 18 slots shared between all cards, and each slot
 * takes only that ingredient. Slots are shown grouped by card, in card-slot order. The slots are fixed when the screen opens
 * ({@code owners}: for each of the farm's catalyst slots, the upgrade slot whose card owns it, or -1).
 */
public class FarmCatalystMenu extends AbstractContainerMenu {
    private final BlockPos pos;
    private final FarmBlockEntity farm;
    private final net.minecraft.world.level.Level level;
    /** For each menu slot: the farm's catalyst slot it shows, the upgrade slot of its card, and which of the card's ingredients it takes. */
    private final List<int[]> entries = new ArrayList<>();

    /** Client side ({@code farm} null): the contents arrive through normal slot syncing. */
    public FarmCatalystMenu(int id, Inventory inventory, BlockPos pos, FarmBlockEntity farm, int[] owners) {
        super(ModMenus.FARM_CATALYSTS.get(), id);
        this.pos = pos;
        this.farm = farm;
        this.level = inventory.player.level();
        ItemStackHandler handler = farm != null ? farm.catalysts() : new ItemStackHandler(FarmBlockEntity.UPGRADE_SLOTS);

        for (int owner = 0; owner < FarmBlockEntity.UPGRADE_SLOTS; owner++) {
            int sub = 0;
            for (int h = 0; h < owners.length; h++) {
                if (owners[h] != owner) continue;
                final int catalystSlot = h;
                int k = entries.size();
                entries.add(new int[]{h, owner, sub++});
                addSlot(new HandlerSlot(handler, h, 8 + (k % 9) * 18, 18 + (k / 9) * 18) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        FarmBlockEntity entity = farm != null ? farm : level.getBlockEntity(pos) instanceof FarmBlockEntity be ? be : null;
                        return entity != null && entity.isCatalystValid(catalystSlot, stack);
                    }
                });
            }
        }
        int top = playerInventoryTop();
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, top + row * 18));
        }
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 8 + col * 18, top + 58));
    }

    public int catalystSlotCount() {
        return entries.size();
    }

    public int rows() {
        return Math.max(1, (int) Math.ceil(entries.size() / 9.0));
    }

    /** Y of the first player inventory row, as in a vanilla chest GUI with this many rows. */
    public int playerInventoryTop() {
        return 31 + rows() * 18;
    }

    /** What the empty catalyst slot {@code k} tells the player: the card it belongs to and the item it takes. */
    public List<Component> hint(int k) {
        int[] entry = entries.get(k);
        if (!(level.getBlockEntity(pos) instanceof FarmBlockEntity entity)) return List.of();
        FarmUpgrade card = FarmUpgrade.find(level.registryAccess(), entity.upgrades().getStackInSlot(entry[1]));
        List<String> items = entity.catalystItemsFor(entry[1]);
        if (card == null || entry[2] >= items.size()) return List.of();
        return List.of(Component.translatable("gui.easy_farms.catalyst_slot", card.displayName()),
                Component.translatable("gui.easy_farms.catalyst_accepts", com.awesomeshot5051.easyfarms.upgrade.UpgradeRules.itemName(items.get(entry[2]))));
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
        int firstPlayer = catalystSlotCount();
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
}
