package com.awesomeshot5051.easyfarms.gui;

import com.awesomeshot5051.easyfarms.farm.FarmBlockEntity;
import com.awesomeshot5051.easyfarms.tier.FarmUpgrade;
import com.awesomeshot5051.easyfarms.util.HandlerSlot;
import com.awesomeshot5051.easyfarms.util.ItemStackHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
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
 * catalyst). It has one slot per such card, in the order of the card slots, and each slot takes only the items its card
 * asks for. The slots are fixed when the screen opens ({@code mask}: bit i = the card in upgrade slot i needs one).
 */
public class FarmCatalystMenu extends AbstractContainerMenu {
    private final BlockPos pos;
    private final FarmBlockEntity farm;
    private final net.minecraft.world.level.Level level;
    /** The upgrade-slot index each catalyst slot belongs to. */
    private final List<Integer> cardSlots = new ArrayList<>();

    /** Client side ({@code farm} null): the contents arrive through normal slot syncing. */
    public FarmCatalystMenu(int id, Inventory inventory, BlockPos pos, FarmBlockEntity farm, int mask) {
        super(ModMenus.FARM_CATALYSTS.get(), id);
        this.pos = pos;
        this.farm = farm;
        this.level = inventory.player.level();
        ItemStackHandler handler = farm != null ? farm.catalysts() : new ItemStackHandler(FarmBlockEntity.UPGRADE_SLOTS);

        for (int i = 0; i < FarmBlockEntity.UPGRADE_SLOTS; i++) {
            if ((mask >> i & 1) == 0) continue;
            final int cardSlot = i;
            int k = cardSlots.size();
            cardSlots.add(i);
            addSlot(new HandlerSlot(handler, i, 8 + (k % 9) * 18, 18 + (k / 9) * 18) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    FarmBlockEntity entity = farm != null ? farm : level.getBlockEntity(pos) instanceof FarmBlockEntity be ? be : null;
                    return entity != null && entity.isCatalystValid(cardSlot, stack);
                }
            });
        }
        int top = playerInventoryTop();
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, top + row * 18));
        }
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 8 + col * 18, top + 58));
    }

    public int catalystSlotCount() {
        return cardSlots.size();
    }

    public int rows() {
        return Math.max(1, (int) Math.ceil(cardSlots.size() / 9.0));
    }

    /** Y of the first player inventory row, as in a vanilla chest GUI with this many rows. */
    public int playerInventoryTop() {
        return 31 + rows() * 18;
    }

    /** What the empty catalyst slot {@code k} tells the player: the card it belongs to and what it takes. */
    public List<Component> hint(int k) {
        int cardSlot = cardSlots.get(k);
        if (!(level.getBlockEntity(pos) instanceof FarmBlockEntity entity)) return List.of();
        FarmUpgrade card = FarmUpgrade.find(level.registryAccess(), entity.upgrades().getStackInSlot(cardSlot));
        if (card == null) return List.of();
        List<Component> items = entity.catalystItemsFor(cardSlot).stream().map(com.awesomeshot5051.easyfarms.upgrade.UpgradeRules::itemName).toList();
        return List.of(Component.translatable("gui.easy_farms.catalyst_slot", card.displayName()),
                Component.translatable("gui.easy_farms.catalyst_accepts", ComponentUtils.formatList(items, Component.literal(", "))));
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
