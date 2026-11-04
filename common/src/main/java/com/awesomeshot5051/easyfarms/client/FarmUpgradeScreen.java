package com.awesomeshot5051.easyfarms.client;

import com.awesomeshot5051.easyfarms.gui.FarmUpgradeMenu;
import com.awesomeshot5051.easyfarms.tier.FarmUpgrade;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * The Upgrades sub-GUI, drawn on the vanilla chest texture. Hovering an empty slot names the card it takes and what the
 * recipe needs for it to work.
 */
public class FarmUpgradeScreen extends AbstractContainerScreen<FarmUpgradeMenu> {
    private static final Identifier CHEST = Identifier.withDefaultNamespace("textures/gui/container/generic_54.png");

    public FarmUpgradeScreen(FarmUpgradeMenu menu, Inventory inventory, Component title) {
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
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        for (int i = 0; i < menu.upgradeSlotCount(); i++) {
            Slot slot = menu.slots.get(i);
            if (!slot.hasItem() && isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY)) {
                g.setComponentTooltipForNextFrame(font, List.of(Component.translatable("gui.easy_farms.upgrade_slot")), mouseX, mouseY);
            }
        }
        if (menu.hasPressSlot()) {
            Slot slot = menu.slots.get(menu.upgradeSlotCount());
            if (!slot.hasItem() && isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY)) {
                g.setComponentTooltipForNextFrame(font, List.of(Component.translatable("gui.easy_farms.inscriber_press"),
                        Component.translatable("gui.easy_farms.inscriber_press.hint")), mouseX, mouseY);
            }
        }
    }
}
