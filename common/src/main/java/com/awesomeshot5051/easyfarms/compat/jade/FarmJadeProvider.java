package com.awesomeshot5051.easyfarms.compat.jade;

import com.awesomeshot5051.easyfarms.EasyFarms;
import com.awesomeshot5051.easyfarms.compat.FarmInfo;
import com.awesomeshot5051.easyfarms.farm.FarmBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum FarmJadeProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;

    private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(EasyFarms.MODID, "farm");

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
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        if (!data.contains("Running")) return;
        FarmInfo.Snapshot s = new FarmInfo.Snapshot(data.getBoolean("Running"), data.getBoolean("EnvOk"), data.getInt("Progress"), data.getString("Tool"),
                data.getLong("DaysLeft"), data.getBoolean("Infinite"),
                data.getLong("Energy"), data.getLong("EnergyCap"), data.getString("Fluid"), data.getInt("FluidAmount"), data.getInt("FluidCap"));
        for (Component line : s.lines()) tooltip.add(line);
        var helper = snownee.jade.api.ui.IElementHelper.get();
        // Bars sit below all the other lines: the tank first, the ME bar last. Same style: coloured fill, value in the middle.
        if (s.hasTank()) {
            tooltip.add(helper.progress(s.fluidFill(), s.fluidText(), helper.progressStyle().color(fluidColor(s.fluid())).textColor(0xFFFFFFFF),
                    snownee.jade.api.ui.BoxStyle.getNestedBox(), true));
        }
        if (s.energyFill() >= 0) {
            tooltip.add(helper.progress(s.energyFill(), s.energyText(), helper.progressStyle().color(0xFFD02828).textColor(0xFFFFFFFF),
                    snownee.jade.api.ui.BoxStyle.getNestedBox(), true));
        }
    }

    /** Water blue, lava orange, anything else the colour of its block on a map. */
    static int fluidColor(String id) {
        if (id.equals("minecraft:water")) return 0xFF3F76E4;
        if (id.equals("minecraft:lava") || id.isEmpty()) return 0xFFE8761A;
        var fluid = net.minecraft.core.registries.BuiltInRegistries.FLUID.get(ResourceLocation.parse(id));
        return 0xFF000000 | fluid.defaultFluidState().createLegacyBlock().getBlock().defaultMapColor().col;
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }
}
