package com.saicone.savedata.module.settings;

import com.saicone.savedata.SaveData;
import com.saicone.savedata.module.data.sql.SqlType;
import com.saicone.settings.Settings;
import com.saicone.settings.SettingsData;
import com.saicone.settings.SettingsNode;
import com.saicone.settings.node.MapNode;
import com.saicone.settings.update.NodeUpdate;
import com.saicone.settings.update.SettingsUpdater;
import com.saicone.settings.update.UpdateAction;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

public class DatabaseUpdater extends NodeUpdate {

    public static final SettingsUpdater UPDATER = new SettingsUpdater(List.of(
            NodeUpdate.delete().from("table"),
            NodeUpdate.delete().from("sql", "statement"),
            NodeUpdate.move().from("sql", "messenger").to("messenger"),
            NodeUpdate.custom(parent -> {
                final SettingsNode node = parent.get("sql", "driver");
                final String driver = node.asString();
                if (driver == null) {
                    return false;
                }

                final SqlType type;
                if (driver.endsWith("cj.jdbc.Driver")) {
                    type = SqlType.MYSQL;
                } else if (driver.endsWith("mariadb.jdbc.Driver")) {
                    type = SqlType.MARIADB;
                } else if (driver.endsWith("postgresql.ds.PGSimpleDataSource")) {
                    type = SqlType.POSTGRESQL;
                } else if (driver.endsWith("sqlite.JDBC")) {
                    type = SqlType.SQLITE;
                } else if (driver.endsWith("h2.Driver")) {
                    type = SqlType.H2;
                } else {
                    return false;
                }
                node.delete();

                final SettingsNode urlNode = parent.get("sql", "url").delete();
                if (!type.isExternal()) {
                    return true;
                }

                String url = urlNode.asString();
                if (url == null) {
                    return true;
                }
                url = url.substring(url.indexOf("//") + 2);

                final int point = url.indexOf(':');
                final int slash = url.indexOf('/');

                parent.get("sql", "host").setValue(url.substring(0, point));
                parent.get("sql", "port").setValue(Integer.parseInt(url.substring(point + 1, slash)));
                parent.get("sql", "database").setValue(url.substring(slash + 1));

                return true;
            })
    ));

    public DatabaseUpdater() {
        super(UpdateAction.CUSTOM);
    }

    @Override
    public boolean apply(@NotNull MapNode parent) {
        final SettingsNode databases = parent.getIgnoreCase("database").delete();
        if (!databases.isMap()) {
            return false;
        }
        for (Map.Entry<String, SettingsNode> entry : databases.asMapNode().entrySet()) {
            if (entry.getValue().isMap()) {
                UPDATER.update(entry.getValue().asMapNode(), null);
            }
        }
        final SettingsData<Settings> data = SettingsData.of("databases.yml");
        final Settings databaseConfig = data.load(SaveData.get().getFolder().toFile());
        databaseConfig.merge(databases);
        data.save();
        return true;
    }
}
