package com.saicone.savedata.module.data.client;

import com.saicone.ezlib.EzlibLoader;
import com.saicone.savedata.SaveData;
import com.saicone.savedata.api.data.DataEntry;
import com.saicone.savedata.api.data.DataNode;
import com.saicone.savedata.api.data.DataType;
import com.saicone.savedata.module.data.DataClient;
import com.saicone.savedata.module.data.file.FileType;
import com.saicone.settings.Settings;
import com.saicone.settings.SettingsData;
import com.saicone.settings.SettingsNode;
import com.saicone.settings.node.MapNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;

public class FileClient implements DataClient {

    private final String databaseName;
    private final Path parentFolder;

    private FileType type;
    private Path folder;

    private final Map<UUID, SettingsData<Settings>> userData = new HashMap<>();

    public FileClient(@NotNull String databaseName, @NotNull Path parentFolder) {
        this.databaseName = databaseName;
        this.parentFolder = parentFolder;
    }

    @Override
    public void onLoad(@NotNull MapNode config) {
        this.type = null;
        final String type = config.getIgnoreCase("type").asString("json");
        this.type = FileType.of(type, null);

        if (this.type == null) {
            SaveData.log(1, "Cannot initialize FILE database, the file type '" + type + "' doesn't exists");
            return;
        }

        if (!this.type.isDependencyPresent()) {
            SaveData.bootstrap().getLibraryLoader().applyDependency(new EzlibLoader.Dependency().path(this.type.getDependency()).relocate(this.type.getRelocations()));
        }

        this.folder = this.type.getFolder(this.parentFolder.resolve("database").resolve(this.databaseName));
    }

    @Override
    public void onStart() {
        if (this.folder != null && !Files.exists(folder)) {
            try {
                Files.createDirectories(folder);
            } catch (IOException e) {
                SaveData.logException(1, e, "Cannot create database folder");
            }
        }
    }

    @Override
    public void onClose() {
        userData.clear();
    }

    @NotNull
    @Override
    public String getDatabaseName() {
        return databaseName;
    }

    @NotNull
    @Override
    public FileType getType() {
        return type;
    }

    @NotNull
    public Path getParentFolder() {
        return parentFolder;
    }

    @NotNull
    public Path getFolder() {
        return folder;
    }

    @NotNull
    public SettingsData<Settings> getData(@NotNull UUID user) {
        SettingsData<Settings> data = this.userData.get(user);
        if (data == null) {
            data = SettingsData.of(com.saicone.settings.data.DataType.FILE, user + "." + this.type.getExtension())
                    .parentFolder(this.folder.toFile());
            this.userData.put(user, data);
        }
        return data;
    }

    @Override
    public @NotNull DataNode loadData(@NotNull UUID user, @NotNull Function<String, DataType<Object>> dataProvider) {
        SaveData.log(4, "Loading user " + user + " from file type " + type.getName());
        final DataNode node = new DataNode(this.databaseName);
        final SettingsData<Settings> data = getData(user);
        if (data.getFile().exists()) {
            SaveData.log(4, "The file " + data.getFile().getName() + " exists");
            final long time = System.currentTimeMillis();
            final Settings config = data.load();
            if (!config.isEmpty()) {
                SaveData.log(4, "The file " + data.getFile().getName() + " is not empty");
                for (Map.Entry<String, SettingsNode> entry : config.getValue().entrySet()) {
                    if (!entry.getValue().isMap()) {
                        SaveData.log(4, "The entry " + entry.getKey() + " is not map");
                        continue;
                    }
                    final DataType<Object> dataType = dataProvider.apply(entry.getKey());
                    if (dataType == null) {
                        SaveData.log(2, "Found invalid data type '" + entry.getKey() + "' for user " + user, ", ignoring it...");
                        continue;
                    }
                    final MapNode map = entry.getValue().asMapNode();
                    final long expiration = map.getIgnoreCase("expiration").asLong(0L);
                    if (expiration > 0 && time >= expiration) {
                        SaveData.log(4, "entry " + entry.getKey() + " expired");
                        continue;
                    }
                    final String type = map.getIgnoreCase("type").asString();
                    final String value = map.getIgnoreCase("value").asString();
                    final Object parsedValue;
                    try {
                        parsedValue = dataType.load(value);
                    } catch (Throwable t) {
                        SaveData.log(2, () -> "Cannot parse value '" + value + "' with data type " + type + " as " +  dataType.getTypeName() + " for user " + user + ", deleting it...");
                        continue;
                    }
                    final DataEntry<?> dataEntry = new DataEntry<>(dataType, parsedValue, expiration);
                    SaveData.log(4, () -> "- " + dataEntry);
                    node.put(entry.getKey(), dataEntry);
                }
            }
        } else {
            SaveData.log(4, "The user doesn't exist as file");
        }
        return node;
    }

