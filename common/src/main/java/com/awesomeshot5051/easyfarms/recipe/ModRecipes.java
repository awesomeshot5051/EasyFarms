package com.awesomeshot5051.easyfarms.recipe;

import com.awesomeshot5051.easyfarms.EasyFarms;
import com.awesomeshot5051.easyfarms.farm.FarmKind;
import com.awesomeshot5051.easyfarms.platform.Services;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/** One recipe type + serializer per farm kind: easy_farms:resource_farm, easy_farms:plant_farm, easy_farms:mob_farm. */
public class ModRecipes {
    public static final Map<FarmKind, Supplier<RecipeSerializer<FarmRecipe>>> SERIALIZERS_BY_KIND = new EnumMap<>(FarmKind.class);
    public static final Map<FarmKind, Supplier<RecipeType<FarmRecipe>>> TYPES_BY_KIND = new EnumMap<>(FarmKind.class);

    public static final Supplier<RecipeSerializer<HoeTillRecipe>> HOE_TILL =
            Services.PLATFORM.register(Registries.RECIPE_SERIALIZER, "hoe_till",
                    () -> new RecipeSerializer<>(MapCodec.unit(HoeTillRecipe::new), StreamCodec.of((buf, recipe) -> {
                    }, buf -> new HoeTillRecipe())));

    static {
        for (FarmKind kind : FarmKind.values()) {
            SERIALIZERS_BY_KIND.put(kind, Services.PLATFORM.register(Registries.RECIPE_SERIALIZER, kind.id(), () -> FarmRecipe.serializer(kind)));
            TYPES_BY_KIND.put(kind, Services.PLATFORM.register(Registries.RECIPE_TYPE, kind.id(), () -> simpleType(EasyFarms.id(kind.id()))));
        }
    }

    private static <T extends net.minecraft.world.item.crafting.Recipe<?>> RecipeType<T> simpleType(net.minecraft.resources.Identifier id) {
        return new RecipeType<>() {
            @Override
            public String toString() {
                return id.toString();
            }
        };
    }

    /** Every loaded recipe of this farm kind. */
    public static List<RecipeHolder<FarmRecipe>> of(RecipeManager manager, FarmKind kind) {
        List<RecipeHolder<FarmRecipe>> out = new ArrayList<>();
        for (RecipeHolder<?> holder : manager.getRecipes()) {
            if (holder.value() instanceof FarmRecipe farm && farm.kind() == kind) {
                @SuppressWarnings("unchecked") RecipeHolder<FarmRecipe> typed = (RecipeHolder<FarmRecipe>) holder;
                out.add(typed);
            }
        }
        return out;
    }

    public static void init() {
        // class load registers the recipe types and serializers
    }
}
