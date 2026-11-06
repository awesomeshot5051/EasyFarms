package com.awesomeshot5051.easyfarms.tier;

import com.awesomeshot5051.easyfarms.EasyFarms;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Optional;

/**
 * Lets a Liquid Farm's tank be emptied into a container: an empty item in hand turns into a filled one and takes some of the
 * tank. Loaded from data/&lt;ns&gt;/easy_farms/fluid_container/*.json, so other mods and datapacks can add their own.
 * <p>
 * It also makes a fluid that is not a registered game fluid usable ("virtual" fluids such as honey and milk): the tank, the
 * screen and the block can show it, using {@code texture} and {@code tint}. A virtual fluid cannot go through pipes.
 *
 * @param fluid   the fluid id (e.g. easy_farms:honey)
 * @param empty   the item the player holds (e.g. minecraft:glass_bottle)
 * @param filled  the item the player gets (e.g. minecraft:honey_bottle)
 * @param amount  millibuckets one container takes from the tank
 * @param texture optional block texture for a virtual fluid (e.g. easy_farms:block/honey_still)
 * @param tint    RGB the texture is multiplied with
 */
public record FluidContainer(Identifier fluid, Identifier empty, Identifier filled, int amount,
                             Optional<Identifier> texture, int tint) {
    public static final ResourceKey<Registry<FluidContainer>> REGISTRY_KEY =
            ResourceKey.createRegistryKey(Identifier.fromNamespaceAndPath(EasyFarms.MODID, "fluid_container"));

    public static final Codec<FluidContainer> CODEC = RecordCodecBuilder.create(i -> i.group(
            Identifier.CODEC.fieldOf("fluid").forGetter(FluidContainer::fluid),
            Identifier.CODEC.fieldOf("empty").forGetter(FluidContainer::empty),
            Identifier.CODEC.fieldOf("filled").forGetter(FluidContainer::filled),
            Codec.intRange(1, 100000).optionalFieldOf("amount", 1000).forGetter(FluidContainer::amount),
            Identifier.CODEC.optionalFieldOf("texture").forGetter(FluidContainer::texture),
            Codec.INT.optionalFieldOf("tint", 0xFFFFFF).forGetter(FluidContainer::tint)
    ).apply(i, FluidContainer::new));

    /** The container that fills {@code held} with this fluid, or null. */
    public static FluidContainer find(RegistryAccess access, ItemStack held, Identifier fluid) {
        var registry = access.lookup(REGISTRY_KEY);
        if (registry.isEmpty() || held.isEmpty()) return null;
        Identifier heldId = BuiltInRegistries.ITEM.getKey(held.getItem());
        for (FluidContainer c : registry.get()) if (c.fluid.equals(fluid) && c.empty.equals(heldId)) return c;
        return null;
    }

    /** Any container entry for this fluid (it supplies the texture and tint of a virtual fluid), or null. */
    public static FluidContainer of(RegistryAccess access, Identifier fluid) {
        var registry = access.lookup(REGISTRY_KEY);
        if (registry.isEmpty()) return null;
        FluidContainer fallback = null;
        for (FluidContainer c : registry.get()) {
            if (!c.fluid.equals(fluid)) continue;
            if (c.texture.isPresent()) return c;
            if (fallback == null) fallback = c;
        }
        return fallback;
    }

    /** True if the fluid is not a registered game fluid but some container entry defines it. */
    public static boolean isVirtual(RegistryAccess access, Identifier fluid) {
        return !BuiltInRegistries.FLUID.containsKey(fluid) && of(access, fluid) != null;
    }

    public ItemStack filledStack() {
        Item item = BuiltInRegistries.ITEM.getValue(filled);
        return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
    }
}
