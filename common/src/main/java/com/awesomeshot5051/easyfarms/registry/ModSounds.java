package com.awesomeshot5051.easyfarms.registry;

import com.awesomeshot5051.easyfarms.EasyFarms;
import com.awesomeshot5051.easyfarms.platform.Services;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;

import java.util.function.Supplier;

public class ModSounds {
    public static final Supplier<SoundEvent> PICKAXE_SOUND = register("pickaxe_sound");
    public static final Supplier<SoundEvent> SHOVEL_SOUND = register("shovel_sound");

    private static Supplier<SoundEvent> register(String name) {
        return Services.PLATFORM.register(Registries.SOUND_EVENT, name, () -> SoundEvent.createVariableRangeEvent(EasyFarms.id(name)));
    }

    public static void init() {
        // class load registers the sounds
    }
}
