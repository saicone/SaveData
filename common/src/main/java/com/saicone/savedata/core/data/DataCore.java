package com.saicone.savedata.core.data;

import com.saicone.mcode.module.task.Task;
import com.saicone.savedata.SaveData;
import com.saicone.savedata.api.data.DataEntry;
import com.saicone.savedata.api.data.DataNode;
import com.saicone.savedata.api.data.DataOperator;
import com.saicone.savedata.api.data.DataResult;
import com.saicone.savedata.api.data.DataType;
import com.saicone.savedata.api.data.DataUser;
import com.saicone.savedata.api.data.type.CollectionDataType;
import com.saicone.savedata.api.top.TopEntry;
import com.saicone.savedata.util.DurationFormatter;
import com.saicone.settings.Settings;
import com.saicone.settings.SettingsData;
import com.saicone.settings.SettingsNode;
import com.saicone.settings.node.MapNode;
import org.javatuples.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.function.Predicate;

public class DataCore {

    private final Executor executor = Task::async;

    private final Map<String, Database> databases = new HashMap<>();
    private final Map<String, DataType<?>> dataTypes = new HashMap<>();
    private final Map<UUID, DataUser> userData = new ConcurrentHashMap<>();

    public void onLoad() {
        // Load data types
        loadDataTypes(SaveData.get().getFolder().resolve("datatypes"));
        SaveData.log(3, "Loaded " + this.dataTypes.size() + " data type" + (this.dataTypes.size() == 1 ? "" : "s"));

        // Load databases
        final Settings databaseConfig = SettingsData.of("databases.yml").or(com.saicone.settings.data.DataType.INPUT_STREAM, "databases.yml").load(SaveData.get().getFolder().toFile());
        for (Map.Entry<String, SettingsNode> entry : databaseConfig.getValue().entrySet()) {
            if (entry.getValue().isMap()) {
                final MapNode node = entry.getValue().asMapNode();
                if (node.getIgnoreCase("enabled").asBoolean(true)) {
                    final String type = node.getIgnoreCase("type").asString();
                    if (type == null || (!type.equalsIgnoreCase("FILE") && !type.equalsIgnoreCase("SQL"))) {
                        continue;
                    }
                    final Database database = new Database(entry.getKey(), type);
                    database.onLoad(node);
                    if (database.getMessenger() != null) {
                        database.getMessenger().subscribe((lines) -> {
                            if (lines.length < 2) {
                                return;
                            }
                            final UUID user = UUID.fromString(lines[0]);
                            final String key = lines[1];
                            if (!dataTypes.containsKey(key)) {
                                return;
                            }

                            if (lines.length > 2) {
                                final TopEntry<?> top = database.getTop(key);
                                if (top != null) {
                                    top.update(user, lines[2]);
                                }
                            }

                            if (!userData.containsKey(user)) {
                                return;
                            }
                            final DataEntry<?> dataEntry = database.getClient().loadDataEntry(user, key, dataTypes.get(key));
                            if (dataEntry == null) {
                                userData.get(user).removeEntry(entry.getKey(), key);
                            } else {
                                userData.get(user).setEntry(entry.getKey(), dataEntry);
                            }
                        });
                    }
                    databases.put(entry.getKey(), database);
                }
            }
        }
        SaveData.log(3, "Loaded " + this.databases.size() + " database" + (this.databases.size() == 1 ? "" : "s") + ": " + String.join(", ", this.databases.keySet()));
    }

    public void onEnable() {
        for (Map.Entry<String, Database> entry : databases.entrySet()) {
            entry.getValue().onEnable();
        }
        loadUser(DataUser.SERVER_ID);
    }

    public void onDisable() {
        saveAllUsers();
        for (Map.Entry<String, Database> entry : databases.entrySet()) {
            entry.getValue().onDisable();
        }
    }

    public void onReload() {
        onDisable();
        onLoad();
        onEnable();
    }

    @NotNull
    public Map<String, Database> getDatabases() {
        return databases;
    }

