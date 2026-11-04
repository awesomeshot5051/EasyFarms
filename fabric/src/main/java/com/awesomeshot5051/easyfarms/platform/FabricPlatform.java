package com.awesomeshot5051.easyfarms.platform;

import com.awesomeshot5051.easyfarms.EasyFarms;
import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.registry.DynamicRegistries;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.fabricmc.fabric.api.event.registry.RegistryAttribute;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
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
    private static final AttachmentType<Set<Identifier>> DISCOVERED_SECRETS = AttachmentRegistry.<Set<Identifier>>builder()
            .initializer(() -> Set.of())
            .persistent(Identifier.CODEC.listOf().xmap(l -> (Set<Identifier>) new HashSet<>(l), s -> List.copyOf(s)))
            .syncWith(ByteBufCodecs.<io.netty.buffer.ByteBuf, Identifier, Set<Identifier>>collection(HashSet::new, Identifier.STREAM_CODEC),
                    AttachmentSyncPredicate.targetOnly())
            .copyOnDeath()
            .buildAndRegister(EasyFarms.id("discovered_secrets"));

    @Override
    public String loaderName() {
        return "Fabric";
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
        Registry<?> registry = (Registry<?>) BuiltInRegistries.REGISTRY.getValue(key.identifier());
        if (registry == null) throw new IllegalStateException("Unknown registry " + key.identifier());
        return registry;
    }

    @Override
    public <B extends net.minecraft.world.level.block.Block> Supplier<B> registerBlock(String name, java.util.function.Function<net.minecraft.world.level.block.state.BlockBehaviour.Properties, B> factory) {
        Identifier id = EasyFarms.id(name);
        var props = net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().setId(ResourceKey.create(net.minecraft.core.registries.Registries.BLOCK, id));
        B block = Registry.register(BuiltInRegistries.BLOCK, id, factory.apply(props));
        return () -> block;
    }

    @Override
    public <I extends net.minecraft.world.item.Item> Supplier<I> registerItem(String name, java.util.function.Function<net.minecraft.world.item.Item.Properties, I> factory) {
        Identifier id = EasyFarms.id(name);
        var props = new net.minecraft.world.item.Item.Properties().setId(ResourceKey.create(net.minecraft.core.registries.Registries.ITEM, id));
        I item = Registry.register(BuiltInRegistries.ITEM, id, factory.apply(props));
        return () -> item;
    }

    @Override
    public <T extends net.minecraft.world.level.block.entity.BlockEntity> net.minecraft.world.level.block.entity.BlockEntityType<T> createBlockEntityType(
            java.util.function.BiFunction<net.minecraft.core.BlockPos, net.minecraft.world.level.block.state.BlockState, T> factory,
            Set<net.minecraft.world.level.block.Block> blocks) {
        return FabricBlockEntityTypeBuilder.<T>create(factory::apply, blocks.toArray(new net.minecraft.world.level.block.Block[0])).build();
    }

    @Override
    public Supplier<net.minecraft.world.item.CreativeModeTab> registerCreativeTab(String name, Component title, Supplier<net.minecraft.world.item.ItemStack> icon,
                                                                                 java.util.function.BiConsumer<net.minecraft.core.HolderLookup.Provider, java.util.function.Consumer<net.minecraft.world.item.ItemStack>> contents) {
        return register(net.minecraft.core.registries.Registries.CREATIVE_MODE_TAB, name, () -> net.minecraft.world.item.CreativeModeTab.builder(net.minecraft.world.item.CreativeModeTab.Row.TOP, 0)
                .title(title)
                .icon(icon)
                .displayItems((params, out) -> contents.accept(params.holders(), out::accept))
                .build());
    }

    @Override
    public <T> Registry<T> createSyncedRegistry(ResourceKey<Registry<T>> key) {
        return FabricRegistryBuilder.create(key).attribute(RegistryAttribute.SYNCED).buildAndRegister();
    }

    @Override
    public <T> void registerSyncedDatapackRegistry(ResourceKey<Registry<T>> key, Codec<T> codec) {
        DynamicRegistries.registerSynced(key, codec);
    }

    @Override
    public <M extends AbstractContainerMenu, D> MenuType<M> createMenuType(MenuFactory<M, D> factory, StreamCodec<RegistryFriendlyByteBuf, D> codec) {
        return new ExtendedMenuType<>(factory::create, codec);
    }

    @Override
    public <D> void openMenu(ServerPlayer player, Component title, D data, StreamCodec<RegistryFriendlyByteBuf, D> codec, ServerMenuFactory factory) {
        player.openMenu(new ExtendedMenuProvider<D>() {
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
    public Set<Identifier> discoveredSecrets(Player player) {
        return player.getAttachedOrElse(DISCOVERED_SECRETS, Set.of());
    }

    @Override
    public void setDiscoveredSecrets(Player player, Set<Identifier> secrets) {
        player.setAttached(DISCOVERED_SECRETS, secrets);
    }
}
