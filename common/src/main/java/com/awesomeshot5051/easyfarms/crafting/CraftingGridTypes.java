package com.awesomeshot5051.easyfarms.crafting;

import com.awesomeshot5051.easyfarms.EasyFarms;
import com.awesomeshot5051.easyfarms.platform.Services;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

import java.util.function.Supplier;

/**
 * The crafting grid type registry ({@code easy_farms:crafting_grid_type}) and the type that ships with the mod. Another mod
 * registers its own {@link CraftingGridType} under key {@link #KEY}; grids that use it are then added with datapack JSON.
 */
public final class CraftingGridTypes {
    private CraftingGridTypes() {
    }

    public static final ResourceKey<Registry<CraftingGridType>> KEY =
            ResourceKey.createRegistryKey(Identifier.fromNamespaceAndPath(EasyFarms.MODID, "crafting_grid_type"));
    public static final Registry<CraftingGridType> REGISTRY = Services.PLATFORM.createSyncedRegistry(KEY);

    /** Crafting-table style recipes: shaped and shapeless, from any mod that adds them as crafting recipes. */
    public static final Supplier<CraftingGridType> VANILLA = Services.PLATFORM.register(KEY, "vanilla", VanillaCraftingGrid::new);

    public static void init() {
        // class load registers the grid types
    }
}
