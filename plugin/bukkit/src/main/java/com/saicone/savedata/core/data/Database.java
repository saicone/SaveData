package com.saicone.savedata.core.data;

import com.saicone.savedata.SaveData;
import com.saicone.savedata.core.data.DataCore;
import com.saicone.savedata.core.delivery.Messenger;
import com.saicone.savedata.module.data.AbstractDatabase;
import com.saicone.savedata.module.data.DataClient;
import com.saicone.savedata.module.data.client.FileData;
import com.saicone.savedata.module.data.client.HikariData;
import com.saicone.savedata.module.delivery.client.HikariDelivery;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.List;

public class Database extends AbstractDatabase {

    private final String id;
    private final String type;
    private final String globalTable;
    private final String playerTable;
    private final ConfigurationSection settings;

    private Messenger messenger;

    public Database(@NotNull String id, @NotNull String type, @NotNull String globalTable, @NotNull String playerTable, @NotNull ConfigurationSection settings) {
        this.id = id;
        this.type = type.trim().toUpperCase();
        this.globalTable = globalTable;
        this.playerTable= playerTable;
        this.settings = settings;
    }

    @NotNull
    public String getId() {
        return id;
    }

    @NotNull
    public String getType() {
        return type;
    }

    @NotNull
    public ConfigurationSection getSettings() {
        return settings;
    }

    @NotNull
    public String getTable(@NotNull DataCore.Type type) {
        return type == DataCore.Type.GLOBAL ? getGlobalTable() : getPlayerTable();
    }

    @NotNull
    public String getGlobalTable() {
        return globalTable;
    }

    @NotNull
    public String getPlayerTable() {
        return playerTable;
    }

    @Nullable
    public Messenger getMessenger() {
        return messenger;
    }

    @Override
    protected @NotNull DataClient loadDataClient() {
        switch (type) {
            case "FILE":
            case "JSON":
            case "YAML":
                return loadFileClient();
            case "SQL":
            case "MYSQL":
            case "MARIADB":
            case "POSTGRESQL":
            case "SQLITE":
            case "H2":
                return loadSqlClient();
            default:
                throw new IllegalArgumentException("The database type '" + type + "' for database '" + id + "' doesn't exists");
        }
    }

    @NotNull
    protected FileData loadFileClient() {
        return new FileData(settings.getString("type", "JSON"), new File(settings.getString("path", "plugins/SaveData/data/file")));
    }

    @NotNull
    protected HikariData loadSqlClient() {
        HikariConfig config = new HikariConfig();
        final String driver = settings.getString("driver");
        if (driver != null) {
            config.setDriverClassName(driver);
        }
        final List<String> flags = settings.getStringList("flags");
        config.setJdbcUrl(settings.getString("url") + (flags.isEmpty() ? "" : "?" + String.join("&", flags)));
        if (settings.contains("username")) {
            config.setUsername(settings.getString("username", "root"));
        }
        if (settings.contains("password")) {
            config.setPassword(settings.getString("password", "password"));
        }
        if (settings.contains("pool-size")) {
            config.setMaximumPoolSize(settings.getInt("pool-size", 10));
        }

        final String create = settings.getString("statement.create", "CREATE TABLE IF NOT EXISTS `<name>` (`id` VARCHAR(255) NOT NULL, `data` TEXT NOT NULL, PRIMARY KEY (`id`)) DEFAULT CHARSET = utf8mb4");
        final String insert = settings.getString("statement.insert", "INSERT INTO `<name>` (`id`, `data`) VALUES(?, ?) ON DUPLICATE KEY UPDATE `data` = VALUES(`data`)");
        final String delete = settings.getString("statement.delete", "DELETE FROM `<name>` WHERE `id` = ?");
        final String select = settings.getString("statement.select", "SELECT * FROM `<name>` WHERE `id` = ?");
        final String selectAll = settings.getString("statement.select-all", "SELECT ALL * FROM <name>");

        final boolean messengerEnabled = settings.getBoolean("messenger.enabled", false);
        final String messengerPrefix = settings.getString("messenger.prefix", "data");

        try {
            final HikariDataSource hikari = new HikariDataSource(config);
            if (messengerEnabled) {
                this.messenger = new Messenger(new HikariDelivery(SaveData.get(), hikari, messengerPrefix));
            }
            return new HikariData(hikari, create, insert, delete, select, selectAll);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    @Override
    public void close() {
        if (messenger != null) {
            messenger.close();
        }
        super.close();
    }
}
