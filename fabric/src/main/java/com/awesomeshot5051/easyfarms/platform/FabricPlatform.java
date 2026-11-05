package com.awesomeshot5051.easyfarms.platform;

import com.awesomeshot5051.easyfarms.EasyFarms;
import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.registry.DynamicRegistries;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.fabricmc.fabric.api.event.registry.RegistryAttribute;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/** Fabric implementation of {@link IPlatform}: registers eagerly. */
public class FabricPlatform implements IPlatform {
    @SuppressWarnings("UnstableApiUsage")
    private static final AttachmentType<Set<ResourceLocation>> DISCOVERED_SECRETS = AttachmentRegistry.<Set<ResourceLocation>>builder()
            .initializer(() -> Set.of())
            .persistent(ResourceLocation.CODEC.listOf().xmap(l -> (Set<ResourceLocation>) new HashSet<>(l), s -> List.copyOf(s)))
            .syncWith(ByteBufCodecs.<io.netty.buffer.ByteBuf, ResourceLocation, Set<ResourceLocation>>collection(HashSet::new, ResourceLocation.STREAM_CODEC),
                    AttachmentSyncPredicate.targetOnly())
            .copyOnDeath()
            .buildAndRegister(EasyFarms.id("discovered_secrets"));

    @Override
    public String loaderName() {
        return "Fabric";
    }

    @Override
    public boolean energyAvailable() {
        return FabricLoader.getInstance().isModLoaded("team_reborn_energy");
    }

    @Override
    public boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    @Override
    public Path configDir() {
        return FabricLoader.getInstance().getConfigDir();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Supplier<T> register(ResourceKey<? extends Registry<? super T>> registry, String name, Supplier<? extends T> factory) {
        Registry<Object> target = (Registry<Object>) findRegistry(registry);
        T value = Registry.register(target, EasyFarms.id(name), factory.get());
        return () -> value;
    }

    @SuppressWarnings("unchecked")
    private static Registry<?> findRegistry(ResourceKey<? extends Registry<?>> key) {
        Registry<?> registry = (Registry<?>) BuiltInRegistries.REGISTRY.get(key.location());
        if (registry == null) throw new IllegalStateException("Unknown registry " + key.location());
        return registry;
    }

    @Override
    public <T extends net.minecraft.world.level.block.entity.BlockEntity> net.minecraft.world.level.block.entity.BlockEntityType<T> createBlockEntityType(
            java.util.function.BiFunction<net.minecraft.core.BlockPos, net.minecraft.world.level.block.state.BlockState, T> factory,
            net.minecraft.world.level.block.Block... blocks) {
        return net.minecraft.world.level.block.entity.BlockEntityType.Builder.of(factory::apply, blocks).build(null);
    }

    @Override
    public <T> Registry<T> createSyncedRegistry(ResourceKey<Registry<T>> key) {
        return FabricRegistryBuilder.createSimple(key).attribute(RegistryAttribute.SYNCED).buildAndRegister();
    }

    @Override
    public <T> void registerSyncedDatapackRegistry(ResourceKey<Registry<T>> key, Codec<T> codec) {
        DynamicRegistries.registerSynced(key, codec);
    }

    @Override
    public <M extends AbstractContainerMenu, D> MenuType<M> createMenuType(MenuFactory<M, D> factory, StreamCodec<RegistryFriendlyByteBuf, D> codec) {
        return new ExtendedScreenHandlerType<>(factory::create, codec);
    }

    @Override
    public <D> void openMenu(ServerPlayer player, Component title, D data, StreamCodec<RegistryFriendlyByteBuf, D> codec, ServerMenuFactory factory) {
        player.openMenu(new ExtendedScreenHandlerFactory<D>() {
            @Override
            public D getScreenOpeningData(ServerPlayer p) {
                return data;
            }

            @Override
            public Component getDisplayName() {
                return title;
            }

            @Override
            public AbstractContainerMenu createMenu(int id, Inventory inventory, Player p) {
                return factory.create(id, inventory, p);
            }
        });
    }

    @Override
    public Set<ResourceLocation> discoveredSecrets(Player player) {
        return player.getAttachedOrElse(DISCOVERED_SECRETS, Set.of());
    }

    @Override
    public void setDiscoveredSecrets(Player player, Set<ResourceLocation> secrets) {
        player.setAttached(DISCOVERED_SECRETS, secrets);
    }
}
