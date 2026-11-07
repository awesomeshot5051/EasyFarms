package com.awesomeshot5051.easyfarms.compat.dimstorage;

import com.awesomeshot5051.easyfarms.farm.network.NetworkImporter;
import com.awesomeshot5051.easyfarms.registry.CardFrequency;
import com.awesomeshot5051.easyfarms.registry.ModDataComponents;
import com.mojang.authlib.GameProfile;
import edivad.dimstorage.api.Frequency;
import edivad.dimstorage.blockentities.BlockEntityDimChest;
import edivad.dimstorage.manager.DimStorageManager;
import edivad.dimstorage.storage.DimChestStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Auto Importer for DimStorage. The card carries a frequency (channel number, public or private) set in its own screen, and the farm
 * stores into the Dimensional Chest storage of that frequency, so every Dim Chest on it shows the items. A card with no frequency set
 * falls back to a Dim Chest next to the farm (using that chest's frequency); a locked chest is refused there.
 */
public final class DimStorageImporter implements NetworkImporter {
    @Override
    public ItemStack insert(ServerLevel level, BlockPos farm, ItemStack card, ItemStack stack) {
        Container storage = findStorage(level, farm, card.get(ModDataComponents.FREQUENCY.get()));
        if (storage == null || stack.isEmpty()) return stack;
        ItemStack rest = stack.copy();
        // Top up matching stacks first, then use empty slots.
        for (int pass = 0; pass < 2 && !rest.isEmpty(); pass++) {
            for (int i = 0; i < storage.getContainerSize() && !rest.isEmpty(); i++) {
                ItemStack slot = storage.getItem(i);
                if (!storage.canPlaceItem(i, rest)) continue;
                if (pass == 0) {
                    if (slot.isEmpty() || !ItemStack.isSameItemSameComponents(slot, rest)) continue;
                    int room = Math.min(slot.getMaxStackSize(), storage.getMaxStackSize()) - slot.getCount();
                    int moved = Math.min(room, rest.getCount());
                    if (moved <= 0) continue;
                    slot.grow(moved);
                    rest.shrink(moved);
                } else if (slot.isEmpty()) {
                    int moved = Math.min(Math.min(rest.getMaxStackSize(), storage.getMaxStackSize()), rest.getCount());
                    storage.setItem(i, rest.copyWithCount(moved));
                    rest.shrink(moved);
                }
            }
        }
        if (rest.getCount() != stack.getCount()) storage.setChanged();
        return rest;
    }

    @Nullable
    private static Container findStorage(ServerLevel level, BlockPos farm, @Nullable CardFrequency card) {
        if (card != null) {
            Optional<GameProfile> owner = card.owner().map(id -> new GameProfile(id, card.ownerName()));
            Frequency frequency = new Frequency(owner, card.channel());
            DimChestStorage storage = (DimChestStorage) DimStorageManager.instance(level).getStorage(level.registryAccess(), frequency, "item");
            return storage;
        }
        for (Direction dir : Direction.values()) {
            if (level.getBlockEntity(farm.relative(dir)) instanceof BlockEntityDimChest chest && !chest.isLocked()) return chest.getStorage();
        }
        return null;
    }
}
