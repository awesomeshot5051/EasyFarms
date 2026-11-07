package com.awesomeshot5051.easyfarms.client;

import com.awesomeshot5051.easyfarms.EasyFarms;
import com.awesomeshot5051.easyfarms.gui.FarmMenu;
import com.awesomeshot5051.easyfarms.farm.FarmKind;
import com.mojang.math.Axis;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

/** Main farm screen, drawn on the old farms' input/output texture: three inputs + the tool on top, outputs below. */
public class FarmScreen extends AbstractContainerScreen<FarmMenu> {
    private static final ResourceLocation BACKGROUND = ResourceLocation.fromNamespaceAndPath(EasyFarms.MODID, "textures/gui/container/input_output.png");
    private static final int BAR_X = 52, BAR_Y = 41, BAR_W = 72, BAR_H = 4;
    private static final int FONT_COLOR = 0x404040;
    /** Vertical energy bar between the slots and the Upgrades arrow (only with a Powered card). */
    private static final int ENERGY_X = 132, ENERGY_Y = 19, ENERGY_W = 14, ENERGY_H = 49;
    /** Liquid Farm tank bar, where the output slots would be. */
    private static final int TANK_X = 52, TANK_Y = 51, TANK_W = 72, TANK_H = 16;
    private static final int ENERGY_COLOR = 0xFFD02828;

    private ArrowButton catalystButton, filterButton, sideButton, craftingButton, variantButton;
    private net.minecraft.client.gui.components.Button xpButton;
    /** Auto-Export card: the side panel (a pop-out left of the screen) is open. */
    private boolean sidePanelOpen;
    private static final String[] FACE_KEYS = {"front", "back", "left", "right", "top", "bottom"};
    /** Cell (column, row) of each relative face in the side panel's unfolded cross. */
    private static final int[][] FACE_CELLS = {{1, 1}, {3, 1}, {0, 1}, {2, 1}, {1, 0}, {1, 2}};
    private static final int PANEL_W = 80, PANEL_H = 74, CELL = 18;
    /** Chisel card: the variant pop-out to the right of the screen (a grid of the variants of the chosen output). */
    private static final int VAR_COLS = 6, VAR_ROWS = 5, VAR_W = VAR_COLS * CELL + 10, VAR_H = 34 + VAR_ROWS * CELL + 6;
    private boolean variantPanelOpen;
    private int variantSourceIndex, variantScroll;
    private List<net.minecraft.world.item.Item> variantList = List.of();
    private List<net.minecraft.resources.ResourceLocation> variantSources = List.of();

    public FarmScreen(FarmMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 164;
        inventoryLabelY = imageHeight - 96 + 3;
    }

