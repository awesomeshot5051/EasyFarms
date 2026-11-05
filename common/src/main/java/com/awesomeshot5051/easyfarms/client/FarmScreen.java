package com.awesomeshot5051.easyfarms.client;

import com.awesomeshot5051.easyfarms.EasyFarms;
import com.awesomeshot5051.easyfarms.gui.FarmMenu;
import com.awesomeshot5051.easyfarms.farm.FarmKind;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

/** Main farm screen, drawn on the old farms' input/output texture: three inputs + the tool on top, outputs below. */
public class FarmScreen extends AbstractContainerScreen<FarmMenu> {
    private static final Identifier BACKGROUND = Identifier.fromNamespaceAndPath(EasyFarms.MODID, "textures/gui/container/input_output.png");
    private static final int BAR_X = 52, BAR_Y = 41, BAR_W = 72, BAR_H = 4;
    private static final int FONT_COLOR = 0x404040;
    /** Vertical energy bar between the slots and the Upgrades arrow (only with a Powered card). */
    private static final int ENERGY_X = 132, ENERGY_Y = 19, ENERGY_W = 14, ENERGY_H = 49;
    /** Liquid Farm tank bar, where the output slots would be. */
    private static final int TANK_X = 52, TANK_Y = 51, TANK_W = 72, TANK_H = 16;
    private static final int ENERGY_COLOR = 0xFFD02828;

    private ArrowButton catalystButton;

    public FarmScreen(FarmMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 164);
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
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (catalystButton != null) catalystButton.visible = menu.hasCatalystCards();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
        // Cycle progress.
        g.fill(leftPos + BAR_X, topPos + BAR_Y, leftPos + BAR_X + BAR_W, topPos + BAR_Y + BAR_H, 0xFF373737);
        if (menu.running() && menu.cycle() > 0) {
            int filled = Math.min(BAR_W, BAR_W * menu.progress() / menu.cycle());
            g.fill(leftPos + BAR_X, topPos + BAR_Y, leftPos + BAR_X + filled, topPos + BAR_Y + BAR_H, 0xFF55FF55);
        }
        if (menu.kind() == FarmKind.LIQUID) drawTank(g);
        if (menu.powered()) drawEnergy(g);
    }

    /** Red fill rising from the bottom; no colour at all when empty. The value is written along the bar. */
    private void drawEnergy(GuiGraphicsExtractor g) {
        int x = leftPos + ENERGY_X, y = topPos + ENERGY_Y;
        long stored = menu.energy(), capacity = Math.max(1, menu.energyCapacity());
        frame(g, x, y, ENERGY_W, ENERGY_H);
        int filled = (int) Math.min(ENERGY_H - 2, (ENERGY_H - 2) * stored / capacity);
        if (stored > 0 && filled == 0) filled = 1;
        g.fill(x + 1, y + ENERGY_H - 1 - filled, x + ENERGY_W - 1, y + ENERGY_H - 1, ENERGY_COLOR);
        String text = Long.toString(stored);
        g.pose().pushMatrix();
        g.pose().translate(x + ENERGY_W / 2f, y + ENERGY_H / 2f);
        g.pose().rotate((float) Math.toRadians(-90));
        g.text(font, text, -font.width(text) / 2, -4, 0xFFFFFFFF, true);
        g.pose().popMatrix();
    }

    /** The tank bar: the fluid's own still texture tiled over the filled part, nothing when empty. */
    private void drawTank(GuiGraphicsExtractor g) {
        int x = leftPos + TANK_X, y = topPos + TANK_Y;
        // Hide the output slot backgrounds the shared texture draws here.
        g.fill(leftPos + TANK_X - 1, topPos + TANK_Y - 1, leftPos + TANK_X + TANK_W + 1, topPos + TANK_Y + TANK_H + 1, 0xFFC6C6C6);
        frame(g, x, y, TANK_W, TANK_H);
        var fluid = menu.fluid();
        int amount = menu.fluidAmount();
        if (fluid != null && amount > 0) {
            int filled = Math.max(1, (TANK_W - 2) * Math.min(amount, menu.fluidCapacity()) / menu.fluidCapacity());
            drawFluid(g, fluid, x + 1, y + 1, filled, TANK_H - 2);
        }
        Component text = Component.translatable("gui.easy_farms.fluid_amount", amount);
        g.text(font, text, x + (TANK_W - font.width(text)) / 2, y + (TANK_H - 8) / 2, 0xFFFFFFFF, true);
    }

    private void drawFluid(GuiGraphicsExtractor g, net.minecraft.world.level.material.Fluid fluid, int x, int y, int w, int h) {
        var sprite = minecraft.getModelManager().getFluidStateModelSet().get(fluid.defaultFluidState()).stillMaterial().sprite();
        int tint = 0xFF000000 | FluidTint.of(fluid);
        g.enableScissor(x, y, x + w, y + h);
        for (int tx = x; tx < x + w; tx += 16) for (int ty = y; ty < y + h; ty += 16) g.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, tx, ty, 16, 16, tint);
        g.disableScissor();
    }

    private static void frame(GuiGraphicsExtractor g, int x, int y, int w, int h) {
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
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        g.text(font, title, (imageWidth - font.width(title)) / 2, 9, 0xFF000000 | FONT_COLOR, false);
        g.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xFF000000 | FONT_COLOR, false);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        if (menu.powered() && inside(mouseX, mouseY, ENERGY_X, ENERGY_Y, ENERGY_W, ENERGY_H)) {
            g.setComponentTooltipForNextFrame(font, List.of(Component.translatable("gui.easy_farms.energy", menu.energy(), menu.energyCapacity())), mouseX, mouseY);
        }
        if (menu.kind() == FarmKind.LIQUID && inside(mouseX, mouseY, TANK_X, TANK_Y, TANK_W, TANK_H)) {
            var fluid = menu.fluid();
            Component name = fluid == null ? Component.translatable("gui.easy_farms.tank_empty") : FluidTint.name(fluid);
            g.setComponentTooltipForNextFrame(font, List.of(name, Component.translatable("gui.easy_farms.fluid_tank", menu.fluidAmount(), menu.fluidCapacity())), mouseX, mouseY);
        }
        if (mouseX >= leftPos + BAR_X && mouseX < leftPos + BAR_X + BAR_W && mouseY >= topPos + BAR_Y - 1 && mouseY <= topPos + BAR_Y + BAR_H + 1) {
            Component status = !menu.running() ? Component.translatable("gui.easy_farms.idle")
                    : !menu.environmentOk() ? Component.translatable("gui.easy_farms.wrong_environment")
                    : Component.translatable("gui.easy_farms.progress", menu.cycle() == 0 ? 0 : 100 * menu.progress() / menu.cycle());
            Component lifetime = menu.usesTool()
                    ? (menu.infinite() ? Component.translatable("gui.easy_farms.tool_infinite") : Component.translatable("gui.easy_farms.tool_days", menu.daysLeft()))
                    : (menu.infinite() ? Component.translatable("gui.easy_farms.inputs_infinite") : Component.translatable("gui.easy_farms.inputs_days", menu.daysLeft()));
            g.setComponentTooltipForNextFrame(font, List.of(status, lifetime), mouseX, mouseY);
        }
    }
}
