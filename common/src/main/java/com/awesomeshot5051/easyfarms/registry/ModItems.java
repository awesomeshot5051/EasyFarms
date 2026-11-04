package com.awesomeshot5051.easyfarms.registry;

import com.awesomeshot5051.easyfarms.EasyFarms;
import com.awesomeshot5051.easyfarms.platform.Services;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;

import java.util.List;
import java.util.function.Supplier;

/** Upgrade cards. Which cards fit which farm, and what they need from the recipe, is explained in the Upgrades screen. */
public class ModItems {
    public static final Supplier<Item> REDSTONE_UPGRADE = card("redstone_upgrade");
    public static final Supplier<Item> SMELTER_UPGRADE = card("smelter_upgrade");
    public static final Supplier<Item> SPEED_UPGRADE = card("speed_upgrade");
    public static final Supplier<Item> NETHER_UPGRADE = card("nether_upgrade");
    public static final Supplier<Item> INSCRIBER_UPGRADE = card("inscriber_upgrade");
    /** New in EasyFarms: with this installed, the recipe's input items are never used up. */
    public static final Supplier<Item> INFINITY_UPGRADE = card("infinity_upgrade");

    /**
     * A blank card for datapacks: give it the {@code easy_farms:card} component and a farm_upgrade with the same
     * {@code card} id, and it is a new upgrade, with no Java and no item registration.
     */
    public static final Supplier<Item> UPGRADE_CARD = Services.PLATFORM.registerItem("upgrade_card", p -> new UpgradeCardItem(p.stacksTo(1)));

    public static final List<Supplier<Item>> UPGRADES = List.of(
            REDSTONE_UPGRADE, SMELTER_UPGRADE, SPEED_UPGRADE, NETHER_UPGRADE, INSCRIBER_UPGRADE, INFINITY_UPGRADE);

    private static Supplier<Item> card(String name) {
        return Services.PLATFORM.registerItem(name, p -> new Item(p.stacksTo(1)));
    }

    public static void init() {
        // class load registers the items
    }
}
