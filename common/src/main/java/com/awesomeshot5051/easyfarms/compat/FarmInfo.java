package com.awesomeshot5051.easyfarms.compat;

import com.awesomeshot5051.easyfarms.farm.FarmBlockEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** What Jade and The One Probe show for a farm. Computed on the server (the client has no timers), shown by both. */
public final class FarmInfo {
    private FarmInfo() {
    }

    public record Snapshot(boolean running, boolean environmentOk, int progressPercent, String toolName, long daysLeft, boolean infinite,
                          long energy, long energyCapacity, String fluid, int fluidAmount, int fluidCapacity) {
        /** Fill (0 to 1) of the ME bar, or -1 when the farm has no Powered card. */
        public float energyFill() {
            return energyCapacity <= 0 ? -1 : Math.min(1f, energy / (float) energyCapacity);
        }

        public Component energyText() {
            return Component.translatable("gui.easy_farms.energy_short", energy, energyCapacity);
        }

        /** True for a Liquid Farm with a fluid in its tank (or an empty tank to show). */
        public boolean hasTank() {
            return fluidCapacity > 0;
        }

        public float fluidFill() {
            return fluidCapacity <= 0 ? 0 : Math.min(1f, fluidAmount / (float) fluidCapacity);
        }

        public Component fluidText() {
            return Component.translatable("gui.easy_farms.fluid_tank", fluidAmount, fluidCapacity);
        }

        public List<Component> lines() {
            List<Component> lines = new ArrayList<>();
            if (!running) {
                lines.add(Component.translatable("gui.easy_farms.idle"));
                return lines;
            }
            if (!environmentOk) lines.add(Component.translatable("gui.easy_farms.wrong_environment"));
            lines.add(Component.translatable("gui.easy_farms.progress", progressPercent));
            if (!toolName.isEmpty()) lines.add(Component.translatable("tooltip.easy_farms.tool", toolName));
            lines.add(infinite ? Component.translatable(toolName.isEmpty() ? "gui.easy_farms.inputs_infinite" : "gui.easy_farms.tool_infinite")
                    : Component.translatable(toolName.isEmpty() ? "gui.easy_farms.inputs_days" : "gui.easy_farms.tool_days", daysLeft));
            return lines;
        }
    }

    public static Snapshot of(FarmBlockEntity farm) {
        double cycle = farm.cycleTicks();
        ItemStack tool = farm.getTool();
        return new Snapshot(farm.isRunning(), farm.environmentOk(), cycle <= 0 ? 0 : (int) Math.min(100, 100 * farm.getTimer() / cycle),
                tool.isEmpty() || !farm.usesTool() ? "" : tool.getHoverName().getString(),
                farm.lifetimeLeftTicks() / 24000L,
                farm.hasInfiniteInputs(),
                farm.energyStored(), farm.energyCapacity(),
                farm.fluidId() == null ? "" : farm.fluidId().toString(), farm.fluidAmount(),
                farm.kind() == com.awesomeshot5051.easyfarms.farm.FarmKind.LIQUID ? farm.fluidCapacity() : 0);
    }
}
