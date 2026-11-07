package com.awesomeshot5051.easyfarms.farm.network;

import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/** The storage networks an Auto Importer card can import into, by the {@code network} id of its {@code network_import} effect. */
public final class NetworkImporters {
    private static final Map<String, NetworkImporter> IMPORTERS = new HashMap<>();

    private NetworkImporters() {
    }

    /** Called by a loader module once the other mod is known to be loaded. */
    public static void register(String id, NetworkImporter importer) {
        IMPORTERS.put(id, importer);
    }

    @Nullable
    public static NetworkImporter get(String id) {
        return IMPORTERS.get(id);
    }
}
