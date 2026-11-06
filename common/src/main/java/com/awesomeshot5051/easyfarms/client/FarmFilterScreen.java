package com.awesomeshot5051.easyfarms.client;

import com.awesomeshot5051.easyfarms.gui.FarmFilterMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * The Filter screen: every item the farm's current recipe can output, as a grid. Click one to block it (red, the farm throws it
 * away) or allow it again. Changing the farm's inputs changes the list, and each recipe keeps its own blocked items.
 */
public class FarmFilterScreen extends AbstractContainerScreen<FarmFilterMenu> {
    private static final int COLUMNS = 9, CELL = 18, LEFT = 8, TOP = 22;

    public FarmFilterScreen(FarmFilterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        int rows = Math.max(1, (menu.items().size() + COLUMNS - 1) / COLUMNS);
        imageHeight = TOP + rows * CELL + 22;
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
        List<net.minecraft.resources.ResourceLocation> items = menu.items();
        for (int i = 0; i < items.size(); i++) {
            int x = cellX(i), y = cellY(i);
            g.fill(x - 1, y - 1, x + 17, y + 17, 0xFF8B8B8B);
            g.fill(x, y, x + 16, y + 16, 0xFF555555);
            ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(items.get(i)));
            g.renderItem(stack, x, y);
            if (menu.isBlocked(i)) {
                g.fill(x, y, x + 16, y + 16, 0x90FF2020);
                g.drawString(font, "X", x + 5, y + 4, 0xFFFFFFFF, true);
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, 8, 8, 0x404040, false);
        if (menu.items().isEmpty()) g.drawString(font, Component.translatable("gui.easy_farms.filter_empty"), 8, TOP + 4, 0x404040, false);
        else g.drawString(font, Component.translatable("gui.easy_farms.filter_hint"), 8, imageHeight - 14, 0x606060, false);
    }

    private int cellX(int i) {
        return leftPos + LEFT + (i % COLUMNS) * CELL;
    }

    private int cellY(int i) {
        return topPos + TOP + (i / COLUMNS) * CELL;
    }

    private int cellAt(double mouseX, double mouseY) {
        for (int i = 0; i < menu.items().size(); i++) {
            if (mouseX >= cellX(i) && mouseX < cellX(i) + 16 && mouseY >= cellY(i) && mouseY < cellY(i) + 16) return i;
        }
        return -1;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int cell = cellAt(mouseX, mouseY);
        if (cell >= 0) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 1 + cell);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        int cell = cellAt(mouseX, mouseY);
        if (cell >= 0) {
            ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(menu.items().get(cell)));
            g.renderComponentTooltip(font, List.of(stack.getHoverName(),
                    Component.translatable(menu.isBlocked(cell) ? "gui.easy_farms.filter_blocked" : "gui.easy_farms.filter_allowed")), mouseX, mouseY);
        }
    }
}