    @NotNull
    public Map<String, DataType<?>> getDataTypes() {
        return dataTypes;
    }

    @NotNull
    public CompletableFuture<DataUser> getUser(@NotNull UUID uniqueId) {
        final DataUser user = userData.get(uniqueId);
        if (user != null) {
            return CompletableFuture.completedFuture(user);
        }
        return CompletableFuture.supplyAsync(() -> loadUser(uniqueId), executor);
    }

    @NotNull
    public CompletableFuture<DataUser> getTransitiveUser(@NotNull UUID uniqueId) {
        return getTransitiveUser(uniqueId, database -> true);
    }

    @NotNull
    @SuppressWarnings("unchecked")
    public CompletableFuture<DataUser> getTransitiveUser(@NotNull UUID uniqueId, @NotNull Predicate<Database> predicate) {
        final DataUser user = userData.get(uniqueId);
        if (user != null) {
            return CompletableFuture.completedFuture(user);
        }
        SaveData.log(4, "The user " + uniqueId + " doesn't exist, so it will be get as transitive object");
        return CompletableFuture.supplyAsync(() -> {
            final DataUser loaded;
            if (userData.containsKey(uniqueId)) {
                SaveData.log(4, "The user " + uniqueId + " doesn't need any load, it exists on cache");
                loaded = userData.get(uniqueId);
            } else {
                SaveData.log(4, "Loading user " + uniqueId + "...");
                loaded = new DataUser(uniqueId);
                for (Map.Entry<String, Database> entry : databases.entrySet()) {
                    if (!predicate.test(entry.getValue())) {
                        continue;
                    }
                    loaded.setNode(entry.getKey(), entry.getValue().getClient().loadData(uniqueId, key -> (DataType<Object>) dataTypes.get(key)));
                }
            }
            return loaded;
        }, executor);
    }

    @Nullable
    public DataUser getUserOrNull(@NotNull UUID uniqueId) {
        return userData.get(uniqueId);
    }

    @NotNull
    public Map<UUID, DataUser> getUserData() {
        return userData;
    }

    @NotNull
    public DataUser getServerUser() {
        return userData.get(DataUser.SERVER_ID);
    }

    @NotNull
    @SuppressWarnings("unchecked")
    public CompletableFuture<Object> userValue(@NotNull UUID uniqueId, @NotNull DataOperator operator, @NotNull String database, @NotNull String dataType, @Nullable Object value, @NotNull Function<String, String> userParser, @Nullable Object language) {
        SaveData.log(4, "Executing userValue from DataCore");
        if (!operator.isEval()) {
            return CompletableFuture.completedFuture(DataResult.INVALID_OPERATOR);
        }
        return getTransitiveUser(uniqueId, db -> db.getName().equals(database)).thenApply(user -> {
            DataEntry<Object> entry = (DataEntry<Object>) user.getEntry(database, dataType);
            if (entry == null) {
                final DataType<Object> type = (DataType<Object>) dataTypes.get(dataType);
                if (type == null) {
                    return DataResult.INVALID_TYPE;
                }
                entry = new DataEntry<>(type);
                user.setEntry(database, entry);
            }
            if (operator == DataOperator.GET) {
                return entry.getUserValue(userParser);
            } else if (operator == DataOperator.CONTAINS && entry.getType() instanceof CollectionDataType) {
                if (value == null || entry.getValue() == null) {
                    return null;
                }
                final Object element;
                try {
                    element = ((CollectionDataType<?, ?>) entry.getType()).loadElement(value);
                } catch (Throwable t) {
                    return DataResult.INVALID_VALUE;
                }
                return entry.getType().test(entry.getValue(), element);
            } else if (operator == DataOperator.EXPIRY) {
                if (entry.isTemporary()) {
                    final Duration duration = Duration.between(Instant.now(), Instant.ofEpochMilli(entry.getExpiration()));
                    if (duration.isNegative()) {
                        return 0;
                    }
                    return DurationFormatter.format(language, duration, value instanceof String ? (String) value : "time");
                } else {
                    return 0;
                }
            }
            return null;
        });
    }

