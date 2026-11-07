package com.awesomeshot5051.easyfarms.compat.refinedstorage;

import com.awesomeshot5051.easyfarms.farm.network.NetworkImporter;
import com.awesomeshot5051.easyfarms.registry.ModDataComponents;
import com.refinedmods.refinedstorage.api.core.Action;
import com.refinedmods.refinedstorage.api.network.Network;
import com.refinedmods.refinedstorage.api.network.node.GraphNetworkComponent;
import com.refinedmods.refinedstorage.api.network.storage.StorageNetworkComponent;
import com.refinedmods.refinedstorage.api.storage.Actor;
import com.refinedmods.refinedstorage.common.api.support.network.InWorldNetworkNodeContainer;
import com.refinedmods.refinedstorage.common.api.support.network.NetworkNodeContainerProvider;
import com.refinedmods.refinedstorage.common.api.support.network.item.NetworkItemPlayerValidator;
import com.refinedmods.refinedstorage.common.support.resource.ItemResource;
import com.refinedmods.refinedstorage.neoforge.api.RefinedStorageNeoForgeApi;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Auto Importer for Refined Storage: stores the farm's output in the network's storage. The network is found from the card's link
 * (any block of the network, such as a cable) or from a network block next to the farm.
 */
public final class RefinedStorageImporter implements NetworkImporter {
    @Override
    public boolean canLink(ServerLevel level, BlockPos pos) {
        if (storageAt(level, pos, null, null) != null) return true;
        for (Direction dir : Direction.values()) if (storageAt(level, pos, dir, null) != null) return true;
        return false;
    }

    @Override
    public ItemStack insert(ServerLevel level, BlockPos farm, ItemStack card, ItemStack stack) {
        StorageNetworkComponent storage = findStorage(level, farm, card.get(ModDataComponents.LINK.get()));
        if (storage == null || stack.isEmpty()) return stack;
        long inserted = storage.insert(ItemResource.ofItemStack(stack), stack.getCount(), Action.EXECUTE, Actor.EMPTY);
        return inserted >= stack.getCount() ? ItemStack.EMPTY : stack.copyWithCount(stack.getCount() - (int) inserted);
    }

    @Nullable
    private static StorageNetworkComponent findStorage(ServerLevel level, BlockPos farm, @Nullable GlobalPos link) {
        for (Direction dir : Direction.values()) {
            StorageNetworkComponent found = storageAt(level, farm.relative(dir), dir.getOpposite(), null);
            if (found != null) return found;
        }
        if (link != null) {
            ServerLevel target = level.getServer().getLevel(link.dimension());
            if (target != null && target.isLoaded(link.pos())) {
                // Wireless: the linked network must have a Wireless Transmitter that reaches the farm, as a Wireless Grid needs.
                var farmAt = new NetworkItemPlayerValidator.PlayerCoordinates(level.dimension(), net.minecraft.world.phys.Vec3.atCenterOf(farm));
                StorageNetworkComponent found = storageAt(target, link.pos(), null, farmAt);
                if (found != null) return found;
                for (Direction dir : Direction.values()) {
                    found = storageAt(target, link.pos(), dir, farmAt);
                    if (found != null) return found;
                }
            }
        }
        return null;
    }

    @Nullable
    private static StorageNetworkComponent storageAt(ServerLevel level, BlockPos pos, @Nullable Direction side, @Nullable NetworkItemPlayerValidator.PlayerCoordinates mustReach) {
        NetworkNodeContainerProvider provider = level.getCapability(RefinedStorageNeoForgeApi.INSTANCE.getNetworkNodeContainerProviderCapability(), pos, side);
        if (provider == null) return null;
        for (InWorldNetworkNodeContainer container : provider.getContainers()) {
            Network network = container.getNode().getNetwork();
            if (network == null) continue;
            if (mustReach != null && !reaches(network, mustReach)) continue;
            StorageNetworkComponent storage = network.getComponent(StorageNetworkComponent.class);
            if (storage != null) return storage;
        }
        return null;
    }

    /** True if a Wireless Transmitter of the network covers the position (range, dimension and being active are its own rules). */
    private static boolean reaches(Network network, NetworkItemPlayerValidator.PlayerCoordinates at) {
        GraphNetworkComponent graph = network.getComponent(GraphNetworkComponent.class);
        return graph != null && graph.getContainers(NetworkItemPlayerValidator.class).stream().anyMatch(v -> v.isValid(at));
    }
}
