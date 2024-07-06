package com.saicone.savedata.module.settings;

import com.saicone.savedata.util.OptionalType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

public class BukkitSettings extends YamlConfiguration {

    private final String path;

    @NotNull
    public static BukkitSettings of(@Nullable ConfigurationSection section) {
        final BukkitSettings settings = new BukkitSettings();
        if (section != null) {
            settings.set(section);
        }
        return settings;
    }

    private BukkitSettings() {
        this.path = null;
    }

    public BukkitSettings(@NotNull String path) {
        this.path = path;
    }

    @Nullable
    public String getPath() {
        return path;
    }

    @NotNull
    public OptionalType getOptional(@NotNull String path) {
        return OptionalType.of(get(path));
    }

    public void set(@NotNull ConfigurationSection section) {
        for (String key : section.getKeys(false)) {
            set(key, section.get(key));
        }
    }

    public void set(@NotNull Map<String, Object> map) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            final Object value = entry.getValue();
            if (value instanceof Map) {
                createSection(entry.getKey(), (Map<?, ?>) value);
            } else {
                set(entry.getKey(), value);
            }
        }
    }

    @NotNull
    public Map<String, Object> asMap() {
        return fromSection(this);
    }

    public void loadFrom(@NotNull File folder) {
        loadFrom(folder, false);
    }

    public void loadFrom(@NotNull File folder, boolean save) {
        if (save) {
            saveResource(folder, path, false);
        }

        final File file = new File(folder, path);
        try {
            load(file);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    @Nullable
    public static InputStream getResource(@NotNull String name) {
        try {
            final URL url = BukkitSettings.class.getClassLoader().getResource(name);
            if (url == null) {
                return null;
            }

            final URLConnection connection = url.openConnection();
            connection.setUseCaches(false);
            return connection.getInputStream();
        } catch (IOException ex) {
            return null;
        }
    }

    public static void saveResource(@NotNull File folder, @NotNull String path, boolean replace) {
        if (path.isBlank()) {
            return;
        }
        path = path.replace('\\', '/');

        final File file = new File(folder, path);
        if (!replace && file.exists()) {
            return;
        }

        final InputStream in = getResource(path);
        if (in == null) {
            return;
        }

        if (!folder.exists()) {
            folder.mkdirs();
        }

        try {
            Files.copy(in, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @NotNull
    public static Map<String, Object> fromSection(@NotNull ConfigurationSection section) {
        final Map<String, Object> map = new HashMap<>();
        for (String key : section.getKeys(false)) {
            final Object value = section.get(key);
            if (value instanceof ConfigurationSection) {
                map.put(key, fromSection((ConfigurationSection) value));
            } else {
                map.put(key, value);
            }
        }
        return map;
    }
}
