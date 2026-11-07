package com.awesomeshot5051.easyfarms.client;

import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.core.Direction;
import org.jspecify.annotations.Nullable;

/** What the farm renderer needs for one frame; filled on extract so the submit step touches no game state. */
public class FarmRenderState extends BlockEntityRenderState {
    /** "block", "plant", "mob" or "goo"; null = draw nothing. */
    public @Nullable String type;
    public Direction facing = Direction.NORTH;
    public long timer;
    /** The ore / soil block (resource farm) or the plant (plant farm). */
    public @Nullable MovingBlockRenderState block;
    /** "goo" visual: the block beside the goo that is consumed, the block it becomes, and the 0..1 progress. */
    public @Nullable MovingBlockRenderState gooInput;
    public @Nullable MovingBlockRenderState gooResult;
    public float gooProgress;
    public final ItemStackRenderState tool = new ItemStackRenderState();
    public boolean shovel;
    public @Nullable EntityRenderState mob;
    /** Liquid Farm: the fluid surface (type "tank"). */
    public net.minecraft.client.renderer.texture.@Nullable TextureAtlasSprite fluidSprite;
    public float fluidLevel;
    public int fluidTint = 0xFFFFFFFF;
    public int fluidLight;
}
