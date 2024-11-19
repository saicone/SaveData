package com.saicone.savedata.api.top;

import com.saicone.mcode.module.task.Task;
import com.saicone.savedata.SaveData;
import com.saicone.savedata.api.data.type.NumberDataType;
import com.saicone.savedata.module.data.DataClient;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class TopEntry<T extends Number> {

    private final NumberDataType<T> type;
    private final long limit;
    private final long update;
    private final boolean indexMapping;
    private final boolean undefinedPosition;

    private Map<UUID, T> data = new HashMap<>();
    private List<UUID> sorted = new ArrayList<>();
    private Map<UUID, Integer> indexes = new HashMap<>();

    private transient long lastUpdate = -1;
    private transient final Map<UUID, Object> toUpdate = new HashMap<>();

    public TopEntry(@NotNull NumberDataType<T> type, long limit, long update, boolean indexMapping, boolean undefinedPosition) {
        this.type = type;
        this.limit = limit;
        this.update = update;
        this.indexMapping = indexMapping;
        this.undefinedPosition = undefinedPosition;
    }

    @NotNull
    public NumberDataType<T> getType() {
        return type;
    }

    public long getLimit() {
        return limit;
    }

    public long getUpdate() {
        return update;
    }

    public int getUndefined() {
        return this.undefinedPosition ? -1 : this.sorted.size() + 1;
    }

    @NotNull
    public List<UUID> getSorted() {
        return sorted;
    }

    @NotNull
    public Map<UUID, Integer> getIndexes() {
        return indexes;
    }

    public boolean isMappingIndexes() {
        return indexMapping;
    }

    public boolean useUndefinedPosition() {
        return undefinedPosition;
    }

    @Nullable
    public UUID get(int position) {
        if (position < 1 || position > this.sorted.size()) {
            return null;
        }
        return this.sorted.get(position - 1);
    }

    public int get(@NotNull UUID user) {
        if (this.indexMapping) {
            final Integer index = this.indexes.get(user);
            return index != null ? index + 1 : getUndefined();
        } else {
            final int index = this.sorted.indexOf(user);
            return index >= 0 ? index + 1 : getUndefined();
        }
    }

    @Nullable
    public T value(int position) {
        final UUID user = get(position);
        if (user == null) {
            return null;
        }
        return value(user);
    }

    @Nullable
    public T value(@NotNull UUID user) {
        return this.data.getOrDefault(user, this.type.getDefaultValue());
    }

    @Nullable
    public Object formatted(int position) {
        return formatted(get(position));
    }

    @Nullable
    public Object formatted(@Nullable UUID user) {
        final T value;
        if (user == null) {
            value = this.type.getDefaultValue();
        } else {
            value = this.data.getOrDefault(user, this.type.getDefaultValue());
        }
        if (value == null) {
            return null;
        }
        return this.type.eval(value);
    }

    public void update(@NotNull UUID user, @Nullable Object value) {
        if (value == null) {
            this.data.remove(user);
            this.sorted.remove(user);
            if (this.indexMapping) {
                this.indexes.remove(user);
            }
            this.toUpdate.remove(user);
        } else {
            // Delayed updates
            if (this.update > 0) {
                this.toUpdate.put(user, value);
                final long time = System.currentTimeMillis();
                if (this.lastUpdate > time) {
                    return;
                }
                this.lastUpdate = time + this.update;
                Task.runAsync(this::updateAll);
            } else {
                updateUser(user, value);
            }
        }
    }

    private synchronized void updateAll() {
        if (this.toUpdate.isEmpty()) {
            return;
        }
        for (UUID user : new HashSet<>(this.toUpdate.keySet())) {
            final Object value = this.toUpdate.remove(user);
            if (value == null) {
                continue;
            }
            updateUser(user, value);
        }
    }

    private void updateUser(@NotNull UUID user, @NotNull Object value) {
        final T parsedValue;
        try {
            parsedValue = this.type.load(value);
        } catch (Throwable t) {
            SaveData.logException(2, t, "Cannot update top entry for user '" + user + "' due the value '" + value + "' cannot be parsed as " + this.type.getTypeName());
            return;
        }
        this.data.put(user, parsedValue);

        // Update index
        this.sorted.remove(user);
        int index = Collections.binarySearch(this.sorted, user, Comparator.comparing(this.data::get, this.type.reversed()));
        if (index < 0) {
            index = -index - 1;
        }
        this.sorted.add(index, user);

        if (this.indexMapping) {
            this.indexes.put(user, index);
        }
    }

    @ApiStatus.Internal
    public void update(@NotNull DataClient client, @NotNull String key) {
        update(client.loadTopEntry(key, this.type));
    }

    public void update(@NotNull Map<UUID, T> data) {
        if (data.isEmpty()) {
            this.data = new HashMap<>();
            this.sorted = new ArrayList<>();
            this.indexes = new HashMap<>();
            return;
        }
        var stream = data.entrySet().stream().sorted(Map.Entry.comparingByValue(this.type.reversed()));
        if (this.limit > 0) {
            stream = stream.limit(this.limit);
        }
        final List<UUID> sorted = stream.map(Map.Entry::getKey).collect(Collectors.toList());
        if (this.indexMapping) {
            final Map<UUID, Integer> map = new HashMap<>();
            for (int i = 0; i < sorted.size(); i++) {
                map.put(sorted.get(i), i);
            }
            this.indexes = map;
        }
        this.data = data;
        this.sorted = sorted;
    }

    public void clear() {
        this.data.clear();
        this.sorted.clear();
        this.indexes.clear();
        this.toUpdate.clear();
    }
}
