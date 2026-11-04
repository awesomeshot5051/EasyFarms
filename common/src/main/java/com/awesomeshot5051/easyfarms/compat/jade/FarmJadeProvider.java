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
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        if (!data.contains("Running")) return;
        FarmInfo.Snapshot s = new FarmInfo.Snapshot(data.getBoolean("Running"), data.getBoolean("EnvOk"), data.getInt("Progress"), data.getString("Tool"),
                data.getLong("DaysLeft"), data.getBoolean("Infinite"));
        for (Component line : s.lines()) tooltip.add(line);
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }
}
