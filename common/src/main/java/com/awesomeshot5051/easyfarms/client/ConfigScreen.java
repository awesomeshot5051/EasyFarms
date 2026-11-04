package com.awesomeshot5051.easyfarms.client;

import com.awesomeshot5051.easyfarms.Config;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * A small settings screen over {@link Config}, used by loaders that have no config screen of their own for it
 * (NeoForge's built-in one reads its own spec type). A value outside its range is shown in red and not saved.
 */
public class ConfigScreen extends Screen {
    private record Row(Config.Value<?> value, EditBox box) {
    }

    private final Screen parent;
    private final List<Row> rows = new ArrayList<>();

    public ConfigScreen(Screen parent) {
        super(Component.translatable("gui.easy_farms.config"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        rows.clear();
        int y = 50;
        for (Config.Value<?> value : Config.values()) {
            EditBox box = new EditBox(font, width / 2 + 10, y, 100, 20, Component.literal(value.name()));
            box.setValue(String.valueOf(value.get()));
            box.setResponder(text -> box.setTextColor(value.trySet(text) ? 0xE0E0E0 : 0xFF5555));
            addRenderableWidget(box);
            rows.add(new Row(value, box));
            y += 30;
        }
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> {
            Config.save();
            onClose();
        }).bounds(width / 2 - 105, height - 30, 100, 20).build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, b -> {
            Config.revertAll();
            onClose();
        }).bounds(width / 2 + 5, height - 30, 100, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 15, 0xFFFFFF);
        for (Row row : rows) {
            graphics.drawString(font, row.value().name(), width / 2 - 150, row.box().getY() + 6, 0xFFFFFF);
            if (row.box().isHovered()) {
                List<Component> tip = new ArrayList<>();
                for (String line : row.value().comment()) tip.add(Component.literal(line));
                tip.add(Component.literal("Range: " + row.value().min() + " ~ " + row.value().max()).withStyle(ChatFormatting.GRAY));
                graphics.renderComponentTooltip(font, tip, mouseX, mouseY);
            }
        }
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }
}
