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

    private static com.awesomeshot5051.easyfarms.tier.FluidContainer virtual(net.minecraft.resources.Identifier id) {
        var level = net.minecraft.client.Minecraft.getInstance().level;
        return level == null ? null : com.awesomeshot5051.easyfarms.tier.FluidContainer.of(level.registryAccess(), id);
    }

    /** True if a fluid with this id can be drawn: a game fluid or a virtual one (honey, milk). */
    public static boolean known(net.minecraft.resources.Identifier id) {
        return net.minecraft.core.registries.BuiltInRegistries.FLUID.containsKey(id) || virtual(id) != null;
    }

    /** The texture to draw this fluid with: a virtual fluid's own texture, else the game fluid's still texture. */
    public static net.minecraft.client.renderer.texture.TextureAtlasSprite sprite(net.minecraft.resources.Identifier id) {
        var mc = net.minecraft.client.Minecraft.getInstance();
        var container = virtual(id);
        if (container != null && container.texture().isPresent()) {
            return mc.getAtlasManager().getAtlasOrThrow(net.minecraft.data.AtlasIds.BLOCKS).getSprite(container.texture().get());
        }
        Fluid fluid = net.minecraft.core.registries.BuiltInRegistries.FLUID.getValue(id);
        return mc.getModelManager().getFluidStateModelSet().get(fluid.defaultFluidState()).stillMaterial().sprite();
    }

    public static int tint(net.minecraft.resources.Identifier id) {
        if (!net.minecraft.core.registries.BuiltInRegistries.FLUID.containsKey(id)) {
            var container = virtual(id);
            return container == null ? 0xFFFFFF : container.tint();
        }
        return of(net.minecraft.core.registries.BuiltInRegistries.FLUID.getValue(id));
    }

    public static boolean glows(net.minecraft.resources.Identifier id) {
        return net.minecraft.core.registries.BuiltInRegistries.FLUID.containsKey(id)
                && net.minecraft.core.registries.BuiltInRegistries.FLUID.getValue(id).defaultFluidState().createLegacyBlock().getLightEmission() > 0;
    }

    public static Component name(net.minecraft.resources.Identifier id) {
        if (net.minecraft.core.registries.BuiltInRegistries.FLUID.containsKey(id)) return name(net.minecraft.core.registries.BuiltInRegistries.FLUID.getValue(id));
        return Component.translatable("fluid." + id.getNamespace() + "." + id.getPath());
    }

    public static Component name(Fluid fluid) {
        return fluid.defaultFluidState().createLegacyBlock().getBlock().getName();
    }
}
