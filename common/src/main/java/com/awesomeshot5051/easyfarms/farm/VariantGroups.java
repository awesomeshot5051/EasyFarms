package com.awesomeshot5051.easyfarms.farm;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Groups of interchangeable items (the variants a Chisel-style mod adds for one block). A group is an item tag whose path starts
 * with {@code variants/}, in any namespace, for example {@code easy_farms:variants/cobblestone}. An item belongs to every such tag
 * that lists it, so a datapack makes a group with one JSON file and needs no code. The same lists are built on the client (for the
 * picker) and on the server (to apply the choice), so an index into a list means the same item on both sides.
 */
public final class VariantGroups {
    public static final String PREFIX = "variants/";

    private VariantGroups() {
    }

    /**
     * Every item that can stand in for {@code item}, with {@code item} itself first and the rest sorted by id. Empty when the item is in
     * no variant group (or the group has nothing but the item).
     */
    public static List<Item> variants(Item item) {
        if (item == Items.AIR) return List.of();
        Set<Item> found = new LinkedHashSet<>();
        BuiltInRegistries.ITEM.getTags().forEach(named -> {
            if (!named.key().location().getPath().startsWith(PREFIX)) return;
            boolean member = false;
            for (Holder<Item> h : named) {
                if (h.value() == item) {
                    member = true;
                    break;
                }
            }
            if (!member) return;
            for (Holder<Item> h : named) found.add(h.value());
        });
        found.remove(item);
        if (found.isEmpty()) return List.of();
        List<Item> sorted = new ArrayList<>(found);
        sorted.sort(Comparator.comparing(i -> BuiltInRegistries.ITEM.getKey(i).toString()));
        sorted.add(0, item);
        return sorted;
    }

    /** True if the item is in a variant group with at least one other item. */
    public static boolean hasVariants(Item item) {
        return !variants(item).isEmpty();
    }
}
