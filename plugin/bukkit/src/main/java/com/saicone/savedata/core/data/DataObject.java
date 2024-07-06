package com.saicone.savedata.core.data;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class DataObject {

    private final Map<String, Map<String, Object>> data = new ConcurrentHashMap<>();
    private final Set<String> edited = new HashSet<>();

    public boolean contains(@NotNull String database) {
        return data.containsKey(database);
    }

    public boolean isEmpty() {
        return data.isEmpty();
    }

    @Nullable
    public Map<String, Object> get(@NotNull String database) {
        return data.get(database);
    }

    @Nullable
    public Object get(@NotNull String database, @NotNull String id) {
        final Map<String, Object> map = data.get(database);
        return map == null ? null : map.get(id);
    }

    @NotNull
    public Map<String, Map<String, Object>> getData() {
        return data;
    }

    @NotNull
    public Set<String> getEdited() {
        return edited;
    }

    public void set(@NotNull String database, @NotNull Map<String, Object> map) {
        data.put(database, map instanceof ConcurrentHashMap ? map : new ConcurrentHashMap<>(map));
    }

    @Nullable
    public synchronized Object set(@NotNull String database, @NotNull String id, @Nullable Object object) {
        return set(database, id, object, true);
    }

    @Nullable
    public synchronized Object set(@NotNull String database, @NotNull String id, @Nullable Object object, boolean edit) {
        Map<String, Object> map = data.get(database);
        if (map == null) {
            if (object == null) {
                return null;
            }
            map = new ConcurrentHashMap<>();
            data.put(database, map);
        }
        if (edit) {
            edited.add(database);
        }
        if (object == null) {
            return map.remove(id);
        } else {
            return map.put(id, object);
        }
    }

    public boolean remove(@NotNull String database) {
        return data.remove(database) != null;
    }

    public void clear() {
        for (Map.Entry<String, Map<String, Object>> entry : data.entrySet()) {
            entry.getValue().clear();
        }
        data.clear();
    }
}