    @Override
    public @Nullable <T> DataEntry<T> loadDataEntry(@NotNull UUID user, @NotNull String key, @NotNull DataType<T> dataType) {
        final long time = System.currentTimeMillis();
        DataEntry<T> entry = null;
        final Settings config = getData(user).load();
        if (!config.isEmpty()) {
            final SettingsNode node = config.get(key);
            if (!node.isMap()) {
                return null;
            }
            final MapNode map = node.asMapNode();
            final long expiration = map.getIgnoreCase("expiration").asLong(0L);
            if (expiration > 0 && time >= expiration) {
                return null;
            }
            final String type = map.getIgnoreCase("type").asString();
            final String value = map.getIgnoreCase("value").asString();
            final T parsedValue;
            try {
                parsedValue = dataType.load(value);
            } catch (Throwable t) {
                SaveData.log(2, () -> "Cannot parse value '" + value + "' with data type " + type + " as " +  dataType.getTypeName() + " for user " + user + ", deleting it...");
                return null;
            }
            entry = new DataEntry<>(dataType, parsedValue, expiration);
        }
        return entry;
    }

    @Override
    public @NotNull <T> Map<UUID, T> loadTopEntry(@NotNull String key, @NotNull DataType<T> dataType) {
        final Map<UUID, T> data = new HashMap<>();
        try (Stream<Path> walk = Files.walk(this.folder, 1)) {
            final Iterator<UUID> iterator = walk
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase().endsWith("." + this.type.getExtension()))
                    .map(path -> {
                        try {
                            final String name = path.getFileName().toString();
                            return UUID.fromString(name.substring(0, name.lastIndexOf('.')));
                        } catch (IllegalArgumentException e) {
                            return null;
                        }
                    })
                    .iterator();
            while (iterator.hasNext()) {
                final UUID user = iterator.next();
                final Settings config = getData(user).load();
                if (!config.isEmpty()) {
                    final SettingsNode node = config.get(key);
                    if (!node.isMap()) {
                        continue;
                    }
                    final MapNode map = node.asMapNode();
                    final String value = map.getIgnoreCase("value").asString();
                    final T parsedValue;
                    try {
                        parsedValue = dataType.load(value);
                    } catch (Throwable t) {
                        continue;
                    }
                    data.put(user, parsedValue);
                }
            }
        } catch (IOException e) {
            SaveData.logException(2, e, "Cannot load top data");
        }
        return data;
    }

    @Override
    public void saveData(@NotNull UUID user, @NotNull DataNode node) {
        final SettingsData<Settings> data = getData(user);
        if (node.isEmpty()) {
            final File file = data.getFile();
            if (file.exists()) {
                file.delete();
            }
            return;
        }
        for (Map.Entry<String, DataEntry<?>> entry : node.entrySet()) {
            saveData(data, entry.getValue());
        }
        saveToFile(data);
    }

    @Override
    public void saveDataEntry(@NotNull UUID user, @NotNull DataEntry<?> entry) {
        final SettingsData<Settings> data = saveData(getData(user), entry);
        saveToFile(data);
    }

    @NotNull
    private SettingsData<Settings> saveData(@NotNull SettingsData<Settings> data, @NotNull DataEntry<?> entry) {
        if (data.getLoaded() == null) {
            data.loaded(new Settings());
        }
        final MapNode map = data.getLoaded().asMapNode();
        if (entry.getValue() == null) {
            map.remove(entry.getType().getId());
        } else {
            final MapNode node = new MapNode();
            if (entry.getType().getTypeName() != null) {
                node.put("type", entry.getType().getTypeName());
            }
            node.put("value", entry.getSavedValue());
            if (entry.isTemporary()) {
                node.put("expiration", entry.getExpiration());
            }
            map.put(entry.getType().getId(), node);
        }
        return data;
    }

    private void saveToFile(@NotNull SettingsData<Settings> data) {
        if (data.getLoaded().isEmpty()) {
            final File file = data.getFile();
            if (file.exists()) {
                file.delete();
            }
        } else {
            data.getSource();
            data.save();
        }
    }

    @Override
    public void deleteData(@NotNull Map<String, Object> columns) {
        throw new RuntimeException("Not yet supported");
    }
}