    @Override
    protected void init() {
        super.init();
        // Top right corner: opens the Upgrades screen.
        addRenderableWidget(new ArrowButton(leftPos + imageWidth - 20, topPos + 5, Component.translatable("gui.easy_farms.upgrades"),
                b -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0)));
        // Under it, only while an installed card needs a catalyst: opens the Catalysts screen.
        catalystButton = addRenderableWidget(new ArrowButton(leftPos + imageWidth - 20, topPos + 21, Component.translatable("gui.easy_farms.catalysts"),
                b -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 1)));
        catalystButton.visible = menu.hasCatalystCards();
        // Under that, only with a Filter card: its own screen.
        filterButton = addRenderableWidget(new ArrowButton(leftPos + imageWidth - 20, topPos + 37, Component.translatable("gui.easy_farms.filter"),
                b -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 2)));
        // Experience card: hands the stored experience to the player.
        xpButton = addRenderableWidget(net.minecraft.client.gui.components.Button.builder(Component.literal("xp"),
                        b -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 3))
                .bounds(leftPos + imageWidth - 22, topPos + 53, 18, 14).build());
        // Left edge, only with an Auto-Export card: opens the side panel that sets what each face of the farm does.
        sideButton = addRenderableWidget(new ArrowButton(leftPos - 16, topPos + 5, Component.translatable("gui.easy_farms.sides"),
                b -> sidePanelOpen = !sidePanelOpen));
        // Left edge, under the sides button, only with a Crafting card: the screen that sets what the farm crafts.
        craftingButton = addRenderableWidget(new ArrowButton(leftPos - 16, topPos + 21, Component.translatable("gui.easy_farms.crafting"),
                b -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 10)));
        // Left edge, under the crafting button, only with a Chisel card: the pop-out listing the variants of the output.
        variantButton = addRenderableWidget(new ArrowButton(leftPos - 16, topPos + 37, Component.translatable("gui.easy_farms.variants"),
                b -> variantPanelOpen = !variantPanelOpen));
        updateCardWidgets();
    }

    private void updateCardWidgets() {
        if (variantButton != null) variantButton.visible = menu.hasVariantCard();
        if (!menu.hasVariantCard()) variantPanelOpen = false;
        refreshVariants();
        if (craftingButton != null) craftingButton.visible = menu.hasCraftingCard();
        if (catalystButton != null) catalystButton.visible = menu.hasCatalystCards();
        if (filterButton != null) filterButton.visible = menu.hasFilterCard();
        if (xpButton != null) {
            xpButton.visible = menu.hasExperienceCard();
            xpButton.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("gui.easy_farms.collect_xp", menu.xp())));
        }
        if (sideButton != null) sideButton.visible = menu.hasExportCard();
        if (!menu.hasExportCard()) sidePanelOpen = false;
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        updateCardWidgets();
    }

    /** Rebuilds the variant list of the selected output (cheap: only the farm's one or two outputs). */
    private void refreshVariants() {
        variantSources = menu.hasVariantCard() ? menu.variantSources() : List.of();
        if (variantSourceIndex >= variantSources.size()) variantSourceIndex = 0;
        if (variantPanelOpen && !variantSources.isEmpty()) {
            variantList = com.awesomeshot5051.easyfarms.farm.VariantGroups.variants(
                    net.minecraft.core.registries.BuiltInRegistries.ITEM.get(variantSources.get(variantSourceIndex)));
        } else {
            variantList = List.of();
        }
        int maxScroll = Math.max(0, (variantList.size() + VAR_COLS - 1) / VAR_COLS - VAR_ROWS);
        variantScroll = Math.max(0, Math.min(variantScroll, maxScroll));
    }

    private int variantPanelX() {
        return leftPos + imageWidth + 2;
    }

    private int variantPanelY() {
        return topPos + 5;
    }

    /** -1 when the mouse is not over a source tab of the open pop-out. */
    private int variantTabAt(double mouseX, double mouseY) {
        if (!variantPanelOpen) return -1;
        for (int i = 0; i < variantSources.size() && i < VAR_COLS; i++) {
            int x = variantPanelX() + 5 + i * CELL, y = variantPanelY() + 14;
            if (mouseX >= x && mouseX < x + CELL - 2 && mouseY >= y && mouseY < y + CELL - 2) return i;
        }
        return -1;
    }

    /** Index into {@link #variantList} under the mouse, or -1. */
    private int variantAt(double mouseX, double mouseY) {
        if (!variantPanelOpen) return -1;
        int gx = variantPanelX() + 5, gy = variantPanelY() + 34;
        if (mouseX < gx || mouseY < gy) return -1;
        int col = (int) ((mouseX - gx) / CELL), row = (int) ((mouseY - gy) / CELL);
        if (col >= VAR_COLS || row >= VAR_ROWS || (mouseX - gx) % CELL >= CELL - 2 || (mouseY - gy) % CELL >= CELL - 2) return -1;
        int index = (row + variantScroll) * VAR_COLS + col;
        return index < variantList.size() ? index : -1;
    }

    private void drawVariantPanel(GuiGraphics g) {
        int x = variantPanelX(), y = variantPanelY();
        g.fill(x, y, x + VAR_W, y + VAR_H, 0xFF373737);
        g.fill(x + 1, y + 1, x + VAR_W - 1, y + VAR_H - 1, 0xFFC6C6C6);
        g.drawString(font, Component.translatable("gui.easy_farms.variants"), x + 5, y + 4, 0xFF404040, false);
        for (int i = 0; i < variantSources.size() && i < VAR_COLS; i++) {
            int cx = x + 5 + i * CELL, cy = y + 14;
            g.fill(cx, cy, cx + CELL - 2, cy + CELL - 2, i == variantSourceIndex ? 0xFF5FA05F : 0xFF373737);
            g.fill(cx + 1, cy + 1, cx + CELL - 3, cy + CELL - 3, 0xFF8B8B8B);
            g.renderItem(new net.minecraft.world.item.ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(variantSources.get(i))), cx + 1, cy + 1);
        }
        if (variantSources.isEmpty()) {
            g.drawString(font, Component.translatable("gui.easy_farms.variants_none"), x + 5, y + 20, 0xFF404040, false);
            return;
        }
        var chosen = menu.variantChoice(variantSources.get(variantSourceIndex));
        for (int row = 0; row < VAR_ROWS; row++) {
            for (int col = 0; col < VAR_COLS; col++) {
                int index = (row + variantScroll) * VAR_COLS + col;
                int cx = x + 5 + col * CELL, cy = y + 34 + row * CELL;
                boolean has = index < variantList.size();
                boolean picked = has && net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(variantList.get(index)).equals(chosen);
                g.fill(cx, cy, cx + CELL - 2, cy + CELL - 2, picked ? 0xFF5FA05F : 0xFF373737);
                g.fill(cx + 1, cy + 1, cx + CELL - 3, cy + CELL - 3, 0xFF8B8B8B);
                if (has) g.renderItem(new net.minecraft.world.item.ItemStack(variantList.get(index)), cx + 1, cy + 1);
            }
        }
        int rows = (variantList.size() + VAR_COLS - 1) / VAR_COLS;
        if (rows > VAR_ROWS) {
            // Scroll bar on the right edge of the grid.
            int barX = x + VAR_W - 4, barTop = y + 34, barH = VAR_ROWS * CELL - 2;
            int thumbH = Math.max(6, barH * VAR_ROWS / rows);
            int thumbY = barTop + (barH - thumbH) * variantScroll / (rows - VAR_ROWS);
            g.fill(barX, barTop, barX + 2, barTop + barH, 0xFF8B8B8B);
            g.fill(barX, thumbY, barX + 2, thumbY + thumbH, 0xFF373737);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (variantPanelOpen && mouseX >= variantPanelX() && mouseX < variantPanelX() + VAR_W && mouseY >= variantPanelY() && mouseY < variantPanelY() + VAR_H) {
            variantScroll -= (int) Math.signum(scrollY);
            refreshVariants();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private int panelX() {
        return leftPos - 16 - PANEL_W;
    }

    private int faceX(int face) {
        return panelX() + 4 + FACE_CELLS[face][0] * CELL;
    }

    private int faceY(int face) {
        return topPos + 18 + FACE_CELLS[face][1] * CELL;
    }

    /** -1 when the mouse is not over a face cell of the open side panel. */
    private int faceAt(double mouseX, double mouseY) {
        if (!sidePanelOpen) return -1;
        for (int f = 0; f < 6; f++) {
            if (mouseX >= faceX(f) && mouseX < faceX(f) + CELL - 2 && mouseY >= faceY(f) && mouseY < faceY(f) + CELL - 2) return f;
        }
        return -1;
    }

    private static int modeColor(int mode) {
        return switch (mode) {
            case 1 -> 0xFF4A7BD0;   // input
            case 2 -> 0xFFE08A2E;   // output
            case 3 -> 0xFF404040;   // off
            default -> 0xFF5FA05F;  // both (the farm's normal behaviour)
        };
    }

    private static String modeLetter(int mode) {
        return switch (mode) {
            case 1 -> "I";
            case 2 -> "O";
            case 3 -> "-";
            default -> "IO";
        };
    }

    private void drawSidePanel(GuiGraphics g) {
        int x = panelX(), y = topPos + 5;
        g.fill(x, y, x + PANEL_W, y + PANEL_H + 14, 0xFF373737);
        g.fill(x + 1, y + 1, x + PANEL_W - 1, y + PANEL_H + 13, 0xFFC6C6C6);
        g.drawString(font, Component.translatable("gui.easy_farms.sides"), x + 5, y + 4, 0xFF404040, false);
        for (int f = 0; f < 6; f++) {
            int cx = faceX(f), cy = faceY(f), mode = menu.sideMode(f);
            g.fill(cx, cy, cx + CELL - 2, cy + CELL - 2, 0xFF373737);
            g.fill(cx + 1, cy + 1, cx + CELL - 3, cy + CELL - 3, modeColor(mode));
            String letter = modeLetter(mode);
            g.drawString(font, letter, cx + (CELL - 2 - font.width(letter)) / 2, cy + 5, 0xFFFFFFFF, true);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int tab = variantTabAt(mouseX, mouseY);
        if (tab >= 0) {
            variantSourceIndex = tab;
            variantScroll = 0;
            refreshVariants();
            return true;
        }
        int variant = variantAt(mouseX, mouseY);
        if (variant >= 0) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId,
                    FarmMenu.VARIANT_BUTTON + variantSourceIndex * FarmMenu.VARIANT_STRIDE + variant);
            return true;
        }
        int face = faceAt(mouseX, mouseY);
        if (face >= 0) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 4 + face);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        g.blit(BACKGROUND, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        // Cycle progress.
        g.fill(leftPos + BAR_X, topPos + BAR_Y, leftPos + BAR_X + BAR_W, topPos + BAR_Y + BAR_H, 0xFF373737);
        if (menu.running() && menu.cycle() > 0) {
            int filled = Math.min(BAR_W, BAR_W * menu.progress() / menu.cycle());
            g.fill(leftPos + BAR_X, topPos + BAR_Y, leftPos + BAR_X + filled, topPos + BAR_Y + BAR_H, 0xFF55FF55);
        }
        if (menu.kind() == FarmKind.LIQUID) drawTank(g);
        if (menu.powered()) drawEnergy(g);
        if (sidePanelOpen) drawSidePanel(g);
        if (variantPanelOpen) drawVariantPanel(g);
    }

    /** Red fill rising from the bottom; no colour at all when empty. The value is written along the bar. */
    private void drawEnergy(GuiGraphics g) {
        int x = leftPos + ENERGY_X, y = topPos + ENERGY_Y;
        long stored = menu.energy(), capacity = Math.max(1, menu.energyCapacity());
        frame(g, x, y, ENERGY_W, ENERGY_H);
        int filled = (int) Math.min(ENERGY_H - 2, (ENERGY_H - 2) * stored / capacity);
        if (stored > 0 && filled == 0) filled = 1;
        g.fill(x + 1, y + ENERGY_H - 1 - filled, x + ENERGY_W - 1, y + ENERGY_H - 1, ENERGY_COLOR);
        String text = Long.toString(stored);
        g.pose().pushPose();
        g.pose().translate(x + ENERGY_W / 2f, y + ENERGY_H / 2f, 0);
        g.pose().mulPose(Axis.ZP.rotationDegrees(-90));
        g.drawString(font, text, -font.width(text) / 2, -4, 0xFFFFFFFF, true);
        g.pose().popPose();
    }

    /** The tank bar: the fluid's own still texture tiled over the filled part, nothing when empty. */
    private void drawTank(GuiGraphics g) {
        int x = leftPos + TANK_X, y = topPos + TANK_Y;
        // Hide the output slot backgrounds the shared texture draws here.
        g.fill(leftPos + TANK_X - 1, topPos + TANK_Y - 1, leftPos + TANK_X + TANK_W + 1, topPos + TANK_Y + TANK_H + 1, 0xFFC6C6C6);
        frame(g, x, y, TANK_W, TANK_H);
        var fluid = menu.fluidKey();
        int amount = menu.fluidAmount();
        if (fluid != null && amount > 0 && FluidTint.known(fluid)) {
            int filled = Math.max(1, (TANK_W - 2) * Math.min(amount, menu.fluidCapacity()) / menu.fluidCapacity());
            drawFluid(g, fluid, x + 1, y + 1, filled, TANK_H - 2);
        }
        Component text = Component.translatable("gui.easy_farms.fluid_amount", amount);
        g.drawString(font, text, x + (TANK_W - font.width(text)) / 2, y + (TANK_H - 8) / 2, 0xFFFFFFFF, true);
    }

    private void drawFluid(GuiGraphics g, net.minecraft.resources.ResourceLocation fluid, int x, int y, int w, int h) {
        var sprite = FluidTint.sprite(fluid);
        int tint = FluidTint.tint(fluid);
        g.setColor((tint >> 16 & 255) / 255f, (tint >> 8 & 255) / 255f, (tint & 255) / 255f, 1f);
        g.enableScissor(x, y, x + w, y + h);
        for (int tx = x; tx < x + w; tx += 16) for (int ty = y; ty < y + h; ty += 16) g.blit(tx, ty, 0, 16, 16, sprite);
        g.disableScissor();
        g.setColor(1f, 1f, 1f, 1f);
    }

    private static void frame(GuiGraphics g, int x, int y, int w, int h) {
        int c = 0xFF373737;
        g.fill(x, y, x + w, y + 1, c);
        g.fill(x, y + h - 1, x + w, y + h, c);
        g.fill(x, y, x + 1, y + h, c);
        g.fill(x + w - 1, y, x + w, y + h, c);
    }

    private boolean inside(int mouseX, int mouseY, int x, int y, int w, int h) {
        return mouseX >= leftPos + x && mouseX < leftPos + x + w && mouseY >= topPos + y && mouseY < topPos + y + h;
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, (imageWidth - font.width(title)) / 2, 9, FONT_COLOR, false);
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, FONT_COLOR, false);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
        int hoveredVariant = variantAt(mouseX, mouseY);
        if (hoveredVariant >= 0) {
            g.renderTooltip(font, new net.minecraft.world.item.ItemStack(variantList.get(hoveredVariant)), mouseX, mouseY);
        }
        int hoveredTab = variantTabAt(mouseX, mouseY);
        if (hoveredTab >= 0) {
            g.renderTooltip(font, new net.minecraft.world.item.ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(variantSources.get(hoveredTab))), mouseX, mouseY);
        }
        int hoveredFace = faceAt(mouseX, mouseY);
        if (hoveredFace >= 0) {
            g.renderComponentTooltip(font, List.of(Component.translatable("gui.easy_farms.face." + FACE_KEYS[hoveredFace]),
                    Component.translatable("gui.easy_farms.side_mode." + menu.sideMode(hoveredFace)),
                    Component.translatable("gui.easy_farms.side_click").withStyle(net.minecraft.ChatFormatting.GRAY)), mouseX, mouseY);
        }
        if (menu.powered() && inside(mouseX, mouseY, ENERGY_X, ENERGY_Y, ENERGY_W, ENERGY_H)) {
            g.renderComponentTooltip(font, List.of(Component.translatable("gui.easy_farms.energy", menu.energy(), menu.energyCapacity())), mouseX, mouseY);
        }
        if (menu.kind() == FarmKind.LIQUID && inside(mouseX, mouseY, TANK_X, TANK_Y, TANK_W, TANK_H)) {
            var fluid = menu.fluidKey();
            Component name = fluid == null || menu.fluidAmount() <= 0 ? Component.translatable("gui.easy_farms.tank_empty") : FluidTint.name(fluid);
            g.renderComponentTooltip(font, List.of(name, Component.translatable("gui.easy_farms.fluid_tank", menu.fluidAmount(), menu.fluidCapacity())), mouseX, mouseY);
        }
        if (mouseX >= leftPos + BAR_X && mouseX < leftPos + BAR_X + BAR_W && mouseY >= topPos + BAR_Y - 1 && mouseY <= topPos + BAR_Y + BAR_H + 1) {
            Component status = !menu.running() ? Component.translatable("gui.easy_farms.idle")
                    : !menu.environmentOk() ? Component.translatable("gui.easy_farms.wrong_environment")
                    : Component.translatable("gui.easy_farms.progress", menu.cycle() == 0 ? 0 : 100 * menu.progress() / menu.cycle());
            Component lifetime = menu.usesTool()
                    ? (menu.infinite() ? Component.translatable("gui.easy_farms.tool_infinite") : Component.translatable("gui.easy_farms.tool_days", menu.daysLeft()))
                    : (menu.infinite() ? Component.translatable("gui.easy_farms.inputs_infinite") : Component.translatable("gui.easy_farms.inputs_days", menu.daysLeft()));
            g.renderComponentTooltip(font, List.of(status, lifetime), mouseX, mouseY);
        }
    }
}
