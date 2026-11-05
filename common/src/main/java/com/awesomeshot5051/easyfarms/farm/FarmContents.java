package com.awesomeshot5051.easyfarms.farm;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Everything a farm holds, packed into the farm item when it is picked up with Silk Touch (like a shulker box):
 * input, tool, output and upgrade slots in that order, plus how long the inputs have been running and the dye colour.
 * {@code extra} carries the rest: real stack counts above 99, stored energy and the liquid tank.
 */
public record FarmContents(List<ItemStack> stacks, long operatingTicks, String color, CompoundTag extra) {
    public static final Codec<FarmContents> CODEC = RecordCodecBuilder.create(i -> i.group(
            ItemStack.OPTIONAL_CODEC.listOf().fieldOf("stacks").forGetter(FarmContents::stacks),
            Codec.LONG.optionalFieldOf("operating_ticks", 0L).forGetter(FarmContents::operatingTicks),
            Codec.STRING.optionalFieldOf("color", "white").forGetter(FarmContents::color),
            CompoundTag.CODEC.optionalFieldOf("extra", new CompoundTag()).forGetter(FarmContents::extra)
    ).apply(i, FarmContents::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, FarmContents> STREAM_CODEC = StreamCodec.composite(
            ItemStack.OPTIONAL_LIST_STREAM_CODEC, FarmContents::stacks,
            ByteBufCodecs.VAR_LONG, FarmContents::operatingTicks,
            ByteBufCodecs.STRING_UTF8, FarmContents::color,
            ByteBufCodecs.COMPOUND_TAG, FarmContents::extra,
            FarmContents::new);

    public int stackCount() {
        return (int) stacks.stream().filter(s -> !s.isEmpty()).count();
    }

    /** True if there is something to keep besides item stacks. */
    public boolean hasExtra() {
        return !extra.isEmpty();
    }
}
