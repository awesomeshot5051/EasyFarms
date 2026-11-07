package com.awesomeshot5051.easyfarms.gui;

import com.awesomeshot5051.easyfarms.farm.FarmKind;
import com.awesomeshot5051.easyfarms.platform.Services;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.inventory.MenuType;

import java.util.function.Supplier;

public class ModMenus {
    /** What the server sends along when it opens the Farm screen: where the farm is. */
    public static final StreamCodec<RegistryFriendlyByteBuf, BlockPos> FARM_CODEC = BlockPos.STREAM_CODEC.cast();

    /** What the server sends along when it opens the Upgrades screen: where the farm is and which kind it is. */
    public record UpgradeOpening(BlockPos pos, FarmKind kind) {
        public static final StreamCodec<RegistryFriendlyByteBuf, UpgradeOpening> CODEC = StreamCodec.of(
                (buf, v) -> {
                    buf.writeBlockPos(v.pos());
                    buf.writeEnum(v.kind());
                },
                buf -> new UpgradeOpening(buf.readBlockPos(), buf.readEnum(FarmKind.class)));
    }

    /**
     * What the server sends along when it opens the Catalysts screen: where the farm is, and for each of its catalyst slots
     * the upgrade slot whose card owns it (or -1), so the client builds the same slots.
     */
    public record CatalystOpening(BlockPos pos, int[] owners) {
        public static final StreamCodec<RegistryFriendlyByteBuf, CatalystOpening> CODEC = StreamCodec.of(
                (buf, v) -> {
                    buf.writeBlockPos(v.pos());
                    buf.writeVarInt(v.owners().length);
                    for (int owner : v.owners()) buf.writeVarInt(owner + 1);
                },
                buf -> {
                    BlockPos pos = buf.readBlockPos();
                    int[] owners = new int[buf.readVarInt()];
                    for (int i = 0; i < owners.length; i++) owners[i] = buf.readVarInt() - 1;
                    return new CatalystOpening(pos, owners);
                });
    }

    /** What the server sends along when it opens the Filter screen: where the farm is and the items to show. */
    public record FilterOpening(BlockPos pos, java.util.List<net.minecraft.resources.ResourceLocation> items) {
        public static final StreamCodec<RegistryFriendlyByteBuf, FilterOpening> CODEC = StreamCodec.of(
                (buf, v) -> {
                    buf.writeBlockPos(v.pos());
                    buf.writeVarInt(v.items().size());
                    for (var id : v.items()) buf.writeResourceLocation(id);
                },
                buf -> {
                    BlockPos pos = buf.readBlockPos();
                    int n = buf.readVarInt();
                    java.util.List<net.minecraft.resources.ResourceLocation> items = new java.util.ArrayList<>();
                    for (int i = 0; i < n; i++) items.add(buf.readResourceLocation());
                    return new FilterOpening(pos, items);
                });
    }

    public static final Supplier<MenuType<FarmMenu>> FARM = Services.PLATFORM.register(Registries.MENU, "farm",
            () -> Services.PLATFORM.createMenuType((id, inv, pos) -> new FarmMenu(id, inv, pos, null), FARM_CODEC));
    public static final Supplier<MenuType<FarmUpgradeMenu>> FARM_UPGRADES = Services.PLATFORM.register(Registries.MENU, "farm_upgrades",
            () -> Services.PLATFORM.createMenuType((id, inv, data) -> new FarmUpgradeMenu(id, inv, data.pos(), null, data.kind()), UpgradeOpening.CODEC));

    public static final Supplier<MenuType<FarmCatalystMenu>> FARM_CATALYSTS = Services.PLATFORM.register(Registries.MENU, "farm_catalysts",
            () -> Services.PLATFORM.createMenuType((id, inv, data) -> new FarmCatalystMenu(id, inv, data.pos(), null, data.owners()), CatalystOpening.CODEC));

    public static final Supplier<MenuType<FarmFilterMenu>> FARM_FILTER = Services.PLATFORM.register(Registries.MENU, "farm_filter",
            () -> Services.PLATFORM.createMenuType((id, inv, data) -> new FarmFilterMenu(id, inv, data.pos(), null, data.items()), FilterOpening.CODEC));

    public static final Supplier<MenuType<FarmCraftingMenu>> FARM_CRAFTING = Services.PLATFORM.register(Registries.MENU, "farm_crafting",
            () -> Services.PLATFORM.createMenuType((id, inv, pos) -> new FarmCraftingMenu(id, inv, pos, null), FARM_CODEC));

    /** The DimStorage Auto Importer card's frequency screen: the server sends which hand holds the card. */
    public static final Supplier<MenuType<CardFrequencyMenu>> CARD_FREQUENCY = Services.PLATFORM.register(Registries.MENU, "card_frequency",
            () -> Services.PLATFORM.createMenuType((id, inv, hand) -> new CardFrequencyMenu(id, inv, hand),
                    net.minecraft.network.codec.ByteBufCodecs.VAR_INT.cast()));

    public static void init() {
        // class load registers the menus
    }
}
