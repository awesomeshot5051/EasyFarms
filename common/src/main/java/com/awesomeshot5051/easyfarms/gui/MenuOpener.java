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

    /** The Catalysts screen: one slot per installed card that needs a catalyst. */
    public static void openCatalysts(ServerPlayer player, BlockPos pos, FarmBlockEntity farm, Component title) {
        int[] owners = farm.catalystOwners();
        Services.PLATFORM.openMenu(player, title, new ModMenus.CatalystOpening(pos, owners), ModMenus.CatalystOpening.CODEC,
                (id, inv, p) -> new FarmCatalystMenu(id, inv, pos, farm, owners));
    }

    /** The Filter screen: the items the farm's current recipe can output, each allowed or blocked. */
    public static void openFilter(ServerPlayer player, BlockPos pos, FarmBlockEntity farm, Component title) {
        java.util.List<net.minecraft.resources.ResourceLocation> items = farm.possibleOutputs();
        if (items.size() > FarmFilterMenu.MAX_ENTRIES) items = items.subList(0, FarmFilterMenu.MAX_ENTRIES);
        final java.util.List<net.minecraft.resources.ResourceLocation> shown = items;
        Services.PLATFORM.openMenu(player, title, new ModMenus.FilterOpening(pos, shown), ModMenus.FilterOpening.CODEC,
                (id, inv, p) -> new FarmFilterMenu(id, inv, pos, farm, shown));
    }

    /** The Crafting card's targets screen. */
    public static void openCrafting(ServerPlayer player, BlockPos pos, FarmBlockEntity farm, Component title) {
        Services.PLATFORM.openMenu(player, title, pos, ModMenus.FARM_CODEC, (id, inv, p) -> new FarmCraftingMenu(id, inv, pos, farm));
    }

    public static void openUpgrades(ServerPlayer player, BlockPos pos, FarmBlockEntity farm, Component title) {
        Services.PLATFORM.openMenu(player, title, new ModMenus.UpgradeOpening(pos, farm.kind()), ModMenus.UpgradeOpening.CODEC,
                (id, inv, p) -> new FarmUpgradeMenu(id, inv, pos, farm));
    }
}
