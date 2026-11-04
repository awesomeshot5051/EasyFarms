package com.awesomeshot5051.easyfarms.client;

import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.core.Direction;
import org.jspecify.annotations.Nullable;

/** What the farm renderer needs for one frame; filled on extract so the submit step touches no game state. */
public class FarmRenderState extends BlockEntityRenderState {
    /** "block", "plant" or "mob"; null = draw nothing. */
    public @Nullable String type;
    public Direction facing = Direction.NORTH;
    public long timer;
    /** The ore / soil block (resource farm) or the plant (plant farm). */
    public @Nullable MovingBlockRenderState block;
    public final ItemStackRenderState tool = new ItemStackRenderState();
    public boolean shovel;
    public @Nullable EntityRenderState mob;
}
