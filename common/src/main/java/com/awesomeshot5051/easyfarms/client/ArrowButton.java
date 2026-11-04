package com.awesomeshot5051.easyfarms.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/**
 * A small square button showing a left arrow (drawn in code, so no texture is needed). Its name is only a tooltip: the
 * button itself has no label, so no text scrolls over the arrow.
 */
public class ArrowButton extends Button {
    public ArrowButton(int x, int y, Component tooltipName, OnPress onPress) {
        super(x, y, 14, 14, Component.empty(), onPress, narration -> Component.translatable("gui.narrate.button", tooltipName));
        setTooltip(net.minecraft.client.gui.components.Tooltip.create(tooltipName));
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.renderWidget(g, mouseX, mouseY, partialTick);
        int color = active ? (isHoveredOrFocused() ? 0xFFFFFFA0 : 0xFFFFFFFF) : 0xFFA0A0A0;
        int cx = getX() + 3, cy = getY() + 7; // arrow tip at the left, centred vertically
        g.fill(cx + 2, cy - 1, cx + 8, cy + 1, color);        // shaft
        g.fill(cx, cy - 1, cx + 2, cy + 1, color);            // tip
        g.fill(cx + 1, cy - 2, cx + 3, cy + 2, color);        // head, widening to the right
        g.fill(cx + 2, cy - 3, cx + 4, cy + 3, color);
    }
}
