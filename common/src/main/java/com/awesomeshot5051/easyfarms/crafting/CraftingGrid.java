package com.awesomeshot5051.easyfarms.crafting;

import com.awesomeshot5051.easyfarms.EasyFarms;
import com.awesomeshot5051.easyfarms.platform.Services;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * One crafting grid the Crafting card can use. Data: {@code data/<ns>/easy_farms/crafting_grid/*.json}, so a pack or another
 * mod adds a grid with a file and the card picks it up (unless the card's effect lists the grids it wants).
 *
 * @param type        a {@link CraftingGridType} id: how this grid crafts
 * @param width       grid width (slots)
 * @param height      grid height (slots)
 * @param recipeType  optional: the recipe type the vanilla grid type reads (default {@code minecraft:crafting})
 * @param requiresMod optional: the grid is ignored unless this mod is loaded
 * @param priority    higher is tried first; ties go to the smaller grid
 * @param name        optional display name
 */
public record CraftingGrid(Identifier type, int width, int height, Optional<Identifier> recipeType, Optional<String> requiresMod,
                           int priority, Optional<Component> name) {
    public static final ResourceKey<Registry<CraftingGrid>> REGISTRY_KEY =
            ResourceKey.createRegistryKey(Identifier.fromNamespaceAndPath(EasyFarms.MODID, "crafting_grid"));

    public static final Codec<CraftingGrid> CODEC = RecordCodecBuilder.create(i -> i.group(
            Identifier.CODEC.optionalFieldOf("type", EasyFarms.id("vanilla")).forGetter(CraftingGrid::type),
            Codec.intRange(1, 16).fieldOf("width").forGetter(CraftingGrid::width),
            Codec.intRange(1, 16).fieldOf("height").forGetter(CraftingGrid::height),
            Identifier.CODEC.optionalFieldOf("recipe_type").forGetter(CraftingGrid::recipeType),
            Codec.STRING.optionalFieldOf("requires_mod").forGetter(CraftingGrid::requiresMod),
            Codec.INT.optionalFieldOf("priority", 0).forGetter(CraftingGrid::priority),
            ComponentSerialization.CODEC.optionalFieldOf("name").forGetter(CraftingGrid::name)
    ).apply(i, CraftingGrid::new));

    /** The registered type that does the crafting, or empty if no mod registered it. */
    public Optional<CraftingGridType> typeInstance() {
        return CraftingGridTypes.REGISTRY.getOptional(type);
    }

    /** True if the type exists and works here and the required mod (if any) is loaded. */
    public boolean usable() {
        if (requiresMod.isPresent() && !Services.PLATFORM.isModLoaded(requiresMod.get())) return false;
        return typeInstance().map(CraftingGridType::available).orElse(false);
    }

    /**
     * The usable grids, in the order they are tried (priority, then smaller first, then id).
     *
     * @param only grid ids to restrict to; empty = every grid
     */
    public static List<CraftingGrid> usable(RegistryAccess access, List<Identifier> only) {
        var lookup = access.lookup(REGISTRY_KEY);
        if (lookup.isEmpty()) return List.of();
        List<Holder.Reference<CraftingGrid>> found = new ArrayList<>();
        lookup.get().listElements().forEach(h -> {
            if ((only.isEmpty() || only.contains(h.key().identifier())) && h.value().usable()) found.add(h);
        });
        found.sort(Comparator.<Holder.Reference<CraftingGrid>>comparingInt(h -> -h.value().priority())
                .thenComparingInt(h -> h.value().width() * h.value().height())
                .thenComparing(h -> h.key().identifier().toString()));
        List<CraftingGrid> grids = new ArrayList<>();
        for (var h : found) grids.add(h.value());
        return grids;
    }
}
