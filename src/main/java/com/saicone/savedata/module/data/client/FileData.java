package com.saicone.savedata.module.data.client;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.saicone.savedata.module.data.DataClient;
import com.saicone.savedata.module.settings.BukkitSettings;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class FileData implements DataClient {

    public static final Gson GSON = new Gson();
    public static final Type MAP_TYPE = new TypeToken<Map<String, Object>>(){}.getType();

    private final FileType type;
    private final File folder;

    public FileData(@NotNull String type, @NotNull File folder) {
        this.type = FileType.of(type);
        this.folder = folder;
    }

    @NotNull
    public FileType getType() {
        return type;
    }

    @NotNull
    public File getFolder() {
        return folder;
    }

    @NotNull
    public File getFile(@NotNull String table, @NotNull String id) {
        return new File(folder, table + '/' + id + type.getSuffix());
    }

    @Override
    public void start(@NotNull String... tables) {
        for (String table : tables) {
            if (table.isBlank()) {
                continue;
            }
            new File(folder, table).mkdirs();
        }
    }

    @Override
    public void close() {
        // empty by default
    }

    @Override
    public void save(@NotNull String table, @NotNull String id, @NotNull Map<String, Object> data) {
        final File file = getFile(table, id);
        if (file.exists()) {
            file.delete();
        }
        switch (type) {
            case JSON:
                try (Writer writer = new BufferedWriter(new FileWriter(file))) {
                    file.createNewFile();
                    GSON.toJson(data, writer);
                } catch (Throwable t) {
                    t.printStackTrace();
                }
                break;
            case YAML:
                try {
                    final BukkitSettings settings = new BukkitSettings(id + type.getSuffix());
                    settings.set(data);
                    settings.save(file);
                } catch (Throwable t) {
                    t.printStackTrace();
                }
                break;
        }
    }

    @Override
    public void delete(@NotNull String table, @NotNull String id) {
        final File file = getFile(table, id);
        if (file.exists()) {
            file.delete();
        }
    }

    @Override
    public void load(@NotNull String table, @NotNull String id, @NotNull Consumer<Map<String, Object>> consumer) {
        final File file = getFile(table, id);
        if (!file.exists()) {
            return;
        }
        switch (type) {
            case JSON:
                try {
                    consumer.accept(readJson(file));
                } catch (Throwable t) {
                    t.printStackTrace();
                }
                break;
            case YAML:
                try {
                    consumer.accept(readYaml(file));
                } catch (Throwable t) {
                    t.printStackTrace();
                }
                break;
        }
    }

    @Override
    public void loadAll(@NotNull String table, @NotNull BiConsumer<String, Map<String, Object>> consumer) {
        final File folder = new File(this.folder, table);
        if (!folder.exists()) {
            return;
        }
        try (Stream<Path> walk = Files.walk(folder.toPath(), 1)) {
            switch (type) {
                case JSON:
                    walk.filter(Files::isRegularFile).filter(path -> path.getFileName().toString().toLowerCase().endsWith(".json")).forEach(path -> {
                        try {
                            final String name = path.getFileName().toString();
                            consumer.accept(name.substring(0, name.lastIndexOf('.')), readJson(path.toFile()));
                        } catch (Throwable t) {
                            t.printStackTrace();
                        }
                    });
                    break;
                case YAML:
                    walk.filter(Files::isRegularFile).filter(path -> path.getFileName().toString().toLowerCase().endsWith(".yaml")).forEach(path -> {
                        try {
                            final String name = path.getFileName().toString();
                            consumer.accept(name.substring(0, name.lastIndexOf('.')), readYaml(path.toFile()));
                        } catch (Throwable t) {
                            t.printStackTrace();
                        }
                    });
                    break;
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    @NotNull
    private Map<String, Object> readJson(@NotNull File file) throws IOException {
        try (Reader reader = new BufferedReader(new FileReader(file))) {
            return GSON.fromJson(reader, MAP_TYPE);
        }
    }

    @NotNull
    private Map<String, Object> readYaml(@NotNull File file) throws IOException, InvalidConfigurationException {
        final YamlConfiguration config = new YamlConfiguration();
        config.load(file);
        return BukkitSettings.fromSection(config);
    }

    public enum FileType {
        JSON(".json"),
        YAML(".yml");

        private final String suffix;

        FileType(@NotNull String suffix) {
            this.suffix = suffix;
        }

        @NotNull
        public String getSuffix() {
            return suffix;
        }

        @NotNull
        public static FileType of(@NotNull String s) {
            switch (s.trim().toUpperCase()) {
                case "YAML":
                    return FileType.YAML;
                case "JSON":
                default:
                    return FileType.JSON;
            }
        }
    }
}
