package com.awesomeshot5051.easyfarms.client;

import com.awesomeshot5051.easyfarms.farm.FarmBlockEntity;
import com.awesomeshot5051.easyfarms.gui.FarmCraftingMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/**
 * The Crafting card's screen: slots for the items the farm should craft. A target no usable grid can make is shown in red, and
 * hovering it says so. The slots are ghosts: they only remember which item.
 */
public class FarmCraftingScreen extends AbstractContainerScreen<FarmCraftingMenu> {
    public FarmCraftingScreen(FarmCraftingMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 166;
        inventoryLabelY = 10000;
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(new ArrowButton(leftPos + imageWidth - 20, topPos + 4, Component.translatable("gui.easy_farms.back"),
                b -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0)));
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        g.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF373737);
        g.fill(leftPos + 1, topPos + 1, leftPos + imageWidth - 1, topPos + imageHeight - 1, 0xFFC6C6C6);
        for (Slot slot : menu.slots) {
            int x = leftPos + slot.x, y = topPos + slot.y;
            g.fill(x - 1, y - 1, x + 17, y + 17, 0xFF8B8B8B);
            g.fill(x, y, x + 16, y + 16, 0xFF555555);
            if (slot.index < FarmBlockEntity.CRAFT_TARGETS) {
                int status = menu.statusOf(slot.index);
                if (status == 1) g.fill(x - 1, y - 1, x + 17, y, 0xFF40C040);
                if (status == 2) g.fill(x, y, x + 16, y + 16, 0x60FF2020);
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, 8, 8, 0x404040, false);
        // Wrapped to the panel: a translation is one sentence, so every language fits whatever its length.
        int y = 52;
        for (FormattedCharSequence line : font.split(Component.translatable("gui.easy_farms.crafting_hint"), imageWidth - 16)) {
            g.drawString(font, line, 8, y, 0x606060, false);
            y += font.lineHeight + 1;
        }
    }

    /** A target slot gets its own tooltip (the item and whether a grid can craft it) instead of the plain item tooltip. */
    @Override
    protected void renderTooltip(GuiGraphics g, int mouseX, int mouseY) {
        if (hoveredSlot != null && hoveredSlot.index < FarmBlockEntity.CRAFT_TARGETS && hoveredSlot.hasItem()) {
            int status = menu.statusOf(hoveredSlot.index);
            g.renderComponentTooltip(font, java.util.List.of(hoveredSlot.getItem().getHoverName(),
                    Component.translatable(status == 2 ? "gui.easy_farms.crafting_no_recipe" : "gui.easy_farms.crafting_ok")), mouseX, mouseY);
            return;
        }
        super.renderTooltip(g, mouseX, mouseY);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
    }
}