    @NotNull
    public CompletableFuture<Object> executeUpdate(@NotNull UUID uniqueId, @NotNull DataOperator operator, @NotNull String database, @NotNull String dataType, @Nullable Object value, @Nullable Long expiration) {
        return executeUpdate(uniqueId, operator, database, dataType, value, expiration, s -> s);
    }

    @NotNull
    @SuppressWarnings("unchecked")
    public CompletableFuture<Object> executeUpdate(@NotNull UUID uniqueId, @NotNull DataOperator operator, @NotNull String database, @NotNull String dataType, @Nullable Object value, @Nullable Long expiration, @NotNull Function<String, String> userParser) {
        if (!operator.isUpdate()) {
            return CompletableFuture.completedFuture(DataResult.INVALID_OPERATOR);
        }
        return getTransitiveUser(uniqueId, db -> db.getName().equals(database)).thenApply(user -> {
            DataEntry<Object> entry = (DataEntry<Object>) user.getEntry(database, dataType);
            if (entry == null) {
                final DataType<Object> type = (DataType<Object>) dataTypes.get(dataType);
                if (type == null) {
                    return DataResult.INVALID_TYPE;
                }
                entry = new DataEntry<>(type);
                user.setEntry(database, entry);
            }

            final Object result;
            if (operator == DataOperator.DELETE) {
                result = null;
            } else {
                final Object providedValue;
                try {
                    if (operator != DataOperator.SET && entry.getType() instanceof CollectionDataType) {
                        providedValue = ((CollectionDataType<?, ?>) entry.getType()).parseElement(value, userParser);
                    } else {
                        providedValue = entry.getType().parse(value, userParser);
                    }
                } catch (Throwable t) {
                    SaveData.log(4, "Cannot parse provided value", t);
                    return DataResult.INVALID_VALUE;
                }
                if (operator == DataOperator.SET) {
                    result = providedValue;
                } else {
                    final Object entryValue = entry.getValue() == null ? entry.getType().getDefaultValue() : entry.getValue();
                    if (entryValue == null) {
                        return DataResult.CANNOT_MODIFY;
                    }
                    switch (operator) {
                        case ADD:
                            result = entry.getType().add(entryValue, providedValue);
                            break;
                        case SUBSTRACT:
                            result = entry.getType().remove(entryValue, providedValue);
                            break;
                        case MULTIPLY:
                            result = entry.getType().multiply(entryValue, providedValue);
                            break;
                        case DIVIDE:
                            result = entry.getType().divide(entryValue, providedValue);
                            break;
                        default:
                            return DataResult.INVALID_OPERATOR;
                    }
                }
            }
            final Object oldValue = entry.getValue();
            entry.setValue(result);
            if (expiration != null) {
                entry.setExpiration(expiration);
            }
            final DataEntry<Object> finalEntry = entry;
            Task.runAsync(() -> databases.get(database).saveDataEntry(uniqueId, finalEntry));
            return Pair.with(oldValue, result);
        });
    }

    protected void loadDataTypes(@NotNull Path path) {
        if (!Files.exists(path)) {
            if (path.endsWith("datatypes")) {
                try {
                    Files.createDirectories(path);
                    SettingsData.of(com.saicone.settings.data.DataType.INPUT_STREAM, "datatypes/default.yml").saveInto(SettingsData.of("default.yml").parentFolder(path.toFile()));
                    loadDataTypes(path.resolve("default.yml"));
                } catch (IOException e) {
                    SaveData.logException(2, e, "Cannot create " + path + " directory");
                }
            }
            return;
        }
        if (Files.isDirectory(path)) {
            try (DirectoryStream<Path> paths = Files.newDirectoryStream(path)) {
                for (Path subPath : paths) {
                    loadDataTypes(subPath);
                }
            } catch (IOException e) {
                SaveData.logException(2, e, "Cannot read " + path + " directory");
            }
        } else {
            final Settings settings = SettingsData.of(path.getFileName().toString()).load(path.getParent().toFile());
            for (Map.Entry<String, SettingsNode> entry : settings.getValue().entrySet()) {
                if (entry.getValue().isObject()) {
                    final String type = entry.getValue().asString();
                    if (type != null) {
                        this.dataTypes.put(entry.getKey(), DataType.builder(entry.getKey(), type).build());
                    }
                    continue;
                } else if (!entry.getValue().isMap()) {
                    continue;
                }
                final MapNode node = entry.getValue().asMapNode();
                final String id = node.getRegex("(?i)id(entifier)?").asString(entry.getKey());
                final String type = node.getIgnoreCase("type").asString("string");
                loadDataType(DataType.builder(id, type), node);
            }
        }
    }

