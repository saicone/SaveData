package com.saicone.savedata.core.data;

import com.saicone.savedata.SaveData;
import com.saicone.savedata.api.data.DataEntry;
import com.saicone.savedata.core.delivery.Messenger;
import com.saicone.savedata.module.data.DataClient;
import com.saicone.savedata.module.data.client.FileClient;
import com.saicone.savedata.module.data.client.HikariClient;
import com.saicone.settings.SettingsNode;
import com.saicone.settings.node.MapNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class Database {

    private final String type;
    private final DataClient client;

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
    public Messenger getMessenger() {
        return messenger;
    }

    public void saveDataEntry(@NotNull UUID user, @NotNull DataEntry<?> entry) {
        client.saveDataEntry(user, entry);
        if (messenger != null) {
            messenger.sendAny(user.toString(), entry.getType().getId());
        }
    }
}
