package com.awesomeshot5051.easyfarms.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/** A small square button showing a left arrow (drawn in code, so no texture is needed). */
public class ArrowButton extends Button.Plain {
    public ArrowButton(int x, int y, Component tooltipName, OnPress onPress) {
        super(x, y, 14, 14, tooltipName, onPress, DEFAULT_NARRATION);
        setTooltip(net.minecraft.client.gui.components.Tooltip.create(tooltipName));
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        extractDefaultSprite(g);
        int color = active ? (isHoveredOrFocused() ? 0xFFFFFFA0 : 0xFFFFFFFF) : 0xFFA0A0A0;
        int cx = getX() + 3, cy = getY() + 7; // arrow tip at the left, centred vertically
        g.fill(cx + 2, cy - 1, cx + 8, cy + 1, color);        // shaft
        g.fill(cx, cy - 1, cx + 2, cy + 1, color);            // tip
        g.fill(cx + 1, cy - 2, cx + 3, cy + 2, color);        // head, widening to the right
        g.fill(cx + 2, cy - 3, cx + 4, cy + 3, color);
    }
}
