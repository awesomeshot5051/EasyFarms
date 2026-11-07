package com.awesomeshot5051.easyfarms.compat.ae2;

import appeng.api.implementations.blockentities.IWirelessAccessPoint;
import appeng.api.features.GridLinkables;
import appeng.api.features.IGridLinkableHandler;
import appeng.api.networking.GridHelper;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.storage.StorageHelper;
import com.awesomeshot5051.easyfarms.farm.network.NetworkImporter;
import com.awesomeshot5051.easyfarms.registry.ModDataComponents;
import com.awesomeshot5051.easyfarms.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Auto Importer for Applied Energistics 2: stores the farm's output in the ME network. Two ways in:
 * <ul>
 * <li>a network cable (or other network block) next to the farm;</li>
 * <li>wirelessly, like a wireless terminal: the card is linked to the network (put it in a Wireless Access Point's link slot, as with a
 * terminal, or use it on a network block) and the farm must be within range of an active Wireless Access Point of that network, in the
 * same dimension. Range boosters on the access point extend it.</li>
 * </ul>
 * The farm uses no channel: it only stores items into the network's storage, paying the usual AE power for it.
 */
public final class Ae2Importer implements NetworkImporter {
    @Override
    public boolean canLink(ServerLevel level, BlockPos pos) {
        return GridHelper.getNodeHost(level, pos) != null;
    }

    @Override
    public ItemStack insert(ServerLevel level, BlockPos farm, ItemStack card, ItemStack stack) {
        IGrid grid = findGrid(level, farm, card.get(ModDataComponents.LINK.get()));
        if (grid == null || stack.isEmpty()) return stack;
        AEItemKey key = AEItemKey.of(stack);
        if (key == null) return stack;
        long inserted = StorageHelper.poweredInsert(grid.getEnergyService(), grid.getStorageService().getInventory(), key, stack.getCount(), IActionSource.empty());
        return inserted >= stack.getCount() ? ItemStack.EMPTY : stack.copyWithCount(stack.getCount() - (int) inserted);
    }

    /** Lets the card be linked in a Wireless Access Point's link slot, exactly like a wireless terminal. */
    public static void registerLinkable() {
        GridLinkables.register(ModItems.UPGRADE_CARD.get(), new IGridLinkableHandler() {
            @Override
            public boolean canLink(ItemStack stack) {
                var card = stack.get(ModDataComponents.CARD.get());
                return card != null && card.equals(AE2_CARD);
            }

            @Override
            public void link(ItemStack stack, GlobalPos pos) {
                stack.set(ModDataComponents.LINK.get(), pos);
            }

            @Override
            public void unlink(ItemStack stack) {
                stack.remove(ModDataComponents.LINK.get());
            }
        });
    }

    private static final net.minecraft.resources.ResourceLocation AE2_CARD = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("easy_farms", "ae2_importer");

    @Nullable
    private static IGrid findGrid(ServerLevel level, BlockPos farm, @Nullable GlobalPos link) {
        for (Direction dir : Direction.values()) {
            IGridNode node = GridHelper.getExposedNode(level, farm.relative(dir), dir.getOpposite());
            if (usable(node)) return node.getGrid();
        }
        if (link != null) {
            ServerLevel target = level.getServer().getLevel(link.dimension());
            if (target != null && target.isLoaded(link.pos())) {
                IGrid grid = gridAt(target, link.pos());
                if (grid != null && inWirelessRange(grid, level, farm)) return grid;
            }
        }
        return null;
    }

    /** True if an active Wireless Access Point of the grid reaches the farm (same dimension, within its range: the terminals' rule). */
    private static boolean inWirelessRange(IGrid grid, ServerLevel level, BlockPos farm) {
        // IGrid.getMachines(Class) only finds machines registered under that exact class, so the interface cannot be asked for directly:
        // walk the grid's machine classes and look at the ones that are access points (also finds other mods' access points).
        for (Class<?> machineClass : grid.getMachineClasses()) {
            if (!IWirelessAccessPoint.class.isAssignableFrom(machineClass)) continue;
            for (Object machine : grid.getMachines(machineClass)) {
                IWirelessAccessPoint wap = (IWirelessAccessPoint) machine;
                if (!wap.isActive() || wap.getLocation().getLevel() != level) continue;
                double range = wap.getRange();
                BlockPos at = wap.getLocation().getPos();
                if (at.distSqr(farm) < range * range) return true;
            }
        }
        return false;
    }

    @Nullable
    private static IGrid gridAt(ServerLevel level, BlockPos pos) {
        IInWorldGridNodeHost host = GridHelper.getNodeHost(level, pos);
        if (host == null) return null;
        for (Direction dir : Direction.values()) {
            IGridNode node = host.getGridNode(dir);
            if (usable(node)) return node.getGrid();
        }
        IGridNode node = host.getGridNode(null);
        return usable(node) ? node.getGrid() : null;
    }

    private static boolean usable(@Nullable IGridNode node) {
        return node != null && node.getGrid() != null && node.isActive();
    }
}
