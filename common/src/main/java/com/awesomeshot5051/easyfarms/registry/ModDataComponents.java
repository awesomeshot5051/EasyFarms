package com.awesomeshot5051.easyfarms.registry;

import com.awesomeshot5051.easyfarms.farm.FarmContents;
import com.awesomeshot5051.easyfarms.platform.Services;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;

import java.util.function.Supplier;

public class ModDataComponents {
    /** Which data-driven upgrade a generic easy_farms:upgrade_card is (matches the "card" field of a farm_upgrade). */
    public static final Supplier<DataComponentType<Identifier>> CARD = Services.PLATFORM.register(Registries.DATA_COMPONENT_TYPE, "card",
            () -> DataComponentType.<Identifier>builder().persistent(Identifier.CODEC).networkSynchronized(Identifier.STREAM_CODEC).build());

    /** Where an Auto Importer card was linked to: any block of the storage network (a cable, an interface), in any dimension. */
    public static final Supplier<DataComponentType<net.minecraft.core.GlobalPos>> LINK = Services.PLATFORM.register(Registries.DATA_COMPONENT_TYPE, "link",
            () -> DataComponentType.<net.minecraft.core.GlobalPos>builder().persistent(net.minecraft.core.GlobalPos.CODEC)
                    .networkSynchronized(net.minecraft.core.GlobalPos.STREAM_CODEC).build());

    /** The DimStorage frequency set on a DimStorage Auto Importer card (set in the card's own screen). */
    public static final Supplier<DataComponentType<CardFrequency>> FREQUENCY = Services.PLATFORM.register(Registries.DATA_COMPONENT_TYPE, "frequency",
            () -> DataComponentType.<CardFrequency>builder().persistent(CardFrequency.CODEC).networkSynchronized(CardFrequency.STREAM_CODEC).build());

    /** What a farm item carries after being broken with Silk Touch. */
    public static final Supplier<DataComponentType<FarmContents>> FARM_CONTENTS = Services.PLATFORM.register(Registries.DATA_COMPONENT_TYPE, "farm_contents",
            () -> DataComponentType.<FarmContents>builder().persistent(FarmContents.CODEC).networkSynchronized(FarmContents.STREAM_CODEC).build());

    public static void init() {
        // class load registers the components
    }
}
