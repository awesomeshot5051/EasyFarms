package com.awesomeshot5051.easyfarms.client;

import com.awesomeshot5051.easyfarms.farm.FarmBlock;
import com.awesomeshot5051.easyfarms.platform.Services;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

/** Draws the empty glass case for a farm item (the item model is builtin/entity so this runs for every display context). */
public final class FarmItemRenderer {
    private FarmItemRenderer() {
    }

    public static void render(ItemStack stack, PoseStack pose, MultiBufferSource buffer, int light, int overlay) {
        if (!(stack.getItem() instanceof BlockItem blockItem) || !(blockItem.getBlock() instanceof FarmBlock block)) return;
        Minecraft mc = Minecraft.getInstance();
        Services.client().renderSingleBlock(mc.getBlockRenderer(), block.defaultBlockState(), pose, buffer, light, overlay, RenderType.cutout());
    }
}
