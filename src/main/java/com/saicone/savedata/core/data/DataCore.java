package com.saicone.savedata.core.data;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalCause;
import com.saicone.savedata.SaveData;
import com.saicone.savedata.util.OptionalType;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

public class DataCore implements Listener {

    private static final String CHANNEL_MAIN = "datacore:main";

    private final Map<String, Database> databases = new HashMap<>();
    private final Map<String, DataType<?>> dataTypes = new HashMap<>();

    private final Map<String, DataObject> globalData = new ConcurrentHashMap<>();
    private final Map<UUID, DataObject> playerData = new ConcurrentHashMap<>();
    private final Map<String, UUID> linkedPlayers = new ConcurrentHashMap<>();

    private final LinkedHashSet<Update> updates = new LinkedHashSet<>();
    private final Cache<UUID, String> toDelete = CacheBuilder.newBuilder()
            .expireAfterWrite(10, TimeUnit.SECONDS)
            .<UUID, String>removalListener(notification -> {
                if (notification.getCause() == RemovalCause.EXPIRED && notification.getKey() != null) {
                    SaveData.log(4, "The player '" + notification.getValue() + "' will be unloaded");
                    Bukkit.getScheduler().runTaskAsynchronously(SaveData.get(), () -> unloadPlayerData(notification.getKey()));
                }
            }).build();
    private BukkitTask updateTask;
    private boolean onUpdate = false;

    @NotNull
    public Map<String, Database> getDatabases() {
        return databases;
    }

    @NotNull
    public Map<String, DataType<?>> getDataTypes() {
        return dataTypes;
    }

    @NotNull
    public Map<String, DataObject> getGlobalData() {
        return globalData;
    }

    @NotNull
    public Map<UUID, DataObject> getPlayerData() {
        return playerData;
    }

    @NotNull
    public Map<String, UUID> getLinkedPlayers() {
        return linkedPlayers;
    }

    public void getDataObject(@NotNull Type type, @NotNull Object name, @NotNull Consumer<DataObject> consumer) {
        getDataObject(type, name, dataObject -> {
            consumer.accept(dataObject);
            return null;
        });
    }

