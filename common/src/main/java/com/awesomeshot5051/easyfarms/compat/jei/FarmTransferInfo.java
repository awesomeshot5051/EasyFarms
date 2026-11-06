package com.awesomeshot5051.easyfarms.compat.jei;

import com.awesomeshot5051.easyfarms.farm.FarmKind;
import com.awesomeshot5051.easyfarms.gui.FarmMenu;
import com.awesomeshot5051.easyfarms.gui.ModMenus;
import com.awesomeshot5051.easyfarms.recipe.FarmRecipe;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferInfo;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.List;
import java.util.Optional;

/**
 * The "+" button in JEI: moves a recipe's items from the player's inventory into the open farm's input and tool slots. JEI does the
 * moving and, when something is missing, highlights the missing ingredients in red and says what is missing.
 */
public record FarmTransferInfo(FarmKind kind, RecipeType<RecipeHolder<FarmRecipe>> type) implements IRecipeTransferInfo<FarmMenu, RecipeHolder<FarmRecipe>> {
    @Override
    public Class<? extends FarmMenu> getContainerClass() {
        return FarmMenu.class;
    }

    @Override
    public Optional<MenuType<FarmMenu>> getMenuType() {
        return Optional.of(ModMenus.FARM.get());
    }

    @Override
    public RecipeType<RecipeHolder<FarmRecipe>> getRecipeType() {
        return type;
    }

    @Override
    public boolean canHandle(FarmMenu menu, RecipeHolder<FarmRecipe> recipe) {
        return menu.kind() == kind;
    }

    @Override
    public List<Slot> getRecipeSlots(FarmMenu menu, RecipeHolder<FarmRecipe> recipe) {
        return menu.recipeSlots(recipe.value());
    }

    @Override
    public List<Slot> getInventorySlots(FarmMenu menu, RecipeHolder<FarmRecipe> recipe) {
        return menu.inventorySlots();
    }
}
