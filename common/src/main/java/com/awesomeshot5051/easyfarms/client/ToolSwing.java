package com.awesomeshot5051.easyfarms.client;

import com.awesomeshot5051.easyfarms.registry.ModSounds;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;

import static com.mojang.math.Axis.YP;
import static com.mojang.math.Axis.ZP;

/** The floating pickaxe/shovel that mines the block. Ported from the old PickaxeRendererUtil / ShovelRendererUtil. */
final class ToolSwing {
    /** Which swings already played their sound, so each swing makes one sound. */
    private static final Map<BlockPos, Boolean> SOUND_PLAYED = new HashMap<>();

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

    /** One sound per swing, at the top of the swing. */
    static void tickSound(Level level, BlockPos pos, Direction direction, long timer, boolean shovel) {
        float swingProgress = (timer % 20) / 20.0f;
        float angle = angle(timer, shovel);
        boolean peak = Math.abs(angle) >= (shovel ? 29.5f : 44.5f) && angle > 0;
        if (peak && !SOUND_PLAYED.getOrDefault(pos, false)) {
            SOUND_PLAYED.put(pos, true);
            play(level, pos, direction, shovel ? ModSounds.SHOVEL_SOUND.get() : ModSounds.PICKAXE_SOUND.get());
        } else if (swingProgress < 0.5) {
            SOUND_PLAYED.put(pos, false);
        }
    }

    private static void play(Level level, BlockPos pos, Direction direction, SoundEvent sound) {
        double x = pos.getX() + 0.5 + direction.getStepX() / 2.0;
        double y = pos.getY() + 0.5 + direction.getStepY() / 2.0;
        double z = pos.getZ() + 0.5 + direction.getStepZ() / 2.0;
        level.playLocalSound(x, y, z, sound, SoundSource.BLOCKS, 0.5F, level.getRandom().nextFloat() * 0.1F + 0.9F, true);
    }
}
