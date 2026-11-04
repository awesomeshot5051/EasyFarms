package com.awesomeshot5051.easyfarms.recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

import java.util.List;

/** What is in the farm's GUI: the input slots (in slot order) and the tool slot. */
public record FarmRecipeInput(List<ItemStack> inputs, ItemStack tool) implements RecipeInput {
    @Override
    public ItemStack getItem(int index) {
        return index < inputs.size() ? inputs.get(index) : tool;
    }

    @Override
    public int size() {
        return inputs.size() + 1;
    }
}
