package com.awesomeshot5051.easyfarms.compat.emi;

import com.awesomeshot5051.easyfarms.EasyFarms;
import com.awesomeshot5051.easyfarms.farm.FarmKind;
import com.awesomeshot5051.easyfarms.recipe.FarmRecipe;
import com.awesomeshot5051.easyfarms.recipe.ModRecipes;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.EnumMap;
import java.util.Map;

/** EMI: the same three categories as JEI, with each farm as the workstation. */
@EmiEntrypoint
public class EasyFarmsEmiPlugin implements EmiPlugin {
    /** Called when the player discovers a secret recipe: EMI rebuilds its recipe list, which now includes it. */
    public static void reload() {
        dev.emi.emi.runtime.EmiReloadManager.reload();
    }

    @Override
    public void register(EmiRegistry registry) {
        Map<FarmKind, EmiRecipeCategory> categories = new EnumMap<>(FarmKind.class);
        for (FarmKind kind : FarmKind.values()) {
            EmiStack icon = EmiStack.of(new ItemStack(EasyFarms.FARM_BLOCKS.get(kind).get()));
            EmiRecipeCategory category = new EmiRecipeCategory(ResourceLocation.fromNamespaceAndPath(EasyFarms.MODID, kind.id()), icon);
            categories.put(kind, category);
            registry.addCategory(category);
            registry.addWorkstation(category, icon);
        }
        registry.addRecipeHandler(com.awesomeshot5051.easyfarms.gui.ModMenus.FARM.get(), new FarmEmiHandler());
        for (FarmKind kind : FarmKind.values()) {
            for (RecipeHolder<FarmRecipe> holder : registry.getRecipeManager().getAllRecipesFor(ModRecipes.TYPES_BY_KIND.get(kind).get())) {
                if (!holder.value().secret() || com.awesomeshot5051.easyfarms.trigger.SecretRecipes.isDiscovered(net.minecraft.client.Minecraft.getInstance().player, holder.id())) registry.addRecipe(new FarmEmiRecipe(categories.get(kind), holder));
            }
        }
    }
}
