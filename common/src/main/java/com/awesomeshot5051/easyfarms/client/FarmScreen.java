package com.awesomeshot5051.easyfarms.client;

import com.awesomeshot5051.easyfarms.EasyFarms;
import com.awesomeshot5051.easyfarms.gui.FarmMenu;
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

    private ArrowButton catalystButton;

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
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (catalystButton != null) catalystButton.visible = menu.hasCatalystCards();
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