    @Nullable
    public <T> T getDataObject(@NotNull Type type, @NotNull Object name, @NotNull Function<DataObject, T> function) {
        final Object key;
        DataObject dataObject;
        if (type == Type.GLOBAL) {
            key = String.valueOf(name);
            dataObject = globalData.get(key);
        } else {
            try {
                if (name instanceof UUID) {
                    key = name;
                    dataObject = playerData.get((UUID) name);
                } else {
                    key = UUID.fromString(String.valueOf(name));
                    dataObject = playerData.get(key);
                }
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
        if (dataObject == null) {
            dataObject = new DataObject();
            if (type == Type.GLOBAL) {
                globalData.put((String) key, dataObject);
            } else {
                playerData.put((UUID) key, dataObject);
            }
        }
        return function.apply(dataObject);
    }

    @Nullable
    public Object getDataValue(@NotNull Type type, @NotNull Object name, @NotNull String database, @NotNull String id) {
        return getDataObject(type, name, dataObject -> {
            return dataObject.get(database, id);
        });
    }

    @NotNull
    public String getDataString(@NotNull Type type, @NotNull Object name, @NotNull String database, @NotNull String id) {
        return getDataString(null, type, name, database, id);
    }

    @NotNull
    @SuppressWarnings("unchecked")
    public String getDataString(@Nullable Player player, @NotNull Type type, @NotNull Object name, @NotNull String database, @NotNull String id) {
        final DataType<Object> dataType = (DataType<Object>) dataTypes.get(id);
        if (dataType == null) {
            return "null";
        }
        Object value = getDataValue(type, name, database, id);
        if (value == null) {
            value = dataType.getDefaultValue();
            if (value == null) {
                return "null";
            }
        }
        return dataType.asString(dataType.wrap(value, player));
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public Object onExecute(@NotNull Type type, @NotNull Object name, @NotNull Operator operator, @NotNull String database, @NotNull String id, @Nullable Object value) {
        if (operator == Operator.GET) {
            return getDataValue(type, name, database, id);
        }
        final DataType<Object> dataType = (DataType<Object>) dataTypes.get(id);
        if (dataType == null) {
            return Result.INVALID_ID;
        }
        value = OptionalType.of(value).as(dataType.getTypeClass());
        final Object finalValue;
        if (operator == Operator.SET) {
            if (value == null) {
                return Result.INVALID_VALUE;
            }
            finalValue = value;
        } else if (operator == Operator.DELETE) {
            finalValue = null;
        } else {
            if (value == null) {
                return Result.INVALID_VALUE;
            }
            Object currentValue = getDataValue(type, name, database, id);
            if (currentValue == null) {
                currentValue = dataType.getDefaultValue();
                if (currentValue == null) {
                    return Result.CANNOT_MODIFY;
                }
            }
            switch (operator) {
                case ADD:
                    finalValue = dataType.add(currentValue, value);
                    break;
                case SUBSTRACT:
                    finalValue = dataType.substract(currentValue, value);
                    break;
                case MULTIPLY:
                    finalValue = ((NumberDataType) dataType).multiply((Number) currentValue, (Number) value);
                    break;
                case DIVIDE:
                    finalValue = ((NumberDataType) dataType).divide((Number) currentValue, (Number) value);
                    break;
                default:
                    return Result.INVALID_OPERATOR;
            }
        }
        return getDataObject(type, name, dataObject -> {
            final Object result = dataObject.set(database, id, finalValue);
            final Update update = new Update(type, name, database, id, finalValue);
            updates.remove(update);
            updates.add(update);
            return result;
        });
    }

    @SuppressWarnings("unchecked")
    public void onReceiveMessage(@NotNull String msg) {
        SaveData.log(4, "Received message '" + msg + "'");
        final String[] split = msg.split("_\\|\\|\\|_", 5);
        if (split.length < 4 || (!split[0].equalsIgnoreCase("global") && !split[0].equalsIgnoreCase("player"))) {
            return;
        }
        final DataType<Object> dataType = (DataType<Object>) dataTypes.get(split[3]);
        if (dataType == null) {
            return;
        }
        final Type type = split[0].equalsIgnoreCase("global") ? Type.GLOBAL : Type.PLAYER;
        if (type == Type.PLAYER) {
            final String updateType = split[3].toLowerCase();
            if (updateType.equals("load")) {
                onReceiveLoad(UUID.fromString(split[1]), split[2]);
            } else if (updateType.equals("unload")) {
                onReceiveUnload(UUID.fromString(split[1]), split[2]);
            }
            return;
        }
        getDataObject(type, split[1], dataObject -> {
            dataObject.set(split[2], split[3], split.length > 4 ? dataType.wrap(OptionalType.of(split[4]).as(dataType.getTypeClass())) : null, false);
        });
    }

    protected void onReceiveLoad(@NotNull UUID uuid, @NotNull String database) {
        // Do not load if the player is marked as it was here
        final String name = toDelete.getIfPresent(uuid);
        if (name == null) {
            // Avoid loaded data
            final DataObject dataObject = playerData.get(uuid);
            if (dataObject != null && dataObject.contains(database)) {
                return;
            }
            // Not override current async thread (its unnecessary)
            if (Bukkit.isPrimaryThread()) {
                Bukkit.getScheduler().runTaskAsynchronously(SaveData.get(), () -> loadPlayerData(uuid, database));
            } else {
                loadPlayerData(uuid, database);
            }
        } else {
            toDelete.invalidate(uuid);
            linkedPlayers.put(name, uuid);
        }
    }

    protected void onReceiveUnload(@NotNull UUID uuid, @NotNull String database) {
        final DataObject dataObject = playerData.get(uuid);
        if (dataObject == null || !dataObject.remove(database)) {
            return;
        }
        if (dataObject.isEmpty()) {
            linkedPlayers.entrySet().removeIf(entry -> entry.getValue().equals(uuid));
            playerData.remove(uuid);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        final UUID uuid = event.getPlayer().getUniqueId();
        toDelete.invalidate(uuid);
        Bukkit.getScheduler().runTaskLaterAsynchronously(SaveData.get(), () -> loadPlayerData(event.getPlayer()), 40L);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Add to deletion task
        toDelete.put(event.getPlayer().getUniqueId(), event.getPlayer().getName());
    }

    public void onReload() {
        SaveData.log(4, "Reloading data core");
        clear();
        final ConfigurationSection databases = SaveData.SETTINGS.getConfigurationSection("database");
        if (databases == null) {
            SaveData.log(2, "The databases configuration doesn't exists");
            return;
        }
        // Load databases
        for (String key : databases.getKeys(false)) {
            final Object value = databases.get(key);
            if (value instanceof ConfigurationSection) {
                loadDatabase(key, (ConfigurationSection) value);
            }
        }
        SaveData.log(3, "Loaded " + this.databases.size() + " database" + (this.databases.size() == 1 ? "" : "s") + ": " + String.join(", ", this.databases.keySet()));
        // Load data types
        loadFolder(new File(SaveData.get().getDataFolder(), "datatypes"));
        for (Map.Entry<String, Database> entry : this.databases.entrySet()) {
            final Database database = entry.getValue();
            database.start(database.getGlobalTable(), database.getPlayerTable());
            if (database.getMessenger() != null) {
                database.getMessenger().subscribe(CHANNEL_MAIN, this::onReceiveMessage);
                database.getMessenger().start();
            }
        }
        SaveData.log(3, "Loaded " + this.dataTypes.size() + " data type" + (this.dataTypes.size() == 1 ? "" : "s"));
        // Load data
        loadGlobalData();
        for (Player player : Bukkit.getOnlinePlayers()) {
            loadPlayerData(player);
        }
        // Inititalize update task
        if (updateTask == null) {
            SaveData.log(4, "Initializing update task");
            updateTask = Bukkit.getScheduler().runTaskTimerAsynchronously(SaveData.get(), () -> {
                if (onUpdate) {
                    return;
                }
                runUpdateTask();
            }, 100L, 100L);
        }
    }

    public void onDisable() {
        if (updateTask != null) {
            // Save queued updates
            if (!onUpdate) {
                runUpdateTask();
            }
            updateTask.cancel();
            updateTask = null;
        }
        clear();
    }

    private void runUpdateTask() {
        onUpdate = true;
        try {
            sendUpdates();
        } catch (Throwable t) {
            t.printStackTrace();
        }
        onUpdate = false;
    }

    public void sendUpdates() {
        final Iterator<Update> iterator = updates.iterator();
        final Set<DataUpdate> dataUpdates = new HashSet<>();
        final List<Update> updates = new ArrayList<>();
        while (iterator.hasNext()) {
            final Update update = iterator.next();
            iterator.remove();
            update.setUnique(true);
            dataUpdates.add(update);
            updates.add(update);
        }
        for (DataUpdate update : dataUpdates) {
            saveData(update.getType(), update.getName(), update.getDatabase());
        }
        for (Update update : updates) {
            sendUpdate(update.getType(), update.getName(), update.getDatabase(), update.getId(), update.getValue());
        }
        if (dataUpdates.size() > 0) {
            SaveData.log(4, "The update task processed " + updates.size() + " updates and " + dataUpdates.size() + " data updates in the last data update");
        }
    }

    public void sendUpdate(@NotNull Type type, @NotNull Object name, @NotNull String database, @NotNull String id, @Nullable Object value) {
        final Database db = databases.get(database);
        if (db == null || db.getMessenger() == null) {
            return;
        }
        db.getMessenger().send(CHANNEL_MAIN, type.name().toLowerCase() + "_|||_" + name + "_|||_" + database + "_|||_" + id + (value == null ? "" : "_|||_" + value));
    }

    public void loadGlobalData() {
        for (Map.Entry<String, Database> entry : this.databases.entrySet()) {
            entry.getValue().getDataClient().loadAll(entry.getValue().getGlobalTable(), (id, data) -> {
                DataObject dataObject = globalData.get(id);
                if (dataObject == null) {
                    dataObject = new DataObject();
                    globalData.put(id, dataObject);
                }
                dataObject.set(entry.getKey(), data);
            });
        }
    }

    public void loadPlayerData(@NotNull Player player) {
        loadPlayerData(player.getUniqueId());
    }

    public void loadPlayerData(@NotNull UUID uuid) {
        final String id = uuid.toString();
        final DataObject dataObject;
        if (playerData.containsKey(uuid)) {
            dataObject = playerData.get(uuid);
        } else {
            dataObject = new DataObject();
            playerData.put(uuid, dataObject);
        }
        for (Map.Entry<String, Database> entry : this.databases.entrySet()) {
            entry.getValue().getDataClient().load(entry.getValue().getPlayerTable(), id, data -> {
                dataObject.set(entry.getKey(), data);
            });
            // Send update to load current data in all connected servers
            if (entry.getValue().getMessenger() != null) {
                entry.getValue().getMessenger().send(CHANNEL_MAIN, "player_|||_" + uuid + "_|||_" + entry.getKey() + "_|||_load");
            }
        }
    }

    public void loadPlayerData(@NotNull UUID uuid, @NotNull String database) {
        final Database db = this.databases.get(database);
        if (db == null) {
            return;
        }
        final String id = uuid.toString();
        db.getDataClient().load(db.getPlayerTable(), id, data -> {
            DataObject dataObject = playerData.get(uuid);
            if (dataObject == null) {
                dataObject = new DataObject();
                playerData.put(uuid, dataObject);
            }
            dataObject.set(database, data);
        });
    }

    public void loadDatabase(@NotNull String id, @NotNull ConfigurationSection section) {
        if (!section.getBoolean("enabled", true)) {
            SaveData.log(4, "The database '" + id + "' is disabled");
            return;
        }
        final String type = section.getString("type", "FILE");
        final String globalTable = section.getString("table.global", "global_data");
        final String playerTable = section.getString("table.player", "player_data");
        final ConfigurationSection settings = section.getConfigurationSection(type.toLowerCase());
        if (settings != null) {
            databases.put(id, new Database(id, type, globalTable, playerTable, settings));
        } else {
            SaveData.log(2, "The database '" + id + "' doesn't have type settings");
        }
    }

    public void loadFolder(@NotNull File folder) {
        if (!folder.exists()) {
            return;
        }
        if (folder.isDirectory()) {
            final File[] files = folder.listFiles();
            if (files == null) {
                return;
            }
            for (File file : files) {
                loadFolder(file);
            }
        } else {
            loadFile(folder);
        }
    }

    public void loadFile(@NotNull File file) {
        final String name = file.getName().toLowerCase();
        if (!name.endsWith(".yml") && !name.endsWith(".yaml")) {
            return;
        }
        final YamlConfiguration config = new YamlConfiguration();
        try {
            config.load(file);
        } catch (Throwable t) {
            t.printStackTrace();
            return;
        }
        for (String key : config.getKeys(false)) {
            final Object value = config.get(key);
            if (value instanceof ConfigurationSection) {
                loadDataType(key, (ConfigurationSection) value);
            }
        }
    }

    public void loadDataType(@NotNull String id, @NotNull ConfigurationSection config) {
        String type = config.getString("type");
        if (type == null || type.isBlank()) {
            return;
        }
        type = type.trim().toLowerCase();
        if (type.isEmpty()) {
            return;
        }

        id = config.getString("id", id);
        if (dataTypes.containsKey(id)) {
            return;
        }
        final Object defaultValue = config.get("default");
        final String permission = config.getString("permission");

        final DataType<?> dataType = DataType.of(type, id, defaultValue, permission, config);
        if (dataType != null) {
            dataTypes.put(id, dataType);
        }
    }

    public void saveAll() {
        SaveData.log(4, "Saving all data...");
        for (Map.Entry<String, DataObject> entry : globalData.entrySet()) {
            final Iterator<String> iterator = entry.getValue().getEdited().iterator();
            while (iterator.hasNext()) {
                final String database = iterator.next();
                iterator.remove();
                final Map<String, Object> data = entry.getValue().get(database);
                final Database db = databases.get(database);
                if (db == null) {
                    continue;
                }
                if (data == null || data.isEmpty()) {
                    db.getDataClient().delete(db.getGlobalTable(), entry.getKey());
                } else {
                    db.getDataClient().save(db.getGlobalTable(), entry.getKey(), data);
                }
            }
        }
        for (Map.Entry<UUID, DataObject> entry : playerData.entrySet()) {
            savePlayerData(entry.getKey(), entry.getValue());
        }
    }

    public void saveData(@NotNull Type type, @NotNull Object name, @NotNull String database) {
        final Database db = databases.get(database);
        if (db == null) {
            return;
        }

        final Map<String, Object> data = getDataObject(type, name, dataObject -> {
            return dataObject.get(database);
        });
        if (data == null || data.isEmpty()) {
            db.getDataClient().delete(db.getTable(type), name.toString());
        } else {
            db.getDataClient().save(db.getTable(type), name.toString(), data);
        }
    }

    public void savePlayerData(@NotNull Player player) {
        final DataObject dataObject = playerData.get(player.getUniqueId());
        if (dataObject == null) {
            return;
        }
        savePlayerData(player.getUniqueId(), dataObject);
        SaveData.log(4, "The data for player " + player.getName() + " was saved successfully");
    }

    public void savePlayerData(@NotNull UUID uuid, @NotNull DataObject dataObject) {
        final Iterator<String> iterator = dataObject.getEdited().iterator();
        while (iterator.hasNext()) {
            final String database = iterator.next();
            iterator.remove();
            final Map<String, Object> data = dataObject.get(database);
            final Database db = databases.get(database);
            if (db == null) {
                continue;
            }
            if (data == null || data.isEmpty()) {
                db.getDataClient().delete(db.getPlayerTable(), uuid.toString());
            } else {
                db.getDataClient().save(db.getPlayerTable(), uuid.toString(), data);
            }
        }
    }

    public void unloadPlayerData(@NotNull UUID uuid) {
        final DataObject dataObject = playerData.remove(uuid);
        if (dataObject == null) {
            return;
        }
        for (var entry : dataObject.getData().entrySet()) {
            final Database db = databases.get(entry.getKey());
            if (db == null || db.getMessenger() == null) {
                continue;
            }
            db.getMessenger().send(CHANNEL_MAIN, "player_|||_" + uuid + "_|||_" + entry.getKey() + "_|||_unload");
        }
        dataObject.clear();
    }

    public void clear() {
        if (!databases.isEmpty()) {
            for (Map.Entry<String, Database> entry : databases.entrySet()) {
                entry.getValue().close();
            }
            databases.clear();
        }
        dataTypes.clear();
        for (Map.Entry<String, DataObject> entry : globalData.entrySet()) {
            entry.getValue().clear();
        }
        globalData.clear();
        for (Map.Entry<UUID, DataObject> entry : playerData.entrySet()) {
            entry.getValue().clear();
        }
        playerData.clear();
    }

    public enum Type {
        GLOBAL, PLAYER;
    }

    public enum Operator {
        SET, GET, ADD, DELETE, SUBSTRACT, MULTIPLY, DIVIDE;
    }

    public enum Result {
        INVALID_OPERATOR, INVALID_ID, INVALID_VALUE, CANNOT_MODIFY;
    }

    private static class DataUpdate {
        private final Type type;
        private final Object name;
        private final String database;

        public DataUpdate(@NotNull Type type, @NotNull Object name, @NotNull String database) {
            this.type = type;
            this.name = name;
            this.database = database;
        }

        @NotNull
        public Type getType() {
            return type;
        }

        @NotNull
        public Object getName() {
            return name;
        }

        @NotNull
        public String getDatabase() {
            return database;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            DataUpdate that = (DataUpdate) o;

            if (type != that.type) return false;
            if (!name.equals(that.name)) return false;
            return database.equals(that.database);
        }

        @Override
        public int hashCode() {
            int result = type.hashCode();
            result = 31 * result + name.hashCode();
            result = 31 * result + database.hashCode();
            return result;
        }
    }

    private static class Update extends DataUpdate {
        private final String id;
        private final Object value;

        private boolean unique;

        public Update(@NotNull Type type, @NotNull Object name, @NotNull String database, @NotNull String id, @Nullable Object value) {
            super(type, name, database);
            this.id = id;
            this.value = value;
        }

        @NotNull
        public String getId() {
            return id;
        }

        @Nullable
        public Object getValue() {
            return value;
        }

        public void setUnique(boolean unique) {
            this.unique = unique;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            if (!super.equals(o)) return false;

            if (unique) return true;

            Update update = (Update) o;

            return id.equals(update.id);
        }

        @Override
        public int hashCode() {
            int result = super.hashCode();
            if (unique) return result;
            result = 31 * result + id.hashCode();
            return result;
        }
    }
}
