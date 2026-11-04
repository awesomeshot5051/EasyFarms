package com.awesomeshot5051.easyfarms.compat.jei;

import com.awesomeshot5051.easyfarms.EasyFarms;
import com.awesomeshot5051.easyfarms.farm.DropEntry;
import com.awesomeshot5051.easyfarms.farm.FarmKind;
import com.awesomeshot5051.easyfarms.recipe.FarmRecipe;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import com.awesomeshot5051.easyfarms.recipe.SizedIngredient;

import java.util.ArrayList;
import java.util.List;

/** Inputs and tool on the first row, drops below, the time and the cards that matter on the right. */
public class FarmRecipeCategory implements IRecipeCategory<RecipeHolder<FarmRecipe>> {
    private static final int INPUT_Y = 14, OUTPUT_Y = 52;
    private static final int WIDTH = 190, HEIGHT = 112, SLOT = 18;

    private final FarmKind kind;
    private final RecipeType<RecipeHolder<FarmRecipe>> type;
    private final IDrawable icon;

    public FarmRecipeCategory(IGuiHelper helper, FarmKind kind, RecipeType<RecipeHolder<FarmRecipe>> type) {
        this.kind = kind;
        this.type = type;
        this.icon = helper.createDrawableItemStack(new ItemStack(EasyFarms.FARM_BLOCKS.get(kind).get()));
    }

    @Override
    public RecipeType<RecipeHolder<FarmRecipe>> getRecipeType() {
        return type;
    }

    @Override
    public Component getTitle() {
        return Component.translatable(EasyFarms.FARM_BLOCKS.get(kind).get().getDescriptionId());
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    @Override
    public ResourceLocation getRegistryName(RecipeHolder<FarmRecipe> recipe) {
        return recipe.id();
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<FarmRecipe> holder, IFocusGroup focuses) {
        FarmRecipe recipe = holder.value();
        int x = 4;
        for (SizedIngredient input : recipe.inputs()) {
            List<ItemStack> stacks = new ArrayList<>();
            for (ItemStack s : input.getItems()) stacks.add(s);
            builder.addSlot(RecipeIngredientRole.INPUT, x, INPUT_Y).addItemStacks(stacks);
            x += SLOT;
        }
        // The tool slot (empty for recipes that need none).
        if (recipe.tool().isPresent()) {
            builder.addSlot(RecipeIngredientRole.CATALYST, 4 + 3 * SLOT + 6, INPUT_Y)
                    .addIngredients(Ingredient.of(recipe.tool().get()))
                    .addRichTooltipCallback((slot, tooltip) -> tooltip.add(Component.translatable("gui.easy_farms.jei.tool")));
        }
        // A recipe derived from a card shows the card it needs.
        var card = EasyFarmsJeiPlugin.CARD_RECIPES.get(holder.id());
        if (card != null) {
            builder.addSlot(RecipeIngredientRole.CATALYST, 4, HEIGHT - SLOT - 4).addItemStack(card.stack())
                    .addRichTooltipCallback((slot, tooltip) -> tooltip.add(Component.translatable("gui.easy_farms.jei.card_needed")));
        }
        int i = 0;
        for (DropEntry drop : recipe.drops()) {
            int col = i % 7, row = i / 7;
            ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(drop.item()), Math.max(1, drop.max()));
            builder.addSlot(RecipeIngredientRole.OUTPUT, 4 + col * SLOT, OUTPUT_Y + row * SLOT)
                    .addItemStack(stack)
                    .addRichTooltipCallback((slot, tooltip) -> describe(drop, tooltip));
            i++;
        }
        // What the Inscriber card makes instead (AE2 prints and dust), listed after the normal drops.
        if (recipe.inscriber().isPresent()) {
            var ins = recipe.inscriber().get();
            for (DropEntry drop : ins.drops()) {
                Item item = BuiltInRegistries.ITEM.get(drop.item());
                if (item == net.minecraft.world.item.Items.AIR) continue;
                int col = i % 7, row = i / 7;
                builder.addSlot(RecipeIngredientRole.OUTPUT, 4 + col * SLOT, OUTPUT_Y + row * SLOT)
                        .addItemStack(new ItemStack(item, Math.max(1, drop.max())))
                        .addRichTooltipCallback((slot, tooltip) -> {
                            tooltip.add(Component.translatable("gui.easy_farms.jei.card_inscriber").withStyle(net.minecraft.ChatFormatting.AQUA));
                            ins.press().ifPresent(id -> tooltip.add(Component.translatable("gui.easy_farms.jei.press", name(id))));
                            describe(drop, tooltip);
                        });
                i++;
            }
        }
    }

