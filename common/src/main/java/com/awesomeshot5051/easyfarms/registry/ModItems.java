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
    /** Runs the farm on energy (ME / FE) at diamond speed. */
    public static final Supplier<Item> POWERED_UPGRADE = card("powered_upgrade");
    /** Doubles item stacks, the energy buffer and the liquid tank per card; stacks (see its farm_upgrade). */
    public static final Supplier<Item> CAPACITY_UPGRADE = Services.PLATFORM.register(Registries.ITEM, "capacity_upgrade", () -> new Item(new Item.Properties().stacksTo(64)));
    /** Cuts the energy a Powered farm uses per tick; needs the Powered card (see its farm_upgrade). */
    public static final Supplier<Item> EFFICIENCY_UPGRADE = Services.PLATFORM.register(Registries.ITEM, "efficiency_upgrade", () -> new Item(new Item.Properties().stacksTo(64)));
    /** Keeps the farm's chunk loaded so it works while nobody is near. */
    public static final Supplier<Item> CHUNK_LOADER_UPGRADE = card("chunk_loader_upgrade");
    public static final Supplier<Item> VOID_UPGRADE = card("void_upgrade");
    public static final Supplier<Item> FILTER_UPGRADE = card("filter_upgrade");
    public static final Supplier<Item> AUTO_EXPORT_UPGRADE = card("auto_export_upgrade");
    public static final Supplier<Item> EXPERIENCE_UPGRADE = card("experience_upgrade");
    /** Copies a farm's side and filter settings (sneak-click) and pastes them onto another farm (click). */
    public static final Supplier<Item> MEMORY_CARD = Services.PLATFORM.register(Registries.ITEM, "memory_card", () -> new Item(new Item.Properties().stacksTo(1)));
    /** New in EasyFarms: with this installed, the recipe's input items are never used up. */
    public static final Supplier<Item> INFINITY_UPGRADE = card("infinity_upgrade");

    /**
     * A blank card for datapacks: give it the {@code easy_farms:card} component and a farm_upgrade with the same
     * {@code card} id, and it is a new upgrade, with no Java and no item registration.
     */
    public static final Supplier<Item> UPGRADE_CARD = Services.PLATFORM.register(Registries.ITEM, "upgrade_card", () -> new UpgradeCardItem(new Item.Properties().stacksTo(64)));

    public static final List<Supplier<Item>> UPGRADES = List.of(
            REDSTONE_UPGRADE, SMELTER_UPGRADE, SPEED_UPGRADE, NETHER_UPGRADE, INSCRIBER_UPGRADE, INFINITY_UPGRADE, POWERED_UPGRADE, CAPACITY_UPGRADE, EFFICIENCY_UPGRADE, CHUNK_LOADER_UPGRADE, VOID_UPGRADE, FILTER_UPGRADE, AUTO_EXPORT_UPGRADE, EXPERIENCE_UPGRADE);

    private static Supplier<Item> card(String name) {
        return Services.PLATFORM.register(Registries.ITEM, name, () -> new Item(new Item.Properties().stacksTo(64)));
    }

    public static void init() {
        // class load registers the items
    }
}
