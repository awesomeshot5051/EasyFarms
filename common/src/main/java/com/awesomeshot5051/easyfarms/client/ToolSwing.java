package com.awesomeshot5051.easyfarms.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import static com.mojang.math.Axis.YP;
import static com.mojang.math.Axis.ZP;

/** The floating pickaxe/shovel that mines the block. Ported from the old PickaxeRendererUtil / ShovelRendererUtil. */
final class ToolSwing {
    private ToolSwing() {
    }

    static void render(Level level, PoseStack pose, MultiBufferSource buffer, int light, int overlay,
                       ItemStack tool, Direction direction, long timer) {
        boolean shovel = tool.getItem() instanceof net.minecraft.world.item.ShovelItem;
        pose.pushPose();
        pose.translate(0.2, 0.55, 0.5);

        float swingProgress = (timer % 20) / 20.0f;
        float angle = (float) Math.sin(swingProgress * Math.PI * 2) * (shovel ? 30 : 45);
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
        pose.mulPose(ZP.rotationDegrees(angle));

        pose.scale(0.5f, 0.5f, 0.5f);
        Minecraft.getInstance().getItemRenderer().renderStatic(tool, ItemDisplayContext.GROUND, light, overlay, pose, buffer, level, 0);
        pose.popPose();
    }
}