    private static void describe(DropEntry d, mezz.jei.api.gui.builder.ITooltipBuilder tooltip) {
        tooltip.add(Component.translatable("gui.easy_farms.jei.count", d.min(), d.max()));
        if (d.chance() < 1F) tooltip.add(Component.translatable("gui.easy_farms.jei.chance", Math.round(d.chance() * 1000) / 10.0));
        d.fortuneMax().ifPresent(m -> tooltip.add(Component.translatable("gui.easy_farms.jei.fortune", d.fortuneMin().orElse(d.min()), m)));
        d.lootingMax().ifPresent(m -> tooltip.add(Component.translatable("gui.easy_farms.jei.looting", d.lootingMin().orElse(d.min()), m)));
        if (d.silkOnly()) tooltip.add(Component.translatable("gui.easy_farms.jei.silk_only"));
        d.silkItem().ifPresent(id -> tooltip.add(Component.translatable("gui.easy_farms.jei.silk", name(id))));
        d.smeltItem().ifPresent(id -> tooltip.add(Component.translatable("gui.easy_farms.jei.smelter", name(id))));
        d.fireItem().ifPresent(id -> tooltip.add(Component.translatable("gui.easy_farms.jei.fire", name(id))));
    }

    private static String blockName(String entry) {
        if (entry.startsWith("#")) return entry.substring(1);
        return BuiltInRegistries.BLOCK.get(ResourceLocation.parse(entry)).getName().getString();
    }

    private static Component name(ResourceLocation id) {
        Item item = BuiltInRegistries.ITEM.get(id);
        return item.getDescription();
    }

    /** Info text sits right of the slots, starts further left than before, and wraps instead of running off the page. */
    @Override
    public void draw(RecipeHolder<FarmRecipe> holder, IRecipeSlotsView slots, GuiGraphics g, double mouseX, double mouseY) {
        FarmRecipe r = holder.value();
        var font = Minecraft.getInstance().font;
        g.drawString(font, Component.translatable("gui.easy_farms.jei.inputs"), 4, 3, 0xFF404040, false);
        g.drawString(font, Component.translatable("gui.easy_farms.jei.outputs"), 4, OUTPUT_Y - 12, 0xFF404040, false);
        final int x = 86, maxW = WIDTH - x - 2;
        List<Component> lines = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();
        lines.add(Component.translatable("gui.easy_farms.jei.time", Math.round(r.baseTicks() / 20.0)));
        colors.add(0xFF404040);
        java.util.function.Consumer<Component> gray = c -> { lines.add(c); colors.add(0xFF808080); };
        if (r.smeltable()) gray.accept(Component.translatable("gui.easy_farms.jei.card_smelter"));
        if (r.nether()) gray.accept(Component.translatable("gui.easy_farms.jei.card_nether"));
        r.inscriber().ifPresent(ins -> gray.accept(ins.press().isPresent()
                ? Component.translatable("gui.easy_farms.jei.card_inscriber").append(" + ").append(Component.translatable("gui.easy_farms.jei.press", name(ins.press().get())))
                : Component.translatable("gui.easy_farms.jei.card_inscriber")));
        r.environment().ifPresent(env -> {
            env.dimension().ifPresent(d -> gray.accept(Component.translatable("gui.easy_farms.jei.dimension", Component.translatable("dimension." + d.getNamespace() + "." + d.getPath()))));
            if (!env.blockBelow().isEmpty()) gray.accept(Component.translatable("gui.easy_farms.jei.over", env.blockBelow().stream().map(FarmRecipeCategory::blockName).collect(java.util.stream.Collectors.joining(", "))));
        });
        if (r.minTierRank() > 0) gray.accept(Component.translatable("gui.easy_farms.jei.min_tier", r.minTierRank()));
        var card = EasyFarmsJeiPlugin.CARD_RECIPES.get(holder.id());
        if (card != null) {
            g.drawString(font, Component.translatable("gui.easy_farms.jei.with_card", card.stack().getHoverName()), 26, HEIGHT - SLOT, 0xFF404040, false);
        }
        int y = 14;
        for (int i = 0; i < lines.size(); i++) {
            for (var part : font.split(lines.get(i), maxW)) {
                if (y + 9 > HEIGHT) return;
                g.drawString(font, part, x, y, colors.get(i), false);
                y += 10;
            }
        }
    }
}
