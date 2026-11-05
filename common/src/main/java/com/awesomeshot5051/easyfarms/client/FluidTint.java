package com.awesomeshot5051.easyfarms.client;

import net.minecraft.network.chat.Component;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/** Colours and names for fluids on the client, without any loader API: water is tinted blue, everything else uses its own texture colours. */
public final class FluidTint {
    private FluidTint() {
    }

    public static final int WATER = 0x3F76E4;

    /** RGB tint to multiply the fluid's still texture with. */
    public static int of(Fluid fluid) {
        return fluid == Fluids.WATER || fluid == Fluids.FLOWING_WATER ? WATER : 0xFFFFFF;
    }

    public static Component name(Fluid fluid) {
        return fluid.defaultFluidState().createLegacyBlock().getBlock().getName();
    }
}
