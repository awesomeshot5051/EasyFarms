package com.awesomeshot5051.easyfarms.recipe;

import com.awesomeshot5051.easyfarms.EasyFarms;
import com.awesomeshot5051.easyfarms.farm.FarmKind;
import com.awesomeshot5051.easyfarms.platform.Services;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;

/** One recipe type + serializer per farm kind: easy_farms:resource_farm, easy_farms:plant_farm, easy_farms:mob_farm. */
public class ModRecipes {
    public static final Map<FarmKind, Supplier<RecipeSerializer<FarmRecipe>>> SERIALIZERS_BY_KIND = new EnumMap<>(FarmKind.class);
    public static final Map<FarmKind, Supplier<RecipeType<FarmRecipe>>> TYPES_BY_KIND = new EnumMap<>(FarmKind.class);

    public static final Supplier<RecipeSerializer<HoeTillRecipe>> HOE_TILL =
            Services.PLATFORM.register(Registries.RECIPE_SERIALIZER, "hoe_till", () -> new SimpleCraftingRecipeSerializer<>(HoeTillRecipe::new));

    static {
        for (FarmKind kind : FarmKind.values()) {
            SERIALIZERS_BY_KIND.put(kind, Services.PLATFORM.register(Registries.RECIPE_SERIALIZER, kind.id(), () -> new FarmRecipe.Serializer(kind)));
            TYPES_BY_KIND.put(kind, Services.PLATFORM.register(Registries.RECIPE_TYPE, kind.id(), () -> simpleType(EasyFarms.id(kind.id()))));
        }
    }

    private static <T extends net.minecraft.world.item.crafting.Recipe<?>> RecipeType<T> simpleType(net.minecraft.resources.ResourceLocation id) {
        return new RecipeType<>() {
            @Override
            public String toString() {
                return id.toString();
            }
        };
    }

    public static void init() {
        // class load registers the recipe types and serializers
    }
}
