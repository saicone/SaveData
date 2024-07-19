package com.saicone.savedata.api.data;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class DataUser {

    public static final UUID SERVER_ID = new UUID(0, 0);

    private final UUID uniqueId;
    private final ConcurrentMap<String, DataNode> nodes = new ConcurrentHashMap<>();

    public DataUser() {
        this(SERVER_ID);
    }

    public DataUser(@NotNull UUID uniqueId) {
        this.uniqueId = uniqueId;
    }

    public boolean isPlayer() {
        return uniqueId != SERVER_ID;
    }

    public boolean isServer() {
        return uniqueId == SERVER_ID;
    }

    public boolean isEmpty() {
        return nodes.isEmpty();
    }

    @NotNull
    public UUID getUniqueId() {
        return uniqueId;
    }

    @Nullable
    public DataNode getNode(@NotNull String database) {
        return nodes.get(database);
    }

    @NotNull
    public Map<String, DataNode> getNodes() {
        return nodes;
    }

    @Nullable
    public DataEntry<?> getEntry(@NotNull String database, @NotNull String key) {
        final DataNode node = nodes.get(database);
        return node == null ? null : node.get(key);
    }

    @Nullable
    public DataNode setNode(@NotNull String database, @NotNull DataNode node) {
        return nodes.put(database, node);
    }

    @Nullable
    public DataEntry<?> setEntry(@NotNull String database, @NotNull DataEntry<?> entry) {
        if (!nodes.containsKey(database)) {
            nodes.put(database, new DataNode(database));
        }
        return nodes.get(database).put(entry.getType().getId(), entry);
    }

    @Nullable
    public DataNode removeNode(@NotNull String database) {
        return nodes.remove(database);
    }

    @Nullable
    public DataEntry<?> removeEntry(@NotNull String database, @NotNull String key) {
        final DataNode node = nodes.get(database);
        if (node != null) {
            return node.remove(key);
        }
        return null;
    }

    public void clear() {
        for (Map.Entry<String, DataNode> entry : nodes.entrySet()) {
            entry.getValue().clear();
        }
        nodes.clear();
    }
}
