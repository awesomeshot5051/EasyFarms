package com.awesomeshot5051.easyfarms.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;

import java.util.Optional;
import java.util.stream.Stream;

/**
 * An ingredient plus how many of it a slot must hold. Vanilla has no such type and loader versions differ, so the
 * mod carries its own; the JSON is the same as NeoForge's nested form:
 * <pre>{"ingredient": "minecraft:cobblestone", "count": 64}   {"ingredient": ["minecraft:chicken", "minecraft:cooked_chicken"]}</pre>
 * {@code count} defaults to 1.
 */
public record SizedIngredient(Ingredient ingredient, int count) {
    public static final Codec<SizedIngredient> NESTED_CODEC = RecordCodecBuilder.create(i -> i.group(
            Ingredient.CODEC.fieldOf("ingredient").forGetter(SizedIngredient::ingredient),
            // Always written (like NeoForge's own), but optional to read.
            ExtraCodecs.POSITIVE_INT.optionalFieldOf("count").forGetter(s -> Optional.of(s.count()))
    ).apply(i, (ingredient, count) -> new SizedIngredient(ingredient, count.orElse(1))));

    public static SizedIngredient of(ItemLike item, int count) {
        return new SizedIngredient(Ingredient.of(item), count);
    }

    public boolean test(ItemStack stack) {
        return ingredient.test(stack) && stack.getCount() >= count;
    }

    /** Every matching item as a stack of {@code count}. */
    public Stream<ItemStack> stacks() {
        return ingredient.items().map(Holder::value).map(Item::getDefaultInstance).map(s -> s.copyWithCount(count));
    }
}
