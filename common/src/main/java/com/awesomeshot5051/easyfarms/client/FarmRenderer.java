package com.awesomeshot5051.easyfarms.client;

import com.awesomeshot5051.easyfarms.farm.FarmBlock;
import com.awesomeshot5051.easyfarms.farm.FarmBlockEntity;
import com.awesomeshot5051.easyfarms.recipe.FarmRecipe;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import com.awesomeshot5051.easyfarms.platform.Services;

import java.util.HashMap;
import java.util.Map;

/**
 * One renderer for every farm; what it draws comes from the active recipe's visual.type:
 * "block"  - the ore/soil block with a tool swinging at it (resource farms)
 * "plant"  - the plant growing during the last visual_ticks of the cycle (plant farms)
 * "mob"    - the mob shown at 0.3 scale during the last visual_ticks of the cycle (mob farms)
 */
public class FarmRenderer implements BlockEntityRenderer<FarmBlockEntity> {
    private final BlockEntityRendererProvider.Context context;
    private final Map<EntityType<?>, Entity> entityCache = new HashMap<>();

    public FarmRenderer(BlockEntityRendererProvider.Context context) {
        this.context = context;
    }

    @Override
    public void render(FarmBlockEntity farm, float partialTick, PoseStack pose, MultiBufferSource buffer, int light, int overlay) {
        if (farm.kind() == com.awesomeshot5051.easyfarms.farm.FarmKind.LIQUID) {
            renderTank(farm, pose, buffer, light, overlay);
            return;
        }
        FarmRecipe.Visual visual = farm.getVisual();
        if (visual == null) return; // no matching recipe: just the empty glass case
        BlockState farmState = farm.getBlockState();
        Direction facing = farmState.hasProperty(FarmBlock.FACING) ? farmState.getValue(FarmBlock.FACING) : Direction.NORTH;
        long timer = farm.getTimer();
        switch (visual.type()) {
            case "block" -> renderMined(farm, visual, facing, timer, pose, buffer, light, overlay);
            case "plant" -> {
                if (timer >= farm.visualStart()) renderPlant(visual, facing, pose, buffer, light, overlay);
            }
            case "mob" -> {
                if (timer >= farm.visualStart()) renderMob(farm, visual, pose, buffer, light);
            }
            default -> {
            }
        }
    }

    /** The Liquid Farm: the fluid's still texture as a surface that rises with the tank level. */
    private void renderTank(FarmBlockEntity farm, PoseStack pose, MultiBufferSource buffer, int light, int overlay) {
        var id = farm.fluidId();
        if (id == null || farm.fluidAmount() <= 0) return;
        var fluid = BuiltInRegistries.FLUID.get(id);
        if (fluid == net.minecraft.world.level.material.Fluids.EMPTY) return;
        var sprite = Minecraft.getInstance().getBlockRenderer().getBlockModelShaper().getParticleIcon(fluid.defaultFluidState().createLegacyBlock());
        float level = Math.min(1f, farm.fluidAmount() / (float) Math.max(1, farm.fluidCapacity()));
        float y = 1f / 16f + level * (13f / 16f);
        float x0 = 1f / 16f, x1 = 15f / 16f;
        int tint = FluidTint.of(fluid);
        int r = tint >> 16 & 255, g = tint >> 8 & 255, b = tint & 255;
        int lit = fluid.defaultFluidState().createLegacyBlock().getLightEmission() > 0 ? 15728880 : light;
        var vc = buffer.getBuffer(RenderType.translucent());
        var last = pose.last();
        float[][] corners = {{x0, x0}, {x0, x1}, {x1, x1}, {x1, x0}};
        // Both windings, so the surface is visible from above and below whatever the culling does.
        for (int pass = 0; pass < 2; pass++) {
            for (int i = 0; i < 4; i++) {
                float[] c = corners[pass == 0 ? i : 3 - i];
                vc.addVertex(last, c[0], y, c[1]).setColor(r, g, b, 220)
                        .setUv(sprite.getU(c[0]), sprite.getV(c[1])).setOverlay(overlay).setLight(lit)
                        .setNormal(last, 0f, pass == 0 ? 1f : -1f, 0f);
            }
        }
    }

    private void renderMined(FarmBlockEntity farm, FarmRecipe.Visual visual, Direction facing, long timer,
                             PoseStack pose, MultiBufferSource buffer, int light, int overlay) {
        Block target = BuiltInRegistries.BLOCK.get(visual.target());
        pose.pushPose();
        pose.scale(0.5f, 0.5f, 0.5f);
        pose.translate(0.5, 0, 0.5);
        Services.client().renderSingleBlock(context.getBlockRenderDispatcher(), target.defaultBlockState(), pose, buffer, light, overlay, RenderType.solid());
        pose.popPose();

        // The tool in the tool slot is the one that swings.
        if (!farm.isPaused() && !farm.getTool().isEmpty()) {
            ToolSwing.render(farm.getLevel(), farm.getBlockPos(), pose, buffer, light, overlay, farm.getTool(), facing, timer);
        }
    }

    private void renderPlant(FarmRecipe.Visual visual, Direction facing, PoseStack pose, MultiBufferSource buffer, int light, int overlay) {
        Block plant = BuiltInRegistries.BLOCK.get(visual.target());
        BlockState state = plant.defaultBlockState();
        // Fully grown crop for anything with an "age" property (wheat, carrots, ...).
        for (var property : state.getProperties()) {
            if (property instanceof IntegerProperty age && age.getName().equals("age")) {
                state = state.setValue(age, age.getPossibleValues().stream().max(Integer::compare).orElse(0));
            }
        }
        pose.pushPose();
        // Same placement as the old SaplingRendererUtil.
        switch (facing) {
            case NORTH -> pose.translate(1D, 0D, 1D);
            case WEST -> pose.translate(1D, 0D, 0D);
            case EAST -> pose.translate(0D, 0D, 1.1D);
            default -> {
            }
        }
        pose.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));
        pose.scale(0.7F, 0.7F, 0.7F);
        Services.client().renderSingleBlock(context.getBlockRenderDispatcher(), state, pose, buffer, light, overlay, null);
        pose.popPose();
    }

    private void renderMob(FarmBlockEntity farm, FarmRecipe.Visual visual, PoseStack pose, MultiBufferSource buffer, int light) {
        if (Minecraft.getInstance().level == null) return;
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(visual.target());
        Entity entity = entityCache.computeIfAbsent(type, t -> t.create(Minecraft.getInstance().level));
        if (entity == null) return;
        if (entity instanceof net.minecraft.world.entity.animal.Sheep sheep) sheep.setColor(farm.getColor());
        EntityRenderDispatcher dispatcher = context.getEntityRenderer();
        pose.pushPose();
        pose.translate(0.5D, 1D / 16D, 0.5D);
        pose.mulPose(Axis.YP.rotationDegrees(-Direction.SOUTH.toYRot()));
        pose.translate(0D, 0D, 3D / 16D);
        pose.scale(0.3F, 0.3F, 0.3F);
        dispatcher.setRenderShadow(false);
        dispatcher.render(entity, 0, 0, 0, 0F, 0F, pose, buffer, light);
        dispatcher.setRenderShadow(true);
        pose.popPose();
    }
}
