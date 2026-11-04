package com.awesomeshot5051.easyfarms.platform;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class FabricClientPlatform implements IClientPlatform {
    @Override
    public void renderSingleBlock(BlockRenderDispatcher dispatcher, BlockState state, PoseStack pose, MultiBufferSource buffer,
                                  int light, int overlay, @Nullable RenderType renderType) {
        // Fabric picks the render layer from the block's registered layer (see EasyFarmsFabricClient).
        dispatcher.renderSingleBlock(state, pose, buffer, light, overlay);
    }
}
