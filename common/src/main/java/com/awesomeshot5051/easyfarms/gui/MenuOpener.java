package com.awesomeshot5051.easyfarms.gui;

import com.awesomeshot5051.easyfarms.farm.FarmBlockEntity;
import com.awesomeshot5051.easyfarms.platform.Services;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** Opens the farm screens, sending the farm's position (and kind, for the Upgrades screen) to the client. */
public final class MenuOpener {
    private MenuOpener() {
    }

    public static void openFarm(ServerPlayer player, BlockPos pos, FarmBlockEntity farm, Component title) {
        Services.PLATFORM.openMenu(player, title, pos, ModMenus.FARM_CODEC, (id, inv, p) -> new FarmMenu(id, inv, pos, farm));
    }

    public static void openUpgrades(ServerPlayer player, BlockPos pos, FarmBlockEntity farm, Component title) {
        Services.PLATFORM.openMenu(player, title, new ModMenus.UpgradeOpening(pos, farm.kind()), ModMenus.UpgradeOpening.CODEC,
                (id, inv, p) -> new FarmUpgradeMenu(id, inv, pos, farm));
    }
}