    protected <T> void loadDataType(@NotNull DataType.Builder<T> builder, @NotNull MapNode node) {
        final Object defaultValue = node.getRegex("(?i)default(-?value)?").getValue();
        if (defaultValue != null) {
            try {
                builder.defaultValue(builder.parser().parse(defaultValue));
            } catch (Throwable t) {
                SaveData.logException(1, t, "Cannot parse '" + defaultValue + "' as required data type '" + builder.id() + "'");
                return;
            }
        }
        builder.permission(node.getRegex("(?i)perm(ission)?").asString());
        builder.expression(node.getRegex("(?i)expression|parse").asString());
        builder.userParseable(node.getRegex("(?i)user-?parse(able)?|papi|(parse-?)?placeholders?").asBoolean(false));
        builder.colored(node.getRegex("(?i)color(ed)?").asBoolean(false));
        final Object min = node.getRegex("(?i)minimum").getValue();
        if (min != null) {
            try {
                builder.min(builder.parser().parse(min));
            } catch (Throwable t) {
                SaveData.logException(2, t, "Cannot parse '" + min + "' as required data type '" + builder.id() + "'");
            }
        }
        final Object max = node.getRegex("(?i)maximum").getValue();
        if (max != null) {
            try {
                builder.min(builder.parser().parse(max));
            } catch (Throwable t) {
                SaveData.logException(2, t, "Cannot parse '" + max + "' as required data type '" + builder.id() + "'");
            }
        }
        builder.format(node.getRegex("(?i)(number-?)?format").asString());
        this.dataTypes.put(builder.id(), builder.build());
    }

    @NotNull
    @SuppressWarnings("unchecked")
    public synchronized DataUser loadUser(@NotNull UUID uniqueId) {
        if (userData.containsKey(uniqueId)) {
            SaveData.log(4, "The user " + uniqueId + " doesn't need any load, it exists on cache");
            return userData.get(uniqueId);
        }
        final DataUser user = new DataUser(uniqueId);
        for (Map.Entry<String, Database> entry : databases.entrySet()) {
            user.setNode(entry.getKey(), entry.getValue().getClient().loadData(uniqueId, key -> (DataType<Object>) dataTypes.get(key)));
        }
        SaveData.log(4, "Saving user " + uniqueId + " into cache...");
        userData.put(uniqueId, user);
        return user;
    }

    public void saveAllUsers() {
        for (Map.Entry<UUID, DataUser> entry : userData.entrySet()) {
            saveUser(entry.getValue());
        }
    }

    public void saveUser(@NotNull UUID uniqueId) {
        final DataUser user = getUserOrNull(uniqueId);
        if (user != null) {
            saveUser(user);
            userData.remove(uniqueId);
        }
    }

    public void saveUser(@NotNull DataUser user) {
        SaveData.log(4, "Saving user " + user.getUniqueId() + " into database...");
        for (Map.Entry<String, DataNode> entry : user.getNodes().entrySet()) {
            final Database database = databases.get(entry.getKey());
            if (database == null) {
                SaveData.log(2, "The user " + user.getUniqueId() + " contains unknown database name: " + entry.getKey());
                continue;
            }
            database.getClient().saveData(user.getUniqueId(), entry.getValue());
        }
    }

    public void clear() {
        databases.clear();
        dataTypes.clear();
        userData.clear();
    }
}
