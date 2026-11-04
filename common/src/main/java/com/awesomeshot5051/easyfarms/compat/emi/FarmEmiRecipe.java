package com.awesomeshot5051.easyfarms.compat.emi;

import com.awesomeshot5051.easyfarms.farm.DropEntry;
import com.awesomeshot5051.easyfarms.recipe.FarmRecipe;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import com.awesomeshot5051.easyfarms.recipe.SizedIngredient;

import java.util.ArrayList;
import java.util.List;

public class FarmEmiRecipe implements EmiRecipe {
    private final EmiRecipeCategory category;
    private final RecipeHolder<FarmRecipe> holder;
    private final List<EmiIngredient> inputs = new ArrayList<>();
    private final EmiIngredient tool;
    private final List<EmiStack> outputs = new ArrayList<>();

    public FarmEmiRecipe(EmiRecipeCategory category, RecipeHolder<FarmRecipe> holder) {
        this.category = category;
        this.holder = holder;
        FarmRecipe r = holder.value();
        for (SizedIngredient in : r.inputs()) inputs.add(EmiIngredient.of(in.ingredient(), in.count()));
        this.tool = r.tool().isPresent() ? EmiIngredient.of(Ingredient.of(r.tool().get())) : EmiIngredient.of(List.of());
        for (DropEntry d : r.drops()) outputs.add(EmiStack.of(new ItemStack(BuiltInRegistries.ITEM.get(d.item()), Math.max(1, d.max()))));
    }

    @Override
    public EmiRecipeCategory getCategory() {
        return category;
    }

    @Override
    public ResourceLocation getId() {
        return holder.id();
    }

    @Override
    public List<EmiIngredient> getInputs() {
        List<EmiIngredient> all = new ArrayList<>(inputs);
        if (!tool.isEmpty()) all.add(tool);
        return all;
    }

    @Override
    public List<EmiStack> getOutputs() {
        return outputs;
    }

    @Override
    public int getDisplayWidth() {
        return 160;
    }

    @Override
    public int getDisplayHeight() {
        return 84;
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        int x = 4;
        for (EmiIngredient in : inputs) {
            widgets.addSlot(in, x, 4);
            x += 18;
        }
        if (!tool.isEmpty()) widgets.addSlot(tool, 4 + 3 * 18 + 6, 4);
        widgets.addText(Component.translatable("gui.easy_farms.jei.time", Math.round(holder.value().baseTicks() / 20.0)), 90, 8, 0x404040, false);
        for (int i = 0; i < outputs.size(); i++) {
            widgets.addSlot(outputs.get(i), 4 + (i % 7) * 18, 28 + (i / 7) * 18).recipeContext(this);
        }
    }
}
