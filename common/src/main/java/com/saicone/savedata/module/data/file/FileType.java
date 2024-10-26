package com.saicone.savedata.module.data.file;

import com.saicone.savedata.module.data.ClientType;
import com.saicone.settings.SettingsSource;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public enum FileType implements ClientType {

    HOCON(
            "conf",
            "com.saicone.settings.source.HoconSettingsSource",
            "com.saicone.settings:settings-hocon:1.0",
            Map.of("com{}saicone{}types", "com.saicone.types",
                    "com{}saicone{}settings", "com.saicone.settings")
    ),
    JSON(
            "json",
            "com.saicone.settings.source.GsonSettingsSource",
            "com.saicone.settings:settings-gson:1.0",
            Map.of("com{}saicone{}types", "com.saicone.types",
                    "com{}saicone{}settings", "com.saicone.settings")
    ),
    TOML(
            "toml",
            "com.saicone.settings.source.TomlSettingsSource",
            "com.saicone.settings:settings-toml:1.0",
            Map.of("com{}saicone{}types", "com.saicone.types",
                    "com{}saicone{}settings", "com.saicone.settings")
    ),
    YAML(
            "yml",
            "com.saicone.settings.source.YamlSettingsSource",
            "com.saicone.settings:settings-yaml:1.0",
            Map.of("com{}saicone{}types", "com.saicone.types",
                    "com{}saicone{}settings", "com.saicone.settings")
    );

    public static FileType[] VALUES = values();

    private final String extension;
    private final String source;
    private final String dependency;
    private final Map<String, String> relocations;

    FileType(@NotNull String extension, @NotNull String source, @NotNull String dependency, @NotNull Map<String, String> relocations) {
        this.extension = extension;
        this.source = source;
        this.dependency = dependency.replace("{}", ".");
        this.relocations = new HashMap<>();
        relocations.forEach((key, value) -> this.relocations.put(key.replace("{}", "."), value));
    }

    @Override
    public boolean isDependencyPresent() {
        try {
            Class.forName(source);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    @Override
    public @NotNull String getName() {
        return name();
    }

    @NotNull
    public String getExtension() {
        return extension;
    }

    @NotNull
    public String getSource() {
        return source;
    }

    @Override
    public @NotNull String getDependency() {
        return dependency;
    }

    @Override
    public @NotNull Map<String, String> getRelocations() {
        return relocations;
    }

    @NotNull
    public Path getFolder(@NotNull Path parent) {
        return parent.resolve(name().toLowerCase());
    }

    @NotNull
    public SettingsSource createSource() {
        try {
            return (SettingsSource) Class.forName(source).getDeclaredConstructor().newInstance();
        } catch (ClassNotFoundException | InvocationTargetException | InstantiationException | IllegalAccessException | NoSuchMethodException e) {
            throw new RuntimeException("Cannot initialize the SettingsSource " + source + " with reflection", e);
        }
    }

    @Nullable
    @Contract("_, !null -> !null")
    public static FileType of(@NotNull String name, @Nullable FileType def) {
        for (FileType value : VALUES) {
            if (value.name().equalsIgnoreCase(name)) {
                return value;
            }
        }
        return def;
    }
}
