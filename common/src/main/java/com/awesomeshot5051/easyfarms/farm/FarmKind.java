package com.awesomeshot5051.easyfarms.farm;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

/** The four machines. What each one does is decided by the machine recipes of its kind. */
public enum FarmKind implements StringRepresentable {
    RESOURCE, PLANT, MOB, LIQUID;

    public static final Codec<FarmKind> CODEC = StringRepresentable.fromEnum(FarmKind::values);

    @Override
    public String getSerializedName() {
        return name().toLowerCase();
    }

    /** Block / recipe-type name, e.g. "mob_farm". */
    public String id() {
        return getSerializedName() + "_farm";
    }
}
