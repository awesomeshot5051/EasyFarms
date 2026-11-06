package com.awesomeshot5051.easyfarms;

import com.awesomeshot5051.easyfarms.platform.Services;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Global tuning, kept in {@code config/easy_farms-common.toml} (the same file and keys on every loader).
 * Tool tiers (speed, input lifetime multiplier) are data, see farm_tier.
 * <p>
 * One small hand-written TOML reader/writer shared by every loader, so the file and its keys are identical everywhere.
 */
public final class Config {
    public static final String FILE_NAME = EasyFarms.MODID + "-common.toml";

    /** One setting: a name, a comment, a default and a valid range. */
    public static final class Value<T extends Comparable<T>> {
        private final String name;
        private final String[] comment;
        private final T defaultValue, min, max;
        private final Function<String, T> parser;
        private T current, saved;

        private Value(String name, T defaultValue, T min, T max, Function<String, T> parser, String... comment) {
            this.name = name;
            this.comment = comment;
            this.defaultValue = defaultValue;
            this.min = min;
            this.max = max;
            this.parser = parser;
            this.current = this.saved = defaultValue;
            ALL.add(this);
        }

        public T get() {
            return current;
        }

        public String name() {
            return name;
        }

        public String[] comment() {
            return comment;
        }

        public T defaultValue() {
            return defaultValue;
        }

        public T min() {
            return min;
        }

        public T max() {
            return max;
        }

        public boolean isValid(T value) {
            return value != null && value.compareTo(min) >= 0 && value.compareTo(max) <= 0;
        }

        /** Parses and sets a value typed by the player. Returns false (and changes nothing) if it is malformed or out of range. */
        public boolean trySet(String text) {
            try {
                T value = parser.apply(text.trim());
                if (!isValid(value)) return false;
                current = value;
                return true;
            } catch (RuntimeException e) {
                return false;
            }
        }

        /** Sets the in-memory value (not saved until {@link Config#save()}). Invalid values are ignored. */
        public void set(T value) {
            if (isValid(value)) current = value;
        }

        public boolean isChanged() {
            return !current.equals(saved);
        }

        void markSaved() {
            saved = current;
        }

        void revert() {
            current = saved;
        }

        void revertToDefault() {
            current = saved = defaultValue;
        }

        private void parse(String text) {
            try {
                T value = parser.apply(text.trim());
                current = saved = isValid(value) ? value : defaultValue;
            } catch (RuntimeException e) {
                current = saved = defaultValue;
            }
        }
    }

    private static final List<Value<?>> ALL = new ArrayList<>();

    public static final Value<Double> TIME_MULTIPLIER = new Value<>("timeMultiplier", 1.0, 0.01, 1000.0, Double::parseDouble,
            "Multiplies every farm's cycle time (2.0 = twice as slow)");

    public static final Value<Integer> BASE_INPUT_LIFETIME_DAYS = new Value<>("baseInputLifetimeDays", 30, 1, 100000, Integer::parseInt,
            "Minecraft days of operation before a farm uses up its input items, at the base tier.",
            "Each tool tier multiplies this (see the farm_tier data). The Infinity upgrade skips it.");

    public static final Value<Integer> LIQUID_BASE_CAPACITY = new Value<>("liquidBaseCapacityMb", 16000, 1, 1000000, Integer::parseInt,
            "Millibuckets (mB) a Liquid Farm's tank holds without Capacity upgrades (1000 mB = one bucket; default 16000 = 16 buckets).",
            "Each Capacity upgrade multiplies it (see the capacity card's farm_upgrade data).");

    private Config() {
    }

    public static List<Value<?>> values() {
        return List.copyOf(ALL);
    }

    public static Path path() {
        return Services.PLATFORM.configDir().resolve(FILE_NAME);
    }

    /** Reads the file (missing or broken entries fall back to their default) and rewrites it so every key is present. */
    public static void load() {
        Path path = path();
        Map<String, String> raw = new HashMap<>();
        if (Files.exists(path)) {
            try {
                for (String line : Files.readAllLines(path)) {
                    String trimmed = line.trim();
                    if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("[")) continue;
                    int eq = trimmed.indexOf('=');
                    if (eq > 0) raw.put(trimmed.substring(0, eq).trim(), trimmed.substring(eq + 1).trim());
                }
            } catch (IOException e) {
                EasyFarms.LOGGER.warn("Could not read {}; using defaults", path, e);
            }
        }
        for (Value<?> value : ALL) {
            String text = raw.get(value.name);
            if (text != null) value.parse(text);
            else value.revertToDefault();
        }
        save();
    }

    public static void save() {
        StringBuilder out = new StringBuilder();
        for (Value<?> value : ALL) {
            for (String line : value.comment) out.append('#').append(line).append('\n');
            out.append("#Range: ").append(value.min).append(" ~ ").append(value.max).append('\n');
            out.append(value.name).append(" = ").append(value.current).append("\n\n");
            value.markSaved();
        }
        try {
            Files.createDirectories(path().getParent());
            Files.writeString(path(), out.toString());
        } catch (IOException e) {
            EasyFarms.LOGGER.warn("Could not write {}", path(), e);
        }
    }

    /** Drops unsaved edits. */
    public static void revertAll() {
        for (Value<?> value : ALL) value.revert();
    }
}
