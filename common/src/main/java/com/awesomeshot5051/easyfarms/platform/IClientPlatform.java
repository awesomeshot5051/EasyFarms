package com.awesomeshot5051.easyfarms.platform;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

/** Client-only loader hooks. Only ever touched from client code. */
public interface IClientPlatform {
    /** Draws a block state outside the world. {@code renderType} may be null (all of the model's layers). */
    void renderSingleBlock(BlockRenderDispatcher dispatcher, BlockState state, PoseStack pose, MultiBufferSource buffer,
                           int light, int overlay, @Nullable RenderType renderType);
}
