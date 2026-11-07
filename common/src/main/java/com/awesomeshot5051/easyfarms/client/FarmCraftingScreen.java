package com.awesomeshot5051.easyfarms.client;

import com.awesomeshot5051.easyfarms.farm.FarmBlockEntity;
import com.awesomeshot5051.easyfarms.gui.FarmCraftingMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/**
 * The Crafting card's screen: slots for the items the farm should craft. A target no usable grid can make is shown in red, and
 * hovering it says so. The slots are ghosts: they only remember which item.
 */
public class FarmCraftingScreen extends AbstractContainerScreen<FarmCraftingMenu> {
    public FarmCraftingScreen(FarmCraftingMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
        inventoryLabelY = 10000;
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(new ArrowButton(leftPos + imageWidth - 20, topPos + 4, Component.translatable("gui.easy_farms.back"),
                b -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0)));
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
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
        g.text(font, title, leftPos + 8, topPos + 8, 0xFF404040, false);
        // Wrapped to the panel: a translation is one sentence, so every language fits whatever its length.
        int y = topPos + 52;
        for (var line : font.split(Component.translatable("gui.easy_farms.crafting_hint"), imageWidth - 16)) {
            g.text(font, line, leftPos + 8, y, 0xFF606060, false);
            y += font.lineHeight + 1;
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        if (hoveredSlot != null && hoveredSlot.index < FarmBlockEntity.CRAFT_TARGETS && hoveredSlot.hasItem()) {
            int status = menu.statusOf(hoveredSlot.index);
            g.setComponentTooltipForNextFrame(font, java.util.List.of(hoveredSlot.getItem().getHoverName(),
                    Component.translatable(status == 2 ? "gui.easy_farms.crafting_no_recipe" : "gui.easy_farms.crafting_ok")), mouseX, mouseY);
        }
    }
}
