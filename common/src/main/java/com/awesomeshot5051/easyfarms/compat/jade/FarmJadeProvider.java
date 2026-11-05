package com.awesomeshot5051.easyfarms.compat.jade;

import com.awesomeshot5051.easyfarms.EasyFarms;
import com.awesomeshot5051.easyfarms.compat.FarmInfo;
import com.awesomeshot5051.easyfarms.farm.FarmBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public final class FarmJadeProvider {
    private FarmJadeProvider() {
    }

    static final Identifier UID = Identifier.fromNamespaceAndPath(EasyFarms.MODID, "farm");

    /** Server side: packs the farm's state into the data Jade sends to the client. */
    public enum Server implements IServerDataProvider<BlockAccessor> {
        INSTANCE;

        @Override
        public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            if (!(accessor.getBlockEntity() instanceof FarmBlockEntity farm)) return;
            FarmInfo.Snapshot s = FarmInfo.of(farm);
            data.putBoolean("Running", s.running());
            data.putBoolean("EnvOk", s.environmentOk());
            data.putInt("Progress", s.progressPercent());
            data.putString("Tool", s.toolName());
            data.putLong("DaysLeft", s.daysLeft());
            data.putBoolean("Infinite", s.infinite());
            data.putLong("Energy", s.energy());
            data.putLong("EnergyCap", s.energyCapacity());
            data.putString("Fluid", s.fluid());
            data.putInt("FluidAmount", s.fluidAmount());
            data.putInt("FluidCap", s.fluidCapacity());
        }

        @Override
        public Identifier getUid() {
            return UID;
        }
    }

    /** Client side: draws the tooltip lines. */
    public enum Client implements IBlockComponentProvider {
        INSTANCE;

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            CompoundTag data = accessor.getServerData();
            if (data.getBoolean("Running").isEmpty()) return;
            FarmInfo.Snapshot s = new FarmInfo.Snapshot(data.getBooleanOr("Running", false), data.getBooleanOr("EnvOk", false), data.getIntOr("Progress", 0), data.getStringOr("Tool", ""),
                    data.getLongOr("DaysLeft", 0L), data.getBooleanOr("Infinite", false),
                    data.getLongOr("Energy", 0L), data.getLongOr("EnergyCap", 0L), data.getStringOr("Fluid", ""),
                    data.getIntOr("FluidAmount", 0), data.getIntOr("FluidCap", 0));
            for (Component line : s.lines()) tooltip.add(line);
            // Bars sit below all the other lines: the tank first, the ME bar last. Same style: coloured fill, value in the middle.
            if (s.hasTank()) bar(tooltip, s.fluidFill(), s.fluidText(), fluidColor(s.fluid()));
            if (s.energyFill() >= 0) bar(tooltip, s.energyFill(), s.energyText(), 0xFFD02828);
        }

        private static void bar(ITooltip tooltip, float fill, Component text, int color) {
            var view = new snownee.jade.api.view.ProgressView(snownee.jade.api.view.ProgressView.Part.of(fill, color), text,
                    snownee.jade.api.ui.JadeUI.progressStyle(), snownee.jade.api.ui.BoxStyle.nestedBox());
            tooltip.add(snownee.jade.api.ui.JadeUI.progress(view));
        }

        /** Water blue, lava orange, anything else a neutral blue-grey. */
        static int fluidColor(String id) {
            if (id.equals("minecraft:water")) return 0xFF3F76E4;
            if (id.equals("minecraft:lava") || id.isEmpty()) return 0xFFE8761A;
            return 0xFF6A8CAF;
        }

        @Override
        public Identifier getUid() {
            return UID;
        }
    }
}
