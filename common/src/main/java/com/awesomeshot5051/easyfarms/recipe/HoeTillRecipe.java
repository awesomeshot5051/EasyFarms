package com.awesomeshot5051.easyfarms.recipe;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/** A hoe and a grass block make farmland, the soil crop farms need. The hoe survives but loses 1 durability. */
public class HoeTillRecipe extends CustomRecipe {
    public HoeTillRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        int hoes = 0, grass = 0, other = 0;
        for (ItemStack stack : input.items()) {
            if (stack.isEmpty()) continue;
            if (stack.getItem() instanceof HoeItem && stack.isDamageableItem()) hoes++;
            else if (stack.is(Items.GRASS_BLOCK)) grass++;
            else other++;
        }
        return hoes == 1 && grass == 1 && other == 0;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        return new ItemStack(Items.FARMLAND);
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        NonNullList<ItemStack> remaining = NonNullList.withSize(input.size(), ItemStack.EMPTY);
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.getItem() instanceof HoeItem && stack.isDamageableItem()) {
                ItemStack worn = stack.copy();
                worn.setDamageValue(worn.getDamageValue() + 1);
                remaining.set(i, worn.getDamageValue() >= worn.getMaxDamage() ? ItemStack.EMPTY : worn);
            }
        }
        return remaining;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<HoeTillRecipe> getSerializer() {
        return ModRecipes.HOE_TILL.get();
    }
}
