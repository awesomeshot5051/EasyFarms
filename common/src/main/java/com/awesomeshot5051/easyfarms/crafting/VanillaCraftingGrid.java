package com.awesomeshot5051.easyfarms.crafting;

import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Crafting-table recipes. Each recipe's own ingredient list says what goes where (a shaped recipe's list is its whole pattern,
 * blanks included), so nothing is searched by brute force; the real {@code matches} / {@code assemble} then confirm the result,
 * which keeps recipes with extra conditions honest. Recipes with no ingredients (special recipes such as map cloning) are skipped.
 */
public final class VanillaCraftingGrid implements CraftingGridType {
    @Override
    public Optional<Crafted> craft(ServerLevel level, CraftingGrid grid, ItemStack target, List<ItemStack> available, int maxCrafts) {
        int[] counts = new int[available.size()];
        for (int i = 0; i < counts.length; i++) counts[i] = available.get(i).getCount();
        for (CraftingRecipe recipe : recipes(level, grid)) {
            int[] work = counts.clone();
            Attempt first = place(level, recipe, grid, available, work);
            if (first == null || !first.makes(target)) continue;
            // Found the recipe: run it again for as long as the items last.
            List<ItemStack> results = new ArrayList<>(), consumed = new ArrayList<>();
            int crafts = 0;
            for (Attempt a = first; a != null && crafts < maxCrafts; ) {
                crafts++;
                for (ItemStack used : a.used()) merge(consumed, used);
                merge(results, a.result());
                for (ItemStack rest : a.remainders()) merge(results, rest);
                a = place(level, recipe, grid, available, work);
                if (a != null && !a.makes(target)) a = null;
            }
            return Optional.of(new Crafted(results, consumed, crafts));
        }
        return Optional.empty();
    }

    @Override
    public boolean canCraft(ServerLevel level, CraftingGrid grid, ItemStack target) {
        for (CraftingRecipe recipe : recipes(level, grid)) {
            // Pretend the farm holds plenty of the first item each ingredient accepts.
            List<ItemStack> pretend = new ArrayList<>();
            boolean complete = true;
            for (Ingredient ingredient : recipe.getIngredients()) {
                if (ingredient.isEmpty()) continue;
                ItemStack[] options = ingredient.getItems();
                if (options.length == 0) {
                    complete = false;
                    break;
                }
                pretend.add(options[0].copyWithCount(64));
            }
            if (!complete || pretend.isEmpty()) continue;
            int[] counts = new int[pretend.size()];
            java.util.Arrays.fill(counts, 64);
            Attempt attempt = place(level, recipe, grid, pretend, counts);
            if (attempt != null && attempt.makes(target)) return true;
        }
        return false;
    }

    /** The crafting recipes this grid reads, in the order the game lists them. */
    private static List<CraftingRecipe> recipes(ServerLevel level, CraftingGrid grid) {
        RecipeType<?> type = grid.recipeType().map(BuiltInRegistries.RECIPE_TYPE::get).orElse(RecipeType.CRAFTING);
        if (type == null) return List.of();
        List<CraftingRecipe> list = new ArrayList<>();
        for (RecipeHolder<?> holder : level.getRecipeManager().getRecipes()) {
            if (holder.value().getType() == type && holder.value() instanceof CraftingRecipe recipe) list.add(recipe);
        }
        return list;
    }

    /**
     * Fills the recipe from {@code available} (spending {@code counts}) and checks it. Returns null if the grid is too small,
     * the items do not cover the ingredients, or the recipe does not accept the arrangement. {@code counts} only changes on success.
     */
    private static Attempt place(ServerLevel level, CraftingRecipe recipe, CraftingGrid grid, List<ItemStack> available, int[] counts) {
        NonNullList<Ingredient> list = recipe.getIngredients();
        List<Integer> required = new ArrayList<>();
        for (int k = 0; k < list.size(); k++) if (!list.get(k).isEmpty()) required.add(k);
        if (required.isEmpty()) return null;

        boolean shaped = recipe instanceof ShapedRecipe;
        int width = grid.width(), height = grid.height();
        if (shaped) {
            ShapedRecipe s = (ShapedRecipe) recipe;
            if (s.getWidth() > width || s.getHeight() > height || list.size() != s.getWidth() * s.getHeight()) return null;
            width = s.getWidth();
            height = s.getHeight();
        } else if (required.size() > width * height) {
            return null;
        }

        // Ingredients that few items fit go first, so a broad ingredient (any planks) does not steal their items.
        int[] work = counts.clone();
        int[] candidates = new int[list.size()];
        for (int k : required) {
            for (int i = 0; i < available.size(); i++) if (work[i] > 0 && list.get(k).test(available.get(i))) candidates[k]++;
        }
        List<Integer> order = new ArrayList<>(required);
        order.sort(Comparator.comparingInt(k -> candidates[k]));
        int[] chosen = new int[list.size()];
        for (int k : order) {
            int pick = -1;
            for (int i = 0; i < available.size() && pick < 0; i++) {
                if (work[i] > 0 && list.get(k).test(available.get(i))) pick = i;
            }
            if (pick < 0) return null;
            work[pick]--;
            chosen[k] = pick;
        }

        List<ItemStack> items = new ArrayList<>(Collections.nCopies(width * height, ItemStack.EMPTY));
        List<ItemStack> used = new ArrayList<>();
        for (int n = 0; n < required.size(); n++) {
            int k = required.get(n);
            // A shaped recipe's list is its pattern; a shapeless one goes in the first slots in order.
            items.set(shaped ? k : n, available.get(chosen[k]).copyWithCount(1));
        }
        for (ItemStack item : items) if (!item.isEmpty()) used.add(item);

        CraftingInput input = CraftingInput.of(width, height, items);
        if (!recipe.matches(input, level)) return null;
        ItemStack result = recipe.assemble(input, level.registryAccess());
        if (result.isEmpty()) return null;
        List<ItemStack> kept = new ArrayList<>();
        for (ItemStack rest : recipe.getRemainingItems(input)) if (!rest.isEmpty()) kept.add(rest);
        System.arraycopy(work, 0, counts, 0, counts.length);
        return new Attempt(used, result, kept);
    }

    private record Attempt(List<ItemStack> used, ItemStack result, List<ItemStack> remainders) {
        boolean makes(ItemStack target) {
            return result.is(target.getItem());
        }
    }

    /** Adds a stack to a list, joining an equal stack that is already there. */
    static void merge(List<ItemStack> list, ItemStack stack) {
        if (stack.isEmpty()) return;
        for (ItemStack existing : list) {
            if (ItemStack.isSameItemSameComponents(existing, stack)) {
                existing.grow(stack.getCount());
                return;
            }
        }
        list.add(stack.copy());
    }
}
