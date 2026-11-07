package com.awesomeshot5051.easyfarms.registry;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.Optional;
import java.util.UUID;

/**
 * A DimStorage frequency set on an Auto Importer card: the channel number and, for a private frequency, the player who owns it
 * (DimStorage keys storage by owner + channel). Kept as plain data here, so the main mod never touches DimStorage's own classes.
 */
public record CardFrequency(int channel, Optional<UUID> owner, String ownerName) {
    public static final int MAX_CHANNEL = 99999;
    public static final CardFrequency PUBLIC_ZERO = new CardFrequency(0, Optional.empty(), "");

    public static final Codec<CardFrequency> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.intRange(0, MAX_CHANNEL).fieldOf("channel").forGetter(CardFrequency::channel),
            UUIDUtil.CODEC.optionalFieldOf("owner").forGetter(CardFrequency::owner),
            Codec.STRING.optionalFieldOf("owner_name", "").forGetter(CardFrequency::ownerName)
    ).apply(i, CardFrequency::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, CardFrequency> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, CardFrequency::channel,
            ByteBufCodecs.optional(UUIDUtil.STREAM_CODEC), CardFrequency::owner,
            ByteBufCodecs.STRING_UTF8, CardFrequency::ownerName,
            CardFrequency::new);

    public boolean isPrivate() {
        return owner.isPresent();
    }

    public CardFrequency withChannel(int channel) {
        return new CardFrequency(Math.max(0, Math.min(MAX_CHANNEL, channel)), owner, ownerName);
    }

    public CardFrequency withOwner(Optional<UUID> owner, String ownerName) {
        return new CardFrequency(channel, owner, owner.isPresent() ? ownerName : "");
    }
}
