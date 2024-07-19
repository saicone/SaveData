package com.saicone.savedata.api.data;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class DataNode implements ConcurrentMap<String, DataEntry<?>> {

    private final String database;
    private final ConcurrentMap<String, DataEntry<?>> entries = new ConcurrentHashMap<>();

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
