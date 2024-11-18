package com.saicone.savedata.core.data;

import com.saicone.savedata.SaveData;
import com.saicone.savedata.api.SaveDataAPI;
import com.saicone.savedata.api.data.DataEntry;
import com.saicone.savedata.api.data.DataType;
import com.saicone.savedata.api.data.type.NumberDataType;
import com.saicone.savedata.api.top.TopEntry;
import com.saicone.savedata.core.delivery.Messenger;
import com.saicone.savedata.module.data.DataClient;
import com.saicone.savedata.module.data.client.FileClient;
import com.saicone.savedata.module.data.client.HikariClient;
import com.saicone.savedata.util.DurationFormatter;
import com.saicone.settings.SettingsNode;
import com.saicone.settings.node.MapNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class Database {

    private final String type;
    private final DataClient client;

    private final Map<String, TopEntry<?>> tops = new HashMap<>();
    private Messenger messenger;

    public Database(@NotNull String databaseName, @NotNull String type) {
        this.type = type;
        if (type.equalsIgnoreCase("FILE")) {
            this.client = new FileClient(databaseName, SaveData.get().getFolder());
        } else if (type.equalsIgnoreCase("SQL")) {
            this.client = new HikariClient(databaseName);
        } else {
            throw new IllegalArgumentException("The database type " + type + " doesn't exist");
        }
    }

    public void onLoad(@NotNull MapNode config) {
        final SettingsNode clientConfig = config.getIgnoreCase(this.type);
        if (clientConfig.isMap()) {
            this.client.onLoad(clientConfig.asMapNode());
        } else {
            this.client.onLoad(new MapNode());
        }
        final SettingsNode top = config.getRegex("(?i)tops?");
        if (top.isList()) {
            for (SettingsNode node : top.asListNode()) {
                final String key;
                final DataType<?> type;
                final long limit;
                final long update;
                final boolean indexMapping;
                final boolean undefinedPosition;
                if (node.isObject()) {
                    key = node.asString("");
                    type = SaveDataAPI.getDataType(key);
                    limit = -1;
                    update = 0;
                    indexMapping = false;
                    undefinedPosition = true;
                } else if (!node.isMap()) {
                    continue;
                } else {
                    final MapNode map = node.asMapNode();
                    key = map.getIgnoreCase("type").asString("");
                    type = SaveDataAPI.getDataType(key);
                    limit = map.getIgnoreCase("limit").asLong(-1L);
                    update = DurationFormatter.format(map.getIgnoreCase("update").asString("0"), TimeUnit.MILLISECONDS);
                    indexMapping = map.getRegex("(?i)index-?mapping").asBoolean(false);
                    undefinedPosition = map.getRegex("(?i)undefined-?position").asBoolean(true);
                }
                if (!(type instanceof NumberDataType)) {
                    SaveData.log(2, "Cannot create a top for non-number data type '" + key + "'");
                    continue;
                }
                final TopEntry<? extends Number> entry = new TopEntry<>((NumberDataType<? extends Number>) type, limit, update, indexMapping, undefinedPosition);
                entry.update(this.client, key);
                this.tops.put(key, entry);
            }
        }
        if (this.client instanceof HikariClient) {
            final SettingsNode messengerConfig = config.getIgnoreCase("messenger");
            if (messengerConfig.isMap() && messengerConfig.asMapNode().getIgnoreCase("enabled").asBoolean(false)) {
                this.messenger = new Messenger((HikariClient) this.client);
                this.messenger.onLoad(messengerConfig.asMapNode());
            }
        }
    }

    public void onEnable() {
        this.client.onStart();
        if (this.messenger != null) {
            this.messenger.onStart();
        }
    }

    public void onDisable() {
        if (this.messenger != null) {
            this.messenger.onClose();
        }
        this.client.onClose();
    }

    @NotNull
    public String getName() {
        return client.getDatabaseName();
    }

    @NotNull
    public String getType() {
        return type;
    }

    @NotNull
    public DataClient getClient() {
        return client;
    }

    @Nullable
    public TopEntry<?> getTop(@NotNull String key) {
        return tops.get(key);
    }

    @NotNull
    public Map<String, TopEntry<?>> getTops() {
        return tops;
    }

    @Nullable
    public Messenger getMessenger() {
        return messenger;
    }

    public void saveDataEntry(@NotNull UUID user, @NotNull DataEntry<?> entry) {
        client.saveDataEntry(user, entry);
        final TopEntry<?> top = getTop(entry.getType().getId());
        if (top != null) {
            top.update(user, entry.getValue());
        }
        if (messenger != null) {
            // Ugly top compatibility
            if (entry.getType() instanceof NumberDataType) {
                messenger.sendAny(user.toString(), entry.getType().getId(), entry.getSavedValue());
            } else {
                messenger.sendAny(user.toString(), entry.getType().getId());
            }
        }
    }
}
