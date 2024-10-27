package com.saicone.savedata.api.data;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.saicone.savedata.SaveData;
import com.saicone.savedata.api.SaveDataAPI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class DataNode implements ConcurrentMap<String, DataEntry<?>> {

    private static final Gson GSON = new Gson();
    private static final Type MAP_TYPE = new TypeToken<Map<String, Object>>(){}.getType();

    private final String database;
    private final ConcurrentMap<String, DataEntry<?>> entries = new ConcurrentHashMap<>();

    @NotNull
    @Deprecated
    public static DataNode of(@NotNull String database, @NotNull String data) {
        final DataNode node = new DataNode(database);
        final Map<String, Object> map = GSON.fromJson(data, MAP_TYPE);
        for (Entry<String, Object> entry : map.entrySet()) {
            final DataType<Object> dataType = SaveDataAPI.getDataType(entry.getKey());
            if (dataType == null) {
                SaveData.log(2, "Found invalid data type '" + entry.getKey() + "' while getting deprecated data");
                continue;
            }
            final Object parsedValue;
            try {
                parsedValue = dataType.load(entry.getValue());
            } catch (Throwable t) {
                SaveData.log(2, () -> "Cannot parse value '" + entry.getValue() + "'  as " +  dataType.getTypeName() + " while getting deprecated data");
                continue;
            }
            node.put(entry.getKey(), new DataEntry<>(dataType, parsedValue, null));
        }
        return node;
    }

    public DataNode(@NotNull String database) {
        this.database = database;
    }

    @NotNull
    public String getDatabase() {
        return database;
    }

    // Vanilla map

    @Override
    public int size() {
        return entries.size();
    }

    @Override
    public boolean isEmpty() {
        return entries.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return entries.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return entries.containsValue(value);
    }

    @Override
    public DataEntry<?> get(Object key) {
        return entries.get(key);
    }

    @Nullable
    @Override
    public DataEntry<?> put(String key, DataEntry<?> value) {
        return entries.put(key, value);
    }

    @Override
    public DataEntry<?> remove(Object key) {
        return entries.remove(key);
    }

    @Override
    public void putAll(@NotNull Map<? extends String, ? extends DataEntry<?>> m) {
        entries.putAll(m);
    }

    @Override
    public void clear() {
        entries.clear();
    }

    @NotNull
    @Override
    public Set<String> keySet() {
        return entries.keySet();
    }

    @NotNull
    @Override
    public Collection<DataEntry<?>> values() {
        return entries.values();
    }

    @NotNull
    @Override
    public Set<Entry<String, DataEntry<?>>> entrySet() {
        return entries.entrySet();
    }

    @Override
    public DataEntry<?> putIfAbsent(@NotNull String key, DataEntry<?> value) {
        return entries.putIfAbsent(key, value);
    }

    @Override
    public boolean remove(@NotNull Object key, Object value) {
        return entries.remove(key, value);
    }

    @Override
    public boolean replace(@NotNull String key, @NotNull DataEntry<?> oldValue, @NotNull DataEntry<?> newValue) {
        return entries.replace(key, oldValue, newValue);
    }

    @Override
    public DataEntry<?> replace(@NotNull String key, @NotNull DataEntry<?> value) {
        return entries.replace(key, value);
    }
}
