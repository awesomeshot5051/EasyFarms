package com.awesomeshot5051.easyfarms.platform;

import com.mojang.serialization.Codec;
import net.minecraft.core.Registry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

import java.nio.file.Path;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Everything the shared code needs from the mod loader. One implementation per loader, found through
 * {@link java.util.ServiceLoader} (see {@link Services}).
 */
public interface IPlatform {
    /** Creates menus from the data the server sends when it opens them. */
    @FunctionalInterface
    interface MenuFactory<M extends AbstractContainerMenu, D> {
        M create(int containerId, Inventory inventory, D data);
    }

    /** Builds the menu on the server. */
    @FunctionalInterface
    interface ServerMenuFactory {
        AbstractContainerMenu create(int containerId, Inventory inventory, Player player);
    }

    String loaderName();

    /** True if an energy API (NeoForge FE, Fabric Team Reborn Energy) is available, so the Powered card can work. */
    boolean energyAvailable();

    boolean isModLoaded(String modId);

    /** Offers a stack to the item container at {@code pos}, reached from {@code side}. Returns what did not fit (the whole stack if there is no container). */
    net.minecraft.world.item.ItemStack insertItem(net.minecraft.server.level.ServerLevel level, net.minecraft.core.BlockPos pos,
                                                  net.minecraft.core.Direction side, net.minecraft.world.item.ItemStack stack);

    /** Offers a fluid to the tank at {@code pos}, reached from {@code side}. Returns the millibuckets it took. */
    int insertFluid(net.minecraft.server.level.ServerLevel level, net.minecraft.core.BlockPos pos, net.minecraft.core.Direction side,
                    ResourceLocation fluid, int millibuckets);

    /** Directory that holds config files, e.g. {@code config/}. */
    Path configDir();

    /**
     * Registers an object. Fabric registers it immediately; NeoForge when its registry event fires. Either way
     * the returned supplier is valid once registration is done.
     */
    <T> Supplier<T> register(ResourceKey<? extends Registry<? super T>> registry, String name, Supplier<? extends T> factory);

    /** A block entity type for the given blocks (the vanilla builder is not accessible to common code). */
    <T extends net.minecraft.world.level.block.entity.BlockEntity> net.minecraft.world.level.block.entity.BlockEntityType<T> createBlockEntityType(
            java.util.function.BiFunction<net.minecraft.core.BlockPos, net.minecraft.world.level.block.state.BlockState, T> factory,
            net.minecraft.world.level.block.Block... blocks);

    /** Creates an easy_farms registry that is synced to clients. Call before registering entries into it. */
    <T> Registry<T> createSyncedRegistry(ResourceKey<Registry<T>> key);

    /** A datapack registry that is also sent to clients. */
    <T> void registerSyncedDatapackRegistry(ResourceKey<Registry<T>> key, Codec<T> codec);

    /** A menu type whose screen is opened with extra data (see {@link #openMenu}). */
    <M extends AbstractContainerMenu, D> MenuType<M> createMenuType(MenuFactory<M, D> factory, StreamCodec<RegistryFriendlyByteBuf, D> codec);

    /** Opens a menu for the player, sending {@code data} to the client. */
    <D> void openMenu(ServerPlayer player, Component title, D data, StreamCodec<RegistryFriendlyByteBuf, D> codec, ServerMenuFactory factory);

    /** The secret recipes this player has discovered (saved, kept on death, synced to the player's own client). */
    Set<ResourceLocation> discoveredSecrets(Player player);

    void setDiscoveredSecrets(Player player, Set<ResourceLocation> secrets);
}
