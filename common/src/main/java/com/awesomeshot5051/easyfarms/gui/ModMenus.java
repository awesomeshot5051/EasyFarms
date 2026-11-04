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

    public static final Supplier<MenuType<FarmMenu>> FARM = Services.PLATFORM.register(Registries.MENU, "farm",
            () -> Services.PLATFORM.createMenuType((id, inv, pos) -> new FarmMenu(id, inv, pos, null), FARM_CODEC));
    public static final Supplier<MenuType<FarmUpgradeMenu>> FARM_UPGRADES = Services.PLATFORM.register(Registries.MENU, "farm_upgrades",
            () -> Services.PLATFORM.createMenuType((id, inv, data) -> new FarmUpgradeMenu(id, inv, data.pos(), null, data.kind()), UpgradeOpening.CODEC));

    public static void init() {
        // class load registers the menus
    }
}
