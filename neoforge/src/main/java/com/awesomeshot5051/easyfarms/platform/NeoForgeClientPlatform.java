package com.awesomeshot5051.easyfarms.platform;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;

public class NeoForgeClientPlatform implements IClientPlatform {
    @Override
    public void renderSingleBlock(BlockRenderDispatcher dispatcher, BlockState state, PoseStack pose, MultiBufferSource buffer,
                                  int light, int overlay, @Nullable RenderType renderType) {
        dispatcher.renderSingleBlock(state, pose, buffer, light, overlay, ModelData.EMPTY, renderType);
    }
}
