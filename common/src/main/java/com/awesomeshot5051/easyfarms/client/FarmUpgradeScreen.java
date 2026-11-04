package com.awesomeshot5051.easyfarms.client;

import com.awesomeshot5051.easyfarms.gui.FarmUpgradeMenu;
import com.awesomeshot5051.easyfarms.tier.FarmUpgrade;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * The Upgrades sub-GUI, drawn on the vanilla chest texture. Hovering an empty slot names the card it takes and what the
 * recipe needs for it to work.
 */
public class FarmUpgradeScreen extends AbstractContainerScreen<FarmUpgradeMenu> {
    private static final ResourceLocation CHEST = ResourceLocation.withDefaultNamespace("textures/gui/container/generic_54.png");

    public FarmUpgradeScreen(FarmUpgradeMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 114 + menu.rows() * 18;
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
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int rows = menu.rows();
        g.blit(CHEST, leftPos, topPos, 0, 0, imageWidth, rows * 18 + 17);
        g.blit(CHEST, leftPos, topPos + rows * 18 + 17, 0, 126, imageWidth, 96);
    }

    /**
     * One tooltip at a time. Holding a card over a slot that would refuse it explains why (instead of the slot's own
     * tooltip, so the two never overlap); an empty slot names what it takes; anything else is the normal item tooltip.
     */
    @Override
    protected void renderTooltip(GuiGraphics g, int mouseX, int mouseY) {
        ItemStack carried = menu.getCarried();
        for (int i = 0; i < menu.upgradeSlotCount(); i++) {
            Slot slot = menu.slots.get(i);
            if (!isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY)) continue;
            if (!carried.isEmpty()) {
                Component reason = menu.restrictionProblem(carried, i);
                if (reason != null) {
                    g.renderComponentTooltip(font, List.of(Component.translatable("gui.easy_farms.restriction.cannot_add").withStyle(ChatFormatting.RED),
                            reason.copy().withStyle(ChatFormatting.GRAY)), mouseX, mouseY);
                    return;
                }
            } else if (!slot.hasItem()) {
                g.renderComponentTooltip(font, List.of(Component.translatable("gui.easy_farms.upgrade_slot")), mouseX, mouseY);
                return;
            }
        }
        if (menu.hasPressSlot()) {
            Slot slot = menu.slots.get(menu.upgradeSlotCount());
            if (carried.isEmpty() && !slot.hasItem() && isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY)) {
                g.renderComponentTooltip(font, List.of(Component.translatable("gui.easy_farms.inscriber_press"),
                        Component.translatable("gui.easy_farms.inscriber_press.hint")), mouseX, mouseY);
                return;
            }
        }
        super.renderTooltip(g, mouseX, mouseY);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
    }
}
