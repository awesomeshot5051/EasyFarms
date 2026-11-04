package com.awesomeshot5051.easyfarms.client;

import com.awesomeshot5051.easyfarms.gui.FarmCatalystMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/**
 * The Catalysts screen, drawn on the vanilla chest texture like the Upgrades screen: one slot per installed card that
 * needs a catalyst. Hovering an empty slot names the card and the items it takes.
 */
public class FarmCatalystScreen extends AbstractContainerScreen<FarmCatalystMenu> {
    private static final Identifier CHEST = Identifier.withDefaultNamespace("textures/gui/container/generic_54.png");

    public FarmCatalystScreen(FarmCatalystMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 114 + menu.rows() * 18);
        inventoryLabelY = imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        // Top right corner: back to the main farm screen.
        addRenderableWidget(new ArrowButton(leftPos + imageWidth - 20, topPos + 2, Component.translatable("gui.easy_farms.back"),
                b -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0)));
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        int rows = menu.rows();
        g.blit(RenderPipelines.GUI_TEXTURED, CHEST, leftPos, topPos, 0, 0, imageWidth, rows * 18 + 17, 256, 256);
        g.blit(RenderPipelines.GUI_TEXTURED, CHEST, leftPos, topPos + rows * 18 + 17, 0, 126, imageWidth, 96, 256, 256);
        // The chest texture draws a full row; hide the cells past the last catalyst slot.
        for (int cell = menu.catalystSlotCount(); cell < rows * 9; cell++) {
            int x = leftPos + 8 + (cell % 9) * 18, y = topPos + 18 + (cell / 9) * 18;
            g.fill(x - 1, y - 1, x + 17, y + 17, 0xFFC6C6C6);
        }
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        if (menu.getCarried().isEmpty()) {
            for (int k = 0; k < menu.catalystSlotCount(); k++) {
                Slot slot = menu.slots.get(k);
                if (!slot.hasItem() && isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY)) {
                    g.setComponentTooltipForNextFrame(font, menu.hint(k), mouseX, mouseY);
                    return;
                }
            }
        }
        super.extractTooltip(g, mouseX, mouseY);
    }
}
