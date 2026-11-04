package com.awesomeshot5051.easyfarms.platform;

import java.util.ServiceLoader;

/** Looks up the loader-specific implementations. */
public final class Services {
    public static final IPlatform PLATFORM = load(IPlatform.class);

    private Services() {
    }

    private static <T> T load(Class<T> type) {
        return ServiceLoader.load(type, Services.class.getClassLoader()).findFirst()
                .orElseThrow(() -> new IllegalStateException("No " + type.getSimpleName() + " implementation found"));
    }
}
