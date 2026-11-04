package com.awesomeshot5051.easyfarms.trigger;

import com.awesomeshot5051.easyfarms.platform.Services;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;

public final class ModTriggers {
    private ModTriggers() {
    }

    public static final Supplier<SecretRecipeTrigger> SECRET_RECIPE = Services.PLATFORM.register(Registries.TRIGGER_TYPE, "secret_recipe", SecretRecipeTrigger::new);

    public static void init() {
        // class load registers the trigger
    }
}
