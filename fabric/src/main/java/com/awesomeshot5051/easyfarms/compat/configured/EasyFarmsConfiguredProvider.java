package com.awesomeshot5051.easyfarms.compat.configured;

import com.awesomeshot5051.easyfarms.Config;
import com.awesomeshot5051.easyfarms.EasyFarms;
import com.mrcrayfish.configured.api.ActionResult;
import com.mrcrayfish.configured.api.ConfigType;
import com.mrcrayfish.configured.api.IConfigEntry;
import com.mrcrayfish.configured.api.IConfigValue;
import com.mrcrayfish.configured.api.IModConfig;
import com.mrcrayfish.configured.api.IModConfigProvider;
import com.mrcrayfish.configured.api.ModContext;
import com.mrcrayfish.configured.api.ValueEntry;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Gives the Configured mod (MrCrayfish) the Easy Farms settings, so they get an in-game screen from the mods list
 * (through Mod Menu) the way NeoForge's built-in configuration screen shows them. Discovered through
 * {@code custom.configured.providers} in fabric.mod.json; Configured is optional.
 */
public class EasyFarmsConfiguredProvider implements IModConfigProvider {
    @Override
    public Set<IModConfig> getConfigurationsForMod(ModContext context) {
        return EasyFarms.MODID.equals(context.modId()) ? Set.of(new FarmConfig()) : Set.of();
    }

    private static final class FarmConfig implements IModConfig {
        @Override
        public ConfigType getType() {
            return ConfigType.UNIVERSAL;
        }

        @Override
        public String getFileName() {
            return Config.FILE_NAME;
        }

        @Override
        public String getModId() {
            return EasyFarms.MODID;
        }

        @Override
        public IConfigEntry createRootEntry() {
            List<IConfigEntry> children = new ArrayList<>();
            for (Config.Value<?> value : Config.values()) children.add(new ValueEntry(adapt(value)));
            return new Root(children);
        }

        @Override
        public ActionResult update(IConfigEntry entry) {
            Config.save();
            return ActionResult.success();
        }

        @Override
        public boolean isChanged() {
            return Config.values().stream().anyMatch(Config.Value::isChanged);
        }

        @Override
        public void stopEditing(boolean save) {
            if (!save) Config.revertAll();
        }

        @Override
        public Optional<Runnable> restoreDefaultsTask() {
            return Optional.of(() -> {
                for (Config.Value<?> value : Config.values()) adapt(value).restore();
            });
        }
    }

    private static <T extends Comparable<T>> IConfigValue<T> adapt(Config.Value<T> value) {
        return new ValueAdapter<>(value);
    }

    private record Root(List<IConfigEntry> children) implements IConfigEntry {
        @Override
        public List<IConfigEntry> getChildren() {
            return children;
        }

        @Override
        public boolean isRoot() {
            return true;
        }

        @Override
        public boolean isLeaf() {
            return false;
        }

        @Override
        public IConfigValue<?> getValue() {
            return null;
        }

        @Override
        public String getEntryName() {
            return EasyFarms.MODID;
        }

        @Override
        public Component getTooltip() {
            return null;
        }

        @Override
        public String getTranslationKey() {
            return EasyFarms.MODID + ".configuration.title";
        }
    }

    private record ValueAdapter<T extends Comparable<T>>(Config.Value<T> value) implements IConfigValue<T> {
        @Override
        public T get() {
            return value.get();
        }

        @Override
        public T getDefault() {
            return value.defaultValue();
        }

        @Override
        public void set(T newValue) {
            value.set(newValue);
        }

        @Override
        public boolean isValid(T candidate) {
            return value.isValid(candidate);
        }

        @Override
        public boolean isDefault() {
            return value.get().equals(value.defaultValue());
        }

        @Override
        public boolean isChanged() {
            return value.isChanged();
        }

        @Override
        public void restore() {
            value.set(value.defaultValue());
        }

        @Override
        public Component getComment() {
            return Component.literal(String.join("\n", value.comment()));
        }

        @Override
        public String getTranslationKey() {
            return EasyFarms.MODID + ".configuration." + value.name();
        }

        @Override
        public Component getValidationHint() {
            return Component.literal("Range: " + value.min() + " ~ " + value.max());
        }

        @Override
        public String getName() {
            return value.name();
        }

        @Override
        public void cleanCache() {
        }

        @Override
        public boolean requiresWorldRestart() {
            return false;
        }

        @Override
        public boolean requiresGameRestart() {
            return false;
        }
    }
}
