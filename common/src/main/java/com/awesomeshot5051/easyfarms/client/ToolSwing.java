package com.awesomeshot5051.easyfarms.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.core.Direction;

import static com.mojang.math.Axis.YP;
import static com.mojang.math.Axis.ZP;

/** The floating pickaxe/shovel that mines the block. Ported from the old PickaxeRendererUtil / ShovelRendererUtil. */
final class ToolSwing {
    private ToolSwing() {
    }

    /** Positions the pose for the swinging tool; the item itself is submitted by the renderer afterwards. */
    static void transform(PoseStack pose, Direction direction, long timer, boolean shovel) {
        pose.translate(0.2, 0.55, 0.5);
        if (shovel) pose.mulPose(ZP.rotationDegrees(90));

        switch (direction) {
            case EAST -> {
                pose.mulPose(YP.rotationDegrees(90));
                pose.translate(shovel ? -0.2 : -0.3, shovel ? -0.6 : 0, shovel ? 0 : 0.3);
            }
            case WEST -> {
                pose.mulPose(YP.rotationDegrees(270));
                pose.translate(shovel ? -0.2 : -0.3, shovel ? -0.6 : 0, shovel ? 0 : -0.3);
            }
            case NORTH -> {
                pose.mulPose(YP.rotationDegrees(180));
                pose.translate(shovel ? -0.4 : -0.6, shovel ? -0.6 : 0, 0);
            }
            default -> {
                if (shovel) pose.translate(0, -0.6, 0);
            }
        }
        pose.mulPose(ZP.rotationDegrees(angle(timer, shovel)));
        pose.scale(0.5f, 0.5f, 0.5f);
    }

    private static float angle(long timer, boolean shovel) {
        float swingProgress = (timer % 20) / 20.0f;
        return (float) Math.sin(swingProgress * Math.PI * 2) * (shovel ? 30 : 45);
    }
}
