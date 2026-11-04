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
                    data.getLongOr("DaysLeft", 0L), data.getBooleanOr("Infinite", false));
            for (Component line : s.lines()) tooltip.add(line);
        }

        @Override
        public Identifier getUid() {
            return UID;
        }
    }
}
