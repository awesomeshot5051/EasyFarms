package com.awesomeshot5051.easyfarms.gui;

import com.awesomeshot5051.easyfarms.farm.FarmBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * The Filter screen (opened from the Filter card's arrow on the farm screen). It has no item slots: it lists every item the
 * farm's current recipe can output, and a click blocks or allows that item. The list is fixed when the screen opens (it is sent
 * along with the open request); which entries are blocked is synced as bits (16 per data slot).
 */
public class FarmFilterMenu extends AbstractContainerMenu {
    /** 3 data slots x 16 bits. */
    public static final int MAX_ENTRIES = 48;

    private final BlockPos pos;
    private final FarmBlockEntity farm;
    private final List<Identifier> items;
    private final ContainerData data;

    /** Client side: {@code farm} is null and the blocked bits arrive through the data slots. */
    public FarmFilterMenu(int id, Inventory inventory, BlockPos pos, FarmBlockEntity farm, List<Identifier> items) {
        super(ModMenus.FARM_FILTER.get(), id);
        this.pos = pos;
        this.farm = farm;
        this.items = items;
        this.data = farm != null ? new ContainerData() {
            @Override
            public int get(int index) {
                int bits = 0;
                for (int b = 0; b < 16; b++) {
                    int entry = index * 16 + b;
                    if (entry < FarmFilterMenu.this.items.size() && farm.isOutputBlocked(FarmFilterMenu.this.items.get(entry))) bits |= 1 << b;
                }
                return bits;
            }

            @Override
            public void set(int index, int value) {
            }

            @Override
            public int getCount() {
                return 3;
            }
        } : new SimpleContainerData(3);
        addDataSlots(data);
    }

    public List<Identifier> items() {
        return items;
    }

    public boolean isBlocked(int entry) {
        return (data.get(entry / 16) >> (entry % 16) & 1) != 0;
    }

    /** Button 0: back to the farm. Button n (n >= 1): block or allow the n-th item. */
    @Override
    public boolean clickMenuButton(Player player, int button) {
        if (farm == null || !(player instanceof ServerPlayer server)) return false;
        if (button == 0) {
            MenuOpener.openFarm(server, pos, farm, Component.translatable(farm.getBlockState().getBlock().getDescriptionId()));
            return true;
        }
        int entry = button - 1;
        if (entry < items.size()) {
            farm.toggleOutputBlocked(items.get(entry));
            return true;
        }
        return false;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.level().getBlockEntity(pos) instanceof FarmBlockEntity && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64;
    }
}
