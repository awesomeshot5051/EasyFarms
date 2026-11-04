package com.awesomeshot5051.easyfarms.compat.jei;

import com.awesomeshot5051.easyfarms.EasyFarms;
import com.awesomeshot5051.easyfarms.farm.FarmKind;
import com.awesomeshot5051.easyfarms.recipe.FarmRecipe;
import com.awesomeshot5051.easyfarms.recipe.ModRecipes;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.runtime.IJeiRuntime;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** JEI: one recipe category per farm (Resource, Plant, Mob) listing every machine recipe, with the farm as its catalyst. */
@JeiPlugin
public class EasyFarmsJeiPlugin implements IModPlugin {
    @SuppressWarnings({"unchecked", "rawtypes"})
    static final Map<FarmKind, RecipeType<RecipeHolder<FarmRecipe>>> TYPES = new EnumMap<>(FarmKind.class);

    static {
        for (FarmKind kind : FarmKind.values()) {
            TYPES.put(kind, new RecipeType<>(ResourceLocation.fromNamespaceAndPath(EasyFarms.MODID, kind.id()),
                    (Class<RecipeHolder<FarmRecipe>>) (Class) RecipeHolder.class));
        }
    }

    /** Recipes the plugin derived from a card: the derived holder's id -> the card that causes it. */
    static final Map<ResourceLocation, com.awesomeshot5051.easyfarms.tier.FarmUpgrade> CARD_RECIPES = new java.util.HashMap<>();

    private static IJeiRuntime runtime;
    /** Secret recipes (the Wither farm) are registered but hidden until the player discovers them. */
    private static final Map<FarmKind, List<RecipeHolder<FarmRecipe>>> SECRETS = new EnumMap<>(FarmKind.class);

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
        refreshSecrets();
    }

    @Override
    public void onRuntimeUnavailable() {
        runtime = null;
    }

    /** Hides the secret recipes the local player has not discovered yet and shows the ones they have. */
    public static void refreshSecrets() {
        if (runtime == null) return;
        var player = Minecraft.getInstance().player;
        for (var entry : SECRETS.entrySet()) {
            List<RecipeHolder<FarmRecipe>> hide = entry.getValue().stream()
                    .filter(h -> !com.awesomeshot5051.easyfarms.trigger.SecretRecipes.isDiscovered(player, h.id())).toList();
            List<RecipeHolder<FarmRecipe>> show = entry.getValue().stream().filter(h -> !hide.contains(h)).toList();
            runtime.getRecipeManager().hideRecipes(TYPES.get(entry.getKey()), hide);
            runtime.getRecipeManager().unhideRecipes(TYPES.get(entry.getKey()), show);
        }
    }

    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(EasyFarms.MODID, "jei_plugin");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        for (FarmKind kind : FarmKind.values()) {
            registration.addRecipeCategories(new FarmRecipeCategory(registration.getJeiHelpers().getGuiHelper(), kind, TYPES.get(kind)));
        }
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        for (FarmKind kind : FarmKind.values()) {
            List<RecipeHolder<FarmRecipe>> recipes = level.getRecipeManager().getAllRecipesFor(ModRecipes.TYPES_BY_KIND.get(kind).get());
            SECRETS.put(kind, recipes.stream().filter(r -> r.value().secret()).toList());
            registration.addRecipes(TYPES.get(kind), recipes);
        }
        registerCardRecipes(registration, level);
    }

    /**
     * For every card whose effects rewrite drops (convert_drops, from any mod's datapack), add a copy of each recipe it
     * changes with the converted drops, marked with the card. So the Plant farm's page shows "logs -> Andesite Casing
     * with the Andesite Casing card" next to the plain logs.
     */
    private static void registerCardRecipes(IRecipeRegistration registration, net.minecraft.client.multiplayer.ClientLevel level) {
        CARD_RECIPES.clear();
        var upgrades = level.registryAccess().registryOrThrow(com.awesomeshot5051.easyfarms.tier.FarmUpgrade.REGISTRY_KEY);
        for (FarmKind kind : FarmKind.values()) {
            List<RecipeHolder<FarmRecipe>> derived = new java.util.ArrayList<>();
            for (RecipeHolder<FarmRecipe> holder : level.getRecipeManager().getAllRecipesFor(ModRecipes.TYPES_BY_KIND.get(kind).get())) {
                if (holder.value().secret()) continue;
                for (var entry : upgrades.entrySet()) {
                    var upgrade = entry.getValue();
                    if (!upgrade.kinds().contains(kind)) continue;
                    List<com.awesomeshot5051.easyfarms.farm.DropEntry> drops = holder.value().drops();
                    for (var effect : upgrade.effects()) drops = effect.previewDrops(drops);
                    if (drops.equals(holder.value().drops())) continue;
                    ResourceLocation id = ResourceLocation.fromNamespaceAndPath(holder.id().getNamespace(),
                            holder.id().getPath() + "/with/" + entry.getKey().location().getNamespace() + "/" + entry.getKey().location().getPath());
                    derived.add(new RecipeHolder<>(id, holder.value().withDrops(drops)));
                    CARD_RECIPES.put(id, upgrade);
                }
            }
            registration.addRecipes(TYPES.get(kind), derived);
        }
    }

    /** Datapack cards are creative-tab items, but make sure the viewer indexes them even if it built its list first. */
    @Override
    public void registerExtraIngredients(mezz.jei.api.registration.IExtraIngredientRegistration registration) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        List<ItemStack> cards = new java.util.ArrayList<>();
        for (var upgrade : level.registryAccess().registryOrThrow(com.awesomeshot5051.easyfarms.tier.FarmUpgrade.REGISTRY_KEY)) {
            if (upgrade.card().isPresent()) cards.add(upgrade.stack());
        }
        registration.addExtraItemStacks(cards);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        for (FarmKind kind : FarmKind.values()) {
            registration.addRecipeCatalyst(new ItemStack(EasyFarms.FARM_BLOCKS.get(kind).get()), TYPES.get(kind));
        }
    }
}
