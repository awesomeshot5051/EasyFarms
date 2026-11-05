package com.awesomeshot5051.easyfarms.compat.top;

import com.awesomeshot5051.easyfarms.EasyFarms;
import com.awesomeshot5051.easyfarms.compat.FarmInfo;
import com.awesomeshot5051.easyfarms.farm.FarmBlockEntity;
import mcjty.theoneprobe.api.IProbeHitData;
import mcjty.theoneprobe.api.IProbeInfo;
import mcjty.theoneprobe.api.IProbeInfoProvider;
import mcjty.theoneprobe.api.ITheOneProbe;
import mcjty.theoneprobe.api.ProbeMode;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Function;

/** The One Probe: same lines as Jade. Registered through an IMC message (see EasyFarms), only when TOP is loaded. */
public class TopCompat implements Function<ITheOneProbe, Void> {
    @Override
    public Void apply(ITheOneProbe probe) {
        probe.registerProvider(new IProbeInfoProvider() {
            @Override
            public Identifier getID() {
                return Identifier.fromNamespaceAndPath(EasyFarms.MODID, "farm");
            }

            @Override
            public void addProbeInfo(ProbeMode mode, IProbeInfo info, Player player, Level level, BlockState state, IProbeHitData data) {
                if (level.getBlockEntity(data.getPos()) instanceof FarmBlockEntity farm) {
                    FarmInfo.Snapshot s = FarmInfo.of(farm);
                    for (Component line : s.lines()) info.mcText(line);
                    if (s.hasTank()) {
                        info.progress(s.fluidAmount(), s.fluidCapacity(), info.defaultProgressStyle().filledColor(0xFF3F76E4).alternateFilledColor(0xFF3F76E4)
                                .suffix(" mB").borderColor(0xFF373737));
                    }
                    if (s.energyFill() >= 0) {
                        info.progress((int) Math.min(Integer.MAX_VALUE, s.energy()), (int) Math.min(Integer.MAX_VALUE, s.energyCapacity()),
                                info.defaultProgressStyle().filledColor(0xFFD02828).alternateFilledColor(0xFFD02828).suffix(" ME").borderColor(0xFF373737));
                    }
                }
            }
        });
        return null;
    }
}
