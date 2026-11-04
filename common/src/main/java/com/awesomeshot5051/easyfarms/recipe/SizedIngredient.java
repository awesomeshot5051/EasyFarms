package com.awesomeshot5051.easyfarms.recipe;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.Arrays;

/**
 * An ingredient plus how many of it a slot must hold (NeoForge ships this class; Fabric does not, so it lives here).
 * <p>
 * JSON, flat like NeoForge's {@code FLAT_CODEC}: the ingredient's own fields next to {@code count} (default 1):
 * <pre>{"item": "minecraft:cobblestone", "count": 64}   {"tag": "c:concretes", "count": 2}</pre>
 * Any one of several items: put them under {@code ingredients} (this replaces {@code neoforge:compound}):
 * <pre>{"ingredients": [{"item": "minecraft:emerald"}, {"item": "minecraft:enchanted_book"}], "count": 1}</pre>
 */
public record SizedIngredient(Ingredient ingredient, int count) {
    public static final Codec<SizedIngredient> FLAT_CODEC = new Codec<>() {
        @Override
        public <T> DataResult<Pair<SizedIngredient, T>> decode(DynamicOps<T> ops, T input) {
            T source = ops.get(input, "ingredients").result().orElse(input);
            DataResult<Ingredient> ingredient = Ingredient.CODEC_NONEMPTY.parse(ops, source);
            if (ingredient.error().isPresent()) return DataResult.error(() -> ingredient.error().get().message());
            int count = ops.get(input, "count").result().flatMap(t -> ops.getNumberValue(t).result()).map(Number::intValue).orElse(1);
            if (count < 1) return DataResult.error(() -> "Ingredient count must be at least 1");
            return DataResult.success(Pair.of(new SizedIngredient(ingredient.getOrThrow(), count), input));
        }

        @Override
        public <T> DataResult<T> encode(SizedIngredient value, DynamicOps<T> ops, T prefix) {
            DataResult<T> encoded = Ingredient.CODEC_NONEMPTY.encodeStart(ops, value.ingredient());
            return encoded.flatMap(t -> ops.getMap(t).result().isPresent()
                    ? ops.mergeToMap(t, ops.createString("count"), ops.createInt(value.count()))
                    : DataResult.success(ops.mapBuilder().add("ingredients", t).add("count", ops.createInt(value.count())).build(ops.empty()).getOrThrow()));
        }
    };

    public static SizedIngredient of(net.minecraft.world.level.ItemLike item, int count) {
        return new SizedIngredient(Ingredient.of(item), count);
    }

    public boolean test(ItemStack stack) {
        return ingredient.test(stack) && stack.getCount() >= count;
    }

    /** Every matching item, each as a stack of {@code count}. */
    public ItemStack[] getItems() {
        return Arrays.stream(ingredient.getItems()).map(s -> s.copyWithCount(count)).toArray(ItemStack[]::new);
    }
}
