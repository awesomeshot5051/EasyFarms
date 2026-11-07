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
import net.minecraft.resources.Identifier;
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
            TYPES.put(kind, new RecipeType<>(Identifier.fromNamespaceAndPath(EasyFarms.MODID, kind.id()),
                    (Class<RecipeHolder<FarmRecipe>>) (Class) RecipeHolder.class));
        }
    }

    /** Recipes the plugin derived from a card: the derived holder's id -> the card that causes it. */
    static final Map<net.minecraft.resources.ResourceKey<net.minecraft.world.item.crafting.Recipe<?>>, com.awesomeshot5051.easyfarms.tier.FarmUpgrade> CARD_RECIPES = new java.util.HashMap<>();

    /** Recipes the plugin derived from a tool ability (a tool that gives effects by itself): the derived holder's id -> the ability. */
    static final Map<net.minecraft.resources.ResourceKey<net.minecraft.world.item.crafting.Recipe<?>>, com.awesomeshot5051.easyfarms.tier.ToolAbility> TOOL_ABILITY_RECIPES = new java.util.HashMap<>();

    /** The tools to show for an ability on a recipe: its display tools (with the required components applied) that the recipe's tool slot accepts. */
    static List<ItemStack> abilityTools(com.awesomeshot5051.easyfarms.tier.ToolAbility ability, net.minecraft.tags.TagKey<net.minecraft.world.item.Item> tag) {
        return ability.displayStacks().stream().filter(stack -> stack.is(tag)).toList();
    }

    /** Derived recipes that also need the Crafting card (it crafts the drops into an item that has variants): the id -> the Crafting card. */
    static final Map<net.minecraft.resources.ResourceKey<net.minecraft.world.item.crafting.Recipe<?>>, com.awesomeshot5051.easyfarms.tier.FarmUpgrade> CRAFT_CARDS = new java.util.HashMap<>();

    /** Derived recipes whose output is "any variant" (the Chisel card): the category shows one cycling slot with every variant. */
    static final java.util.Set<net.minecraft.resources.ResourceKey<net.minecraft.world.item.crafting.Recipe<?>>> VARIANT_RECIPES = new java.util.HashSet<>();

    private static IJeiRuntime runtime;
    /** Secret recipes (the Wither farm) are registered but hidden until the player discovers them. */
    private static final Map<FarmKind, List<RecipeHolder<FarmRecipe>>> SECRETS = new EnumMap<>(FarmKind.class);

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
        refreshSecrets();
        try {
            registerCraftedVariants(jeiRuntime);
        } catch (RuntimeException e) {
            EasyFarms.LOGGER.warn("Could not list crafted Chisel variants in JEI", e);
        }
    }

    /** What a crafting recipe makes from one kind of drop. */
    private record Crafted(net.minecraft.world.item.Item result, int out, int in) {
    }

    /**
     * The Crafting card plus a variant card (Chisel): a farm that crafts its drops into an item with variants (logs into planks) offers the
     * variants. Needs JEI's crafting recipes, so it runs once the runtime exists. The grids say which recipe type to search, and the recipes
     * are read through their JEI slots, so any grid and any recipe type works.
     */
    private static void registerCraftedVariants(IJeiRuntime rt) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        var upgrades = level.registryAccess().lookupOrThrow(com.awesomeshot5051.easyfarms.tier.FarmUpgrade.REGISTRY_KEY);
        com.awesomeshot5051.easyfarms.tier.FarmUpgrade craftCard = null, variantCard = null;
        List<Identifier> gridIds = List.of();
        for (var entry : upgrades.entrySet()) {
            for (var effect : entry.getValue().effects()) {
                if (craftCard == null && effect.craftsOutputs()) {
                    craftCard = entry.getValue();
                    if (effect instanceof com.awesomeshot5051.easyfarms.upgrade.UpgradeEffects.AutoCraft auto) gridIds = auto.grids();
                }
                if (variantCard == null && effect.selectsVariants()) variantCard = entry.getValue();
            }
        }
        if (craftCard == null || variantCard == null) return;
        List<mezz.jei.api.recipe.types.IRecipeType<?>> types = new java.util.ArrayList<>();
        for (var grid : com.awesomeshot5051.easyfarms.crafting.CraftingGrid.usable(level.registryAccess(), gridIds)) {
            rt.getRecipeManager().getRecipeType(grid.recipeType().orElse(Identifier.withDefaultNamespace("crafting")))
                    .filter(t -> !types.contains(t)).ifPresent(types::add);
        }
        if (types.isEmpty()) return;
        Map<net.minecraft.world.item.Item, List<Crafted>> cache = new java.util.HashMap<>();
        for (FarmKind kind : FarmKind.values()) {
            if (!craftCard.kinds().contains(kind) || !variantCard.kinds().contains(kind)) continue;
            List<RecipeHolder<FarmRecipe>> derived = new java.util.ArrayList<>();
            for (RecipeHolder<FarmRecipe> holder : com.awesomeshot5051.easyfarms.client.ClientRecipes.of(ModRecipes.TYPES_BY_KIND.get(kind).get())) {
                if (holder.value().secret()) continue;
                for (var drop : holder.value().drops()) {
                    var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(drop.item());
                    // Items that already have variants need no crafting; loot-table drops have no fixed item.
                    if (item == net.minecraft.world.item.Items.AIR || drop.lootTable().isPresent()
                            || com.awesomeshot5051.easyfarms.farm.VariantGroups.hasVariants(item)) continue;
                    List<Crafted> options = cache.computeIfAbsent(item, k -> {
                        Map<net.minecraft.world.item.Item, Crafted> found = new java.util.LinkedHashMap<>();
                        for (var type : types) for (Crafted c : craftedFrom(rt, type, k)) found.putIfAbsent(c.result(), c);
                        return List.copyOf(found.values());
                    });
                    for (Crafted crafted : options) {
                        var resultId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(crafted.result());
                        List<com.awesomeshot5051.easyfarms.farm.DropEntry> drops = new java.util.ArrayList<>();
                        for (var d : holder.value().drops()) drops.add(d == drop ? d.scaled(resultId, crafted.out(), crafted.in()) : d);
                        var id = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE,
                                Identifier.fromNamespaceAndPath(holder.id().identifier().getNamespace(), holder.id().identifier().getPath()
                                        + "/with_crafted/" + resultId.getNamespace() + "/" + resultId.getPath()));
                        derived.add(new RecipeHolder<>(id, holder.value().withDrops(drops)));
                        CARD_RECIPES.put(id, variantCard);
                        CRAFT_CARDS.put(id, craftCard);
                        VARIANT_RECIPES.add(id);
                    }
                }
            }
            if (!derived.isEmpty()) rt.getRecipeManager().addRecipes(TYPES.get(kind), derived);
        }
    }

    /** Recipes of this type that turn only {@code input} into one item that has variants (read through JEI's slots, so any recipe type works). */
    private static <R> List<Crafted> craftedFrom(IJeiRuntime rt, mezz.jei.api.recipe.types.IRecipeType<R> type, net.minecraft.world.item.Item input) {
        List<Crafted> found = new java.util.ArrayList<>();
        var manager = rt.getRecipeManager();
        var factory = rt.getJeiHelpers().getFocusFactory();
        var focus = factory.createFocus(mezz.jei.api.recipe.RecipeIngredientRole.INPUT, mezz.jei.api.constants.VanillaTypes.ITEM_STACK, new ItemStack(input));
        var category = manager.getRecipeCategory(type);
        manager.createRecipeLookup(type).limitFocus(List.of(focus)).get().forEach(recipe -> {
            try {
                var layout = manager.createRecipeLayoutDrawable(category, recipe, factory.getEmptyFocusGroup()).orElse(null);
                if (layout == null) return;
                var slots = layout.getRecipeSlotsView();
                int in = 0;
                for (var slot : slots.getSlotViews(mezz.jei.api.recipe.RecipeIngredientRole.INPUT)) {
                    if (slot.isEmpty()) continue;
                    in++;
                    // Every ingredient must accept the drop: the farm only has its drops to craft with.
                    if (slot.getItemStacks().noneMatch(s -> s.is(input))) return;
                }
                var outputs = slots.getSlotViews(mezz.jei.api.recipe.RecipeIngredientRole.OUTPUT).stream().filter(s -> !s.isEmpty()).toList();
                if (in == 0 || outputs.size() != 1) return;
                ItemStack out = outputs.get(0).getDisplayedItemStack().orElse(ItemStack.EMPTY);
                if (!out.isEmpty() && com.awesomeshot5051.easyfarms.farm.VariantGroups.hasVariants(out.getItem())) found.add(new Crafted(out.getItem(), out.getCount(), in));
            } catch (RuntimeException ignored) {
                // a recipe JEI cannot lay out is skipped
            }
        });
        return found;
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
                    .filter(h -> !com.awesomeshot5051.easyfarms.trigger.SecretRecipes.isDiscovered(player, h.id().identifier())).toList();
            List<RecipeHolder<FarmRecipe>> show = entry.getValue().stream().filter(h -> !hide.contains(h)).toList();
            runtime.getRecipeManager().hideRecipes(TYPES.get(entry.getKey()), hide);
            runtime.getRecipeManager().unhideRecipes(TYPES.get(entry.getKey()), show);
        }
    }

    @Override
    public Identifier getPluginUid() {
        return Identifier.fromNamespaceAndPath(EasyFarms.MODID, "jei_plugin");
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
            List<RecipeHolder<FarmRecipe>> recipes = new java.util.ArrayList<>(com.awesomeshot5051.easyfarms.client.ClientRecipes.of(ModRecipes.TYPES_BY_KIND.get(kind).get()));
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
        CRAFT_CARDS.clear();
        VARIANT_RECIPES.clear();
        TOOL_ABILITY_RECIPES.clear();
        // Every item that is in a variant group (so a recipe is only copied for a card when it makes a block with variants).
        java.util.Set<net.minecraft.world.item.Item> withVariants = new java.util.HashSet<>();
        net.minecraft.core.registries.BuiltInRegistries.ITEM.getTags().forEach(named -> {
            if (!named.key().location().getPath().startsWith(com.awesomeshot5051.easyfarms.farm.VariantGroups.PREFIX)) return;
            for (var holder : named) withVariants.add(holder.value());
        });
        var upgrades = level.registryAccess().lookupOrThrow(com.awesomeshot5051.easyfarms.tier.FarmUpgrade.REGISTRY_KEY);
        for (FarmKind kind : FarmKind.values()) {
            List<RecipeHolder<FarmRecipe>> derived = new java.util.ArrayList<>();
            for (RecipeHolder<FarmRecipe> holder : com.awesomeshot5051.easyfarms.client.ClientRecipes.of(ModRecipes.TYPES_BY_KIND.get(kind).get())) {
                if (holder.value().secret()) continue;
                for (var entry : upgrades.entrySet()) {
                    var upgrade = entry.getValue();
                    if (!upgrade.kinds().contains(kind)) continue;
                    if (upgrade.effects().stream().anyMatch(com.awesomeshot5051.easyfarms.upgrade.UpgradeEffect::selectsVariants)) {
                        // The Chisel card: the same recipe, but any variant of the block can come out (picked in the farm's GUI).
                        boolean makesVariants = holder.value().drops().stream()
                                .anyMatch(d -> withVariants.contains(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(d.item())));
                        if (!makesVariants) continue;
                        var variantId = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE,
                                Identifier.fromNamespaceAndPath(holder.id().identifier().getNamespace(),
                                        holder.id().identifier().getPath() + "/with/" + entry.getKey().identifier().getNamespace() + "/" + entry.getKey().identifier().getPath()));
                        derived.add(new RecipeHolder<>(variantId, holder.value()));
                        CARD_RECIPES.put(variantId, upgrade);
                        VARIANT_RECIPES.add(variantId);
                        continue;
                    }
                    List<com.awesomeshot5051.easyfarms.farm.DropEntry> drops = holder.value().drops();
                    for (var effect : upgrade.effects()) drops = effect.previewDrops(holder.value(), drops);
                    if (drops.equals(holder.value().drops())) continue;
                    var id = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE,
                            Identifier.fromNamespaceAndPath(holder.id().identifier().getNamespace(),
                                    holder.id().identifier().getPath() + "/with/" + entry.getKey().identifier().getNamespace() + "/" + entry.getKey().identifier().getPath()));
                    derived.add(new RecipeHolder<>(id, holder.value().withDrops(drops)));
                    CARD_RECIPES.put(id, upgrade);
                }
            }
            // Tool abilities (a tool that gives effects by itself, e.g. a smelter upgrade on the tool): the same, with the upgraded tools shown.
            var abilities = level.registryAccess().lookupOrThrow(com.awesomeshot5051.easyfarms.tier.ToolAbility.REGISTRY_KEY);
            for (RecipeHolder<FarmRecipe> holder : com.awesomeshot5051.easyfarms.client.ClientRecipes.of(ModRecipes.TYPES_BY_KIND.get(kind).get())) {
                if (holder.value().secret() || holder.value().tool().isEmpty()) continue;
                for (var entry : abilities.entrySet()) {
                    var ability = entry.getValue();
                    if (!ability.kinds().contains(kind) || abilityTools(ability, holder.value().tool().get()).isEmpty()) continue;
                    List<com.awesomeshot5051.easyfarms.farm.DropEntry> drops = holder.value().drops();
                    for (var effect : ability.effects()) drops = effect.previewDrops(holder.value(), drops);
                    if (drops.equals(holder.value().drops())) continue;
                    var id = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE,
                            Identifier.fromNamespaceAndPath(holder.id().identifier().getNamespace(),
                                    holder.id().identifier().getPath() + "/with_tool/" + entry.getKey().identifier().getNamespace() + "/" + entry.getKey().identifier().getPath()));
                    derived.add(new RecipeHolder<>(id, holder.value().withDrops(drops)));
                    TOOL_ABILITY_RECIPES.put(id, ability);
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
        for (var upgrade : level.registryAccess().lookupOrThrow(com.awesomeshot5051.easyfarms.tier.FarmUpgrade.REGISTRY_KEY)) {
            if (upgrade.card().isPresent()) cards.add(upgrade.stack());
        }
        registration.addExtraItemStacks(cards);
    }

    /** The "+" button: move a recipe's items into the open farm (JEI also shows what is missing). */
    @Override
    public void registerRecipeTransferHandlers(mezz.jei.api.registration.IRecipeTransferRegistration registration) {
        for (FarmKind kind : FarmKind.values()) registration.addRecipeTransferHandler(new FarmTransferInfo(kind, TYPES.get(kind)));
    }

    /**
     * Every datapack card is the same item (easy_farms:upgrade_card) told apart by its easy_farms:card component. Without
     * this JEI treats them as duplicates and lists only one.
     */
    @Override
    public void registerItemSubtypes(mezz.jei.api.registration.ISubtypeRegistration registration) {
        registration.registerFromDataComponentTypes(com.awesomeshot5051.easyfarms.registry.ModItems.UPGRADE_CARD.get(),
                com.awesomeshot5051.easyfarms.registry.ModDataComponents.CARD.get());
    }


    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        for (FarmKind kind : FarmKind.values()) {
            registration.addRecipeCatalyst(new ItemStack(EasyFarms.FARM_BLOCKS.get(kind).get()), TYPES.get(kind));
        }
    }
}
