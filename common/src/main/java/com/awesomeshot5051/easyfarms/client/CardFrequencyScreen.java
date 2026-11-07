package com.awesomeshot5051.easyfarms.client;

import com.awesomeshot5051.easyfarms.gui.CardFrequencyMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Sets the frequency of a DimStorage Auto Importer card: channel +/- buttons and a public/private toggle. */
public class CardFrequencyScreen extends AbstractContainerScreen<CardFrequencyMenu> {
    private Button privateButton;

    public CardFrequencyScreen(CardFrequencyMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 96;
    }

    @Override
    protected void init() {
        super.init();
        int[] labels = {1, -1, 10, -10, 100, -100};
        for (int i = 0; i < labels.length; i++) {
            final int button = i;
            String text = (labels[i] > 0 ? "+" : "") + labels[i];
            addRenderableWidget(Button.builder(Component.literal(text), b -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId, button))
                    .bounds(leftPos + 8 + (i / 2) * 54, topPos + 34 + (i % 2) * 20, 50, 18).build());
        }
        privateButton = addRenderableWidget(Button.builder(Component.empty(),
                        b -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId, CardFrequencyMenu.TOGGLE_PRIVATE))
                .bounds(leftPos + 8, topPos + 74, 110, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.easy_farms.frequency_reset"),
                        b -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId, CardFrequencyMenu.RESET))
                .bounds(leftPos + 122, topPos + 74, 46, 18).build());
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        privateButton.setMessage(Component.translatable(menu.isPrivate() ? "gui.easy_farms.frequency_private" : "gui.easy_farms.frequency_public"));
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        g.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF373737);
        g.fill(leftPos + 1, topPos + 1, leftPos + imageWidth - 1, topPos + imageHeight - 1, 0xFFC6C6C6);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, 8, 6, 0xFF404040, false);
        g.drawString(font, Component.translatable("gui.easy_farms.frequency_channel", menu.channel()), 8, 20, 0xFF404040, false);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
    }
}
