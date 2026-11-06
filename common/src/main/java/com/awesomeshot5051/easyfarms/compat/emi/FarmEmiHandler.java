package com.awesomeshot5051.easyfarms.compat.emi;

import com.awesomeshot5051.easyfarms.farm.FarmBlockEntity;
import com.awesomeshot5051.easyfarms.gui.FarmMenu;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.handler.EmiCraftContext;
import dev.emi.emi.api.recipe.handler.StandardRecipeHandler;
import net.minecraft.world.inventory.Slot;

import java.util.ArrayList;
import java.util.List;

/**
 * EMI's "+" button for the farm screen: moves a recipe's items from the player's inventory into the farm's input and tool slots.
 * EMI colours what is missing red in the recipe, as for the crafting table.
 */
public class FarmEmiHandler implements StandardRecipeHandler<FarmMenu> {
    @Override
    public List<Slot> getInputSources(FarmMenu menu) {
        return menu.inventorySlots();
    }

    @Override
    public List<Slot> getCraftingSlots(FarmMenu menu) {
        List<Slot> list = new ArrayList<>();
        for (int i = 0; i <= FarmBlockEntity.INPUTS; i++) list.add(menu.slots.get(i));   // the inputs, then the tool
        return list;
    }

    @Override
    public List<Slot> getCraftingSlots(EmiRecipe recipe, FarmMenu menu) {
        return recipe instanceof FarmEmiRecipe farm ? menu.recipeSlots(farm.holder().value()) : getCraftingSlots(menu);
    }

    @Override
    public boolean supportsRecipe(EmiRecipe recipe) {
        return recipe instanceof FarmEmiRecipe;
    }

    @Override
    public boolean canCraft(EmiRecipe recipe, EmiCraftContext<FarmMenu> context) {
        return recipe instanceof FarmEmiRecipe farm && context.getScreenHandler().kind() == farm.holder().value().kind()
                && StandardRecipeHandler.super.canCraft(recipe, context);
    }
}
