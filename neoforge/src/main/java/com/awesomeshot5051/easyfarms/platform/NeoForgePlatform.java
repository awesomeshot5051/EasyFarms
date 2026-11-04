package com.awesomeshot5051.easyfarms.platform;

import com.awesomeshot5051.easyfarms.EasyFarms;
import com.mojang.serialization.Codec;
import net.minecraft.core.Registry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/** NeoForge implementation of {@link IPlatform}. Registrations are collected, then attached to the mod bus by {@link #bind}. */
public class NeoForgePlatform implements IPlatform {
    private static final Map<Identifier, DeferredRegister<?>> REGISTERS = new LinkedHashMap<>();
    private static final List<Runnable> DATAPACK_REGISTRIES = new ArrayList<>();
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, EasyFarms.MODID);

    private static final Supplier<AttachmentType<Set<Identifier>>> DISCOVERED_SECRETS = ATTACHMENTS.register("discovered_secrets",
            () -> AttachmentType.<Set<Identifier>>builder(() -> Set.of())
                    .serialize(Identifier.CODEC.listOf().xmap(l -> (Set<Identifier>) new HashSet<>(l), s -> List.copyOf(s)).fieldOf("ids"))
                    .sync(ByteBufCodecs.<io.netty.buffer.ByteBuf, Identifier, Set<Identifier>>collection(HashSet::new, Identifier.STREAM_CODEC))
                    .copyOnDeath()
                    .build());

    /** Attaches every collected registration to the mod event bus. Call once, after {@code EasyFarms.init()}. */
    public static void bind(IEventBus modBus) {
        REGISTERS.values().forEach(r -> r.register(modBus));
        ATTACHMENTS.register(modBus);
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        modBus.addListener(NeoForgePlatform::onNewDatapackRegistry);
    }

    private static void onNewDatapackRegistry(DataPackRegistryEvent.NewRegistry event) {
        DatapackRegistrar.event = event;
        DATAPACK_REGISTRIES.forEach(Runnable::run);
        DatapackRegistrar.event = null;
    }

    private static final class DatapackRegistrar {
        static DataPackRegistryEvent.NewRegistry event;
    }

    @SuppressWarnings("unchecked")
    private static <T> DeferredRegister<T> registerFor(ResourceKey<? extends Registry<? super T>> key) {
        return (DeferredRegister<T>) REGISTERS.computeIfAbsent(key.identifier(),
                id -> DeferredRegister.create((ResourceKey<? extends Registry<Object>>) key, EasyFarms.MODID));
    }

    @Override
    public String loaderName() {
        return "NeoForge";
    }

    @Override
    public boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    @Override
    public Path configDir() {
        return FMLPaths.CONFIGDIR.get();
    }

    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(EasyFarms.MODID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(EasyFarms.MODID);

    @Override
    public <B extends net.minecraft.world.level.block.Block> Supplier<B> registerBlock(String name, java.util.function.Function<net.minecraft.world.level.block.state.BlockBehaviour.Properties, B> factory) {
        return BLOCKS.registerBlock(name, factory);
    }

    @Override
    public <I extends net.minecraft.world.item.Item> Supplier<I> registerItem(String name, java.util.function.Function<net.minecraft.world.item.Item.Properties, I> factory) {
        return ITEMS.registerItem(name, factory);
    }

    @Override
    public <T> Supplier<T> register(ResourceKey<? extends Registry<? super T>> registry, String name, Supplier<? extends T> factory) {
        DeferredRegister<T> register = registerFor(registry);
        return register.register(name, factory);
    }

    @Override
    public <T extends net.minecraft.world.level.block.entity.BlockEntity> net.minecraft.world.level.block.entity.BlockEntityType<T> createBlockEntityType(
            java.util.function.BiFunction<net.minecraft.core.BlockPos, net.minecraft.world.level.block.state.BlockState, T> factory,
            Set<net.minecraft.world.level.block.Block> blocks) {
        return new net.minecraft.world.level.block.entity.BlockEntityType<>(factory::apply, blocks);
    }

    @Override
    public Supplier<net.minecraft.world.item.CreativeModeTab> registerCreativeTab(String name, Component title, Supplier<net.minecraft.world.item.ItemStack> icon,
                                                                                 java.util.function.BiConsumer<net.minecraft.core.HolderLookup.Provider, java.util.function.Consumer<net.minecraft.world.item.ItemStack>> contents) {
        return register(net.minecraft.core.registries.Registries.CREATIVE_MODE_TAB, name, () -> net.minecraft.world.item.CreativeModeTab.builder()
                .title(title)
                .icon(icon)
                .displayItems((params, out) -> contents.accept(params.holders(), out::accept))
                .build());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Registry<T> createSyncedRegistry(ResourceKey<Registry<T>> key) {
        DeferredRegister<T> register = registerFor((ResourceKey<? extends Registry<? super T>>) key);
        return register.makeRegistry(b -> b.sync(true));
    }

    @Override
    public <T> void registerSyncedDatapackRegistry(ResourceKey<Registry<T>> key, Codec<T> codec) {
        DATAPACK_REGISTRIES.add(() -> DatapackRegistrar.event.dataPackRegistry(key, codec, codec));
    }

    @Override
    public <M extends AbstractContainerMenu, D> MenuType<M> createMenuType(MenuFactory<M, D> factory, StreamCodec<RegistryFriendlyByteBuf, D> codec) {
        return IMenuTypeExtension.create((id, inv, buf) -> factory.create(id, inv, codec.decode(buf)));
    }

    @Override
    public <D> void openMenu(ServerPlayer player, Component title, D data, StreamCodec<RegistryFriendlyByteBuf, D> codec, ServerMenuFactory factory) {
        player.openMenu(new SimpleMenuProvider(factory::create, title), buf -> codec.encode(buf, data));
    }

    @Override
    public Set<Identifier> discoveredSecrets(Player player) {
        return player.getData(DISCOVERED_SECRETS.get());
    }

    @Override
    public void setDiscoveredSecrets(Player player, Set<Identifier> secrets) {
        player.setData(DISCOVERED_SECRETS.get(), secrets);
    }
}
