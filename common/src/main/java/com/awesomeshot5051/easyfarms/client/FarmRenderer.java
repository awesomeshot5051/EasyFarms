package com.awesomeshot5051.easyfarms.client;

import com.awesomeshot5051.easyfarms.farm.FarmBlock;
import com.awesomeshot5051.easyfarms.farm.FarmBlockEntity;
import com.awesomeshot5051.easyfarms.recipe.FarmRecipe;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * One renderer for every farm; what it draws comes from the active recipe's visual.type:
 * "block"  - the ore/soil block with a tool swinging at it (resource farms)
 * "plant"  - the plant growing during the last visual_ticks of the cycle (plant farms)
 * "mob"    - the mob shown at 0.3 scale during the last visual_ticks of the cycle (mob farms)
 */
public class FarmRenderer implements BlockEntityRenderer<FarmBlockEntity, FarmRenderState> {
    private final BlockEntityRendererProvider.Context context;
    private final Map<EntityType<?>, Entity> entityCache = new HashMap<>();

    public FarmRenderer(BlockEntityRendererProvider.Context context) {
        this.context = context;
    }

    @Override
    public FarmRenderState createRenderState() {
        return new FarmRenderState();
    }

    @Override
    public void extractRenderState(FarmBlockEntity farm, FarmRenderState state, float partialTick, Vec3 cameraPos,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
        BlockEntityRenderer.super.extractRenderState(farm, state, partialTick, cameraPos, crumbling);
        state.type = null;
        state.block = null;
        state.mob = null;
        state.tool.clear();
        FarmRecipe.Visual visual = farm.getVisual();
        if (visual == null || !(farm.getLevel() instanceof ClientLevel level)) return; // no matching recipe: just the empty glass case
        BlockState farmState = farm.getBlockState();
        state.facing = farmState.hasProperty(FarmBlock.FACING) ? farmState.getValue(FarmBlock.FACING) : Direction.NORTH;
        state.timer = farm.getTimer();
        boolean visible = state.timer >= farm.visualStart();
        switch (visual.type()) {
            case "block" -> {
                state.type = "block";
                state.block = movingBlock(level, farm, BuiltInRegistries.BLOCK.getValue(visual.target()).defaultBlockState());
                // The tool in the tool slot is the one that swings.
                if (!farm.isPaused() && !farm.getTool().isEmpty()) {
                    state.shovel = farm.getTool().is(net.minecraft.tags.ItemTags.SHOVELS);
                    Minecraft.getInstance().getItemModelResolver().updateForTopItem(state.tool, farm.getTool(),
                            ItemDisplayContext.GROUND, level, null, 0);
                    ToolSwing.tickSound(level, farm.getBlockPos(), state.facing, state.timer, state.shovel);
                }
            }
            case "plant" -> {
                if (!visible) return;
                BlockState plant = BuiltInRegistries.BLOCK.getValue(visual.target()).defaultBlockState();
                // Fully grown crop for anything with an "age" property (wheat, carrots, ...).
                for (var property : plant.getProperties()) {
                    if (property instanceof IntegerProperty age && age.getName().equals("age")) {
                        plant = plant.setValue(age, age.getPossibleValues().stream().max(Integer::compare).orElse(0));
                    }
                }
                state.type = "plant";
                state.block = movingBlock(level, farm, plant);
            }
            case "mob" -> {
                if (!visible) return;
                EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(visual.target());
                Entity entity = entityCache.computeIfAbsent(type, t -> t.create(level, EntitySpawnReason.LOAD));
                if (entity == null) return;
                if (entity instanceof net.minecraft.world.entity.animal.sheep.Sheep sheep) sheep.setColor(farm.getColor());
                EntityRenderState mob = context.entityRenderer().extractEntity(entity, partialTick);
                mob.lightCoords = state.lightCoords;
                mob.shadowRadius = 0;
                mob.shadowPieces.clear();
                mob.nameTag = null;
                mob.displayFireAnimation = false;
                state.type = "mob";
                state.mob = mob;
            }
            default -> {
            }
        }
    }

    private static MovingBlockRenderState movingBlock(ClientLevel level, FarmBlockEntity farm, BlockState blockState) {
        MovingBlockRenderState block = new MovingBlockRenderState();
        block.randomSeedPos = farm.getBlockPos();
        block.blockPos = farm.getBlockPos();
        block.blockState = blockState;
        block.biome = level.getBiome(farm.getBlockPos());
        block.cardinalLighting = level.cardinalLighting();
        block.lightEngine = level.getLightEngine();
        return block;
    }

    @Override
    public void submit(FarmRenderState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.type == null) return;
        switch (state.type) {
            case "block" -> {
                if (state.block != null) {
                    pose.pushPose();
                    pose.scale(0.5f, 0.5f, 0.5f);
                    pose.translate(0.5, 0, 0.5);
                    collector.submitMovingBlock(pose, state.block);
                    pose.popPose();
                }
                if (!state.tool.isEmpty()) {
                    pose.pushPose();
                    ToolSwing.transform(pose, state.facing, state.timer, state.shovel);
                    state.tool.submit(pose, collector, state.lightCoords, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, 0);
                    pose.popPose();
                }
            }
            case "plant" -> {
                if (state.block == null) return;
                pose.pushPose();
                // Same placement as the old SaplingRendererUtil.
                switch (state.facing) {
                    case NORTH -> pose.translate(1D, 0D, 1D);
                    case WEST -> pose.translate(1D, 0D, 0D);
                    case EAST -> pose.translate(0D, 0D, 1.1D);
                    default -> {
                    }
                }
                pose.mulPose(Axis.YP.rotationDegrees(-state.facing.toYRot()));
                pose.scale(0.7F, 0.7F, 0.7F);
                collector.submitMovingBlock(pose, state.block);
                pose.popPose();
            }
            case "mob" -> {
                if (state.mob == null) return;
                pose.pushPose();
                pose.translate(0.5D, 1D / 16D, 0.5D);
                pose.mulPose(Axis.YP.rotationDegrees(-Direction.SOUTH.toYRot()));
                pose.translate(0D, 0D, 3D / 16D);
                pose.scale(0.3F, 0.3F, 0.3F);
                context.entityRenderer().submit(state.mob, camera, 0, 0, 0, pose, collector);
                pose.popPose();
            }
            default -> {
            }
        }
    }
}
