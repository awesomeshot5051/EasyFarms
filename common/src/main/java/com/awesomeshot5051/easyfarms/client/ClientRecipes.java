package com.awesomeshot5051.easyfarms.client;

import com.awesomeshot5051.easyfarms.recipe.FarmRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.Collection;
import java.util.List;

/** Clients no longer get every recipe from vanilla; the server sends the farm recipe types and they are kept here. */
public final class ClientRecipes {
    private static Collection<RecipeHolder<?>> recipes = List.of();

    private ClientRecipes() {
    }

    /** Called by the loader when the client receives the server's recipes. */
    public static void set(Collection<RecipeHolder<?>> received) {
        recipes = List.copyOf(received);
    }

    @SuppressWarnings("unchecked")
    public static Collection<RecipeHolder<FarmRecipe>> of(RecipeType<FarmRecipe> type) {
        return recipes.stream()
                .filter(holder -> holder.value().getType() == type)
                .map(holder -> (RecipeHolder<FarmRecipe>) holder)
                .toList();
    }
}
