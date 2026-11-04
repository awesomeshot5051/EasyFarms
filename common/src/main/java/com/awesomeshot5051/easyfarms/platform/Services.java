package com.awesomeshot5051.easyfarms.platform;

import java.util.ServiceLoader;

/** Looks up the loader-specific implementations. */
public final class Services {
    public static final IPlatform PLATFORM = load(IPlatform.class);

    private Services() {
    }

    /** Client hooks are loaded on demand so a dedicated server never touches them. */
    public static IClientPlatform client() {
        return ClientHolder.INSTANCE;
    }

    private static final class ClientHolder {
        static final IClientPlatform INSTANCE = load(IClientPlatform.class);
    }

    private static <T> T load(Class<T> type) {
        return ServiceLoader.load(type, Services.class.getClassLoader()).findFirst()
                .orElseThrow(() -> new IllegalStateException("No " + type.getSimpleName() + " implementation found"));
    }
}
