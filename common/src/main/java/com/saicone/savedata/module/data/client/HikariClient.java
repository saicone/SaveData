package com.saicone.savedata.module.data.client;

import com.saicone.ezlib.EzlibLoader;
import com.saicone.mcode.util.Strings;
import com.saicone.savedata.SaveData;
import com.saicone.savedata.api.data.DataEntry;
import com.saicone.savedata.api.data.DataNode;
import com.saicone.savedata.api.data.DataType;
import com.saicone.savedata.api.data.DataUser;
import com.saicone.savedata.module.data.DataClient;
import com.saicone.savedata.module.data.sql.SqlSchema;
import com.saicone.savedata.module.data.sql.SqlType;
import com.saicone.settings.node.MapNode;
import com.saicone.types.TypeParser;
import com.saicone.types.Types;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public class HikariClient implements DataClient {

    private static final SqlSchema SCHEMA = new SqlSchema(SqlType.MYSQL);
    private static final Map<String, ColumnFunction<?>> COLUMNS = Map.of(
            "id", ColumnFunction.of(Types.INTEGER, PreparedStatement::setInt),
            "user", ColumnFunction.of(Types.STRING, PreparedStatement::setString),
            "type", ColumnFunction.of(Types.STRING, PreparedStatement::setString),
            "key", ColumnFunction.of(Types.STRING, PreparedStatement::setString),
            "value", ColumnFunction.of(Types.STRING, PreparedStatement::setString),
            "expiration", ColumnFunction.of(Types.LONG, PreparedStatement::setLong)
    );

    private final SqlSchema schema;
    private final String databaseName;

    private SqlType type;
    private String tableName;
    private HikariConfig hikariConfig;
    private HikariDataSource hikari;

    public HikariClient(@NotNull String databaseName) {
        this(SCHEMA, databaseName);
    }

    public HikariClient(@NotNull SqlSchema schema, @NotNull String databaseName) {
        this.schema = schema;
        this.databaseName = databaseName;
    }

    @Override
    public void onLoad(@NotNull MapNode config) {
        this.type = null;
        final String type = config.getIgnoreCase("type").asString("mysql");
        this.type = SqlType.of(type, null);

        this.tableName = config.getRegex("(?i)table-?(name)?").asString("data");

        if (this.type == null) {
            SaveData.log(1, "Cannot initialize SQL database, the sql type '" + type + "' doesn't exists");
            this.hikariConfig = null;
            return;
        }

        if (!SCHEMA.isLoaded()) {
            try {
                SCHEMA.load("com/saicone/savedata/module/data/schema");
                SaveData.log(4, "Sql schema load queries for " + SCHEMA.getQueries().size() + " sql types: " + SCHEMA.getQueries().keySet().stream().map(Enum::name).collect(Collectors.joining(", ")));
            } catch (IOException e) {
                SaveData.logException(1, e, "Cannot load SQL schema");
            }
        }
        if (SCHEMA.getQueries().containsKey(this.type)) {
            SaveData.log(4, "Using sql type '" + this.type.name() + "' with queries: " + String.join(", ", SCHEMA.getQueries().get(this.type).keySet()));
        }

        this.hikariConfig = new HikariConfig();
        if (!this.type.isDependencyPresent()) {
            SaveData.bootstrap().getLibraryLoader().applyDependency(new EzlibLoader.Dependency().path(this.type.getDependency()).relocate(this.type.getRelocations()));
        }
        this.hikariConfig.setDriverClassName(this.type.getDriver());

        if (this.type.isExternal()) {
            final String host = config.getIgnoreCase("host").asString("localhost");
            final int port = config.getIgnoreCase("port").asInt(3306);
            final String database = config.getRegex("(?i)(db|database)(-?name)?").asString("database");
            final String[] flags = config.getRegex("(?i)flags?|propert(y|ies)").asArray(Types.STRING);

            this.hikariConfig.setJdbcUrl(this.type.getUrl(host, port, database, flags));
            this.hikariConfig.setUsername(config.getRegex("(?i)user(-?name)?").asString("root"));
            this.hikariConfig.setPassword(config.getIgnoreCase("password").asString("password"));
        } else {
            final String strPath = config.getIgnoreCase("path").asString(SaveData.get().getFolder() + "/database/" + this.databaseName + "-" + this.type.name().toLowerCase());
            final Path path = Path.of(strPath);
            if (strPath.contains("/") && !Files.exists(path.getParent())) {
                try {
                    Files.createDirectories(path.getParent());
                } catch (IOException e) {
                    SaveData.logException(1, e, "Cannot create database parent directory");
                }
            }

            this.hikariConfig.setJdbcUrl(this.type.getUrl(strPath));
        }
    }

    @Override
    public void onStart() {
        if (hikariConfig == null) {
            return;
        }
        hikari = new HikariDataSource(hikariConfig);
        connect(con -> {
            // This table creation process was taken from LuckPerms
            if (isTablePresent(con, tableName)) {
                return;
            }
            SaveData.log(43, "The table '" + tableName + "' doesn't exist, so will be created");

            final List<String> list = schema.getList(type, "create:data_table", "{table_name}", tableName);
            boolean fail = false;

            try (Statement stmt = con.createStatement()) {
                for (String sql : list) {
                    stmt.addBatch(Strings.replaceArgs(sql, "utf8mb4"));
                }

                try {
                    stmt.executeBatch();
                } catch (BatchUpdateException e) {
                    if (e.getMessage().contains("Unknown character set")) {
                        fail = true;
                    } else {
                        throw e;
                    }
                }
            }

            if (fail) {
                try (Statement stmt = con.createStatement()) {
                    for (String sql : list) {
                        stmt.addBatch(Strings.replaceArgs(sql, "utf8"));
                    }

                    stmt.executeBatch();
                }
            }

            // Migrate old data
            final Map<String, DataNode> nodes = new HashMap<>();
            if (isTablePresent(con, "global_data")) {
                SaveData.log(3, "Detected old global data, loading it...");

                String sql = "SELECT ALL `data` FROM `global_data`";
                if (this.type == SqlType.POSTGRESQL) {
                    sql = sql.replace('`', '"');
                }
                DataNode node = null;
                try (PreparedStatement stmt = con.prepareStatement(sql)) {
                    final ResultSet result = stmt.executeQuery();
                    while (result.next()) {
                        final String data = result.getString("data");
                        final DataNode loaded = DataNode.of(this.databaseName, data);
                        if (node == null) {
                            node = loaded;
                        } else {
                            node.putAll(loaded);
                        }
                    }
                }
                if (node != null && !node.isEmpty()) {
                    nodes.put(DataUser.SERVER_ID.toString(), node);
                }
            }
            if (isTablePresent(con, "players_data")) {
                SaveData.log(3, "Detected old player data, loading it...");

                String sql = "SELECT ALL `id`, `data` FROM `players_data`";
                if (this.type == SqlType.POSTGRESQL) {
                    sql = sql.replace('`', '"');
                }
                try (PreparedStatement stmt = con.prepareStatement(sql)) {
                    final ResultSet result = stmt.executeQuery();
                    while (result.next()) {
                        final String user = result.getString("id");
                        final String data = result.getString("data");
                        final DataNode node = DataNode.of(this.databaseName, data);
                        if (!node.isEmpty()) {
                            nodes.put(user, node);
                        }
                    }
                }
            }
            if (!nodes.isEmpty()) {
                SaveData.log(3, "Updating old data into new format...");

                try (PreparedStatement insert = con.prepareStatement(getInsertStatement())) {
                    int count = 0;
                    for (Map.Entry<String, DataNode> userEntry : nodes.entrySet()) {
                        for (Map.Entry<String, DataEntry<?>> nodeEntry : userEntry.getValue().entrySet()) {
                            if (count > 0 && count % 1000 == 0) {
                                SaveData.log(3, "Updating " + count + " data entries...");
                            }

                            final DataEntry<?> entry = nodeEntry.getValue();
                            insert.setString(1, userEntry.getKey());
                            insert.setString(2, entry.getType().getTypeName());
                            insert.setString(3, entry.getType().getId());
                            insert.setString(4, entry.getSavedValue());
                            insert.setNull(5, java.sql.Types.BIGINT);
                            insert.addBatch();

                            count++;
                        }
                    }

                    SaveData.log(3, "Updating " + count + " data entries so far...");
                    insert.executeBatch();
                }
            }
        });
    }

    @Override
    public void onClose() {
        this.hikari.close();
    }

    @NotNull
    @Override
    public String getDatabaseName() {
        return databaseName;
    }

    @NotNull
    @Override
    public SqlType getType() {
        return type;
    }

    @NotNull
    public HikariDataSource getHikari() {
        return hikari;
    }

    @NotNull
    public String getSelectStatement() {
        return this.schema.getSelect(this.type, "select:data", List.of("*"), "{table_name}", tableName);
    }

    @NotNull
    public String getSelectEntryStatement() {
        return this.schema.getSelect(this.type, "select:data_entry", List.of("*"), "{table_name}", tableName);
    }

    @NotNull
    public String getInsertStatement() {
        return this.schema.get(this.type, "insert:data", "{table_name}", tableName);
    }

    @NotNull
    public String getUpdateStatement() {
        return this.schema.getUpdate(this.type, "update:data", List.of("type", "key", "value", "expiration"), "{table_name}", tableName);
    }

    @NotNull
    public String getDeleteStatement() {
        return this.schema.getDelete(this.type, "delete:data", List.of("id"), "{table_name}", tableName);
    }

    @Override
    public @NotNull DataNode loadData(@NotNull UUID user, @NotNull Function<String, DataType<Object>> dataProvider) {
        return connect(con -> {
            final long time = System.currentTimeMillis();
            final DataNode node = new DataNode(this.databaseName);
            final Set<Integer> toDelete = new HashSet<>();
            try (PreparedStatement stmt = con.prepareStatement(getSelectStatement())) {
                stmt.setString(1, user.toString());
                final ResultSet result = stmt.executeQuery();
                while (result.next()) {
                    final int id = result.getInt("id");
                    final String type = result.getString("type");
                    final String key = result.getString("key");
                    if (node.containsKey(key)) {
                        SaveData.log(2, "Found duplicated data type '" + key + "' for user " + user, ", deleting it...");
                        toDelete.add(id);
                        continue;
                    }
                    final DataType<Object> dataType = dataProvider.apply(key);
                    if (dataType == null) {
                        SaveData.log(2, "Found invalid data type '" + key + "' for user " + user, ", deleting it...");
                        toDelete.add(id);
                        continue;
                    }
                    final String value = result.getString("value");
                    final long expiration = result.getLong("expiration");
                    if (expiration > 0 && time >= expiration) {
                        toDelete.add(id);
                        continue;
                    }
                    final Object parsedValue;
                    try {
                        parsedValue = dataType.load(value);
                    } catch (Throwable t) {
                        SaveData.log(2, () -> "Cannot parse value '" + value + "' with data type " + type + " as " +  dataType.getTypeName() + " for user " + user + ", deleting it...");
                        toDelete.add(id);
                        continue;
                    }
                    node.put(key, new DataEntry<>(id, dataType, parsedValue, expiration));
                }
            }
            if (!toDelete.isEmpty()) {
                deleteData(con, toDelete);
            }
            return node;
        });
    }

    @Override
    public @Nullable <T> DataEntry<T> loadDataEntry(@NotNull UUID user, @NotNull String key, @NotNull DataType<T> dataType) {
        return connect(con -> {
            final long time = System.currentTimeMillis();
            DataEntry<T> entry = null;
            final Set<Integer> toDelete = new HashSet<>();
            try (PreparedStatement stmt = con.prepareStatement(getSelectEntryStatement())) {
                stmt.setString(1, user.toString());
                stmt.setString(2, key);
                final ResultSet result = stmt.executeQuery();
                while (result.next()) {
                    final int id = result.getInt("id");
                    if (entry != null) {
                        SaveData.log(2, "Found duplicated data type '" + key + "' for user " + user, ", deleting it...");
                        toDelete.add(id);
                        continue;
                    }
                    final String type = result.getString("type");
                    final String value = result.getString("value");
                    final long expiration = result.getLong("expiration");
                    if (expiration > 0 && time >= expiration) {
                        toDelete.add(id);
                        continue;
                    }
                    final T parsedValue;
                    try {
                        parsedValue = dataType.load(value);
                    } catch (Throwable t) {
                        SaveData.log(2, () -> "Cannot parse value '" + value + "' with data type " + type + " as " +  dataType.getTypeName() + " for user " + user + ", deleting it...");
                        toDelete.add(id);
                        continue;
                    }
                    entry = new DataEntry<>(id, dataType, parsedValue, expiration);
                }
            }
            if (!toDelete.isEmpty()) {
                deleteData(con, toDelete);
            }
            return entry;
        });
    }

    @Override
    public void saveData(@NotNull UUID user, @NotNull DataNode node) {
        if (node.isEmpty()) {
            return;
        }
        final List<DataEntry<?>> toInsert = new ArrayList<>();
        final List<DataEntry<?>> toUpdate = new ArrayList<>();
        final Set<Integer> toDelete = new HashSet<>();
        for (Map.Entry<String, DataEntry<?>> e : node.entrySet()) {
            final DataEntry<?> entry = e.getValue();
            if (entry.getValue() == null) {
                if (entry.isSaved()) {
                    toDelete.add(entry.getId());
                }
                continue;
            }
            if (!entry.isEdited()) {
                continue;
            }
            if (entry.isSaved()) {
                toUpdate.add(entry);
            } else {
                toInsert.add(entry);
            }
        }
        if (toInsert.isEmpty() && toUpdate.isEmpty() && toDelete.isEmpty()) {
            return;
        }
        final String uniqueId = user.toString();
        connect(con -> {
            if (!toInsert.isEmpty()) {
                try (PreparedStatement insert = con.prepareStatement(getInsertStatement())) {
                    for (DataEntry<?> entry : toInsert) {
                        insert.setString(1, uniqueId);
                        insert.setString(2, entry.getType().getTypeName());
                        insert.setString(3, entry.getType().getId());
                        insert.setString(4, entry.getSavedValue());
                        if (entry.isTemporary()) {
                            insert.setLong(5, entry.getExpiration());
                        } else {
                            insert.setNull(5, java.sql.Types.BIGINT);
                        }
                        insert.addBatch();
                    }
                    insert.executeBatch();
                }
            }
            if (!toUpdate.isEmpty()) {
                try (PreparedStatement update = con.prepareStatement(getUpdateStatement())) {
                    for (DataEntry<?> entry : toInsert) {
                        update.setString(1, entry.getType().getTypeName());
                        update.setString(2, entry.getType().getId());
                        update.setString(3, entry.getSavedValue());
                        if (entry.isTemporary()) {
                            update.setLong(4, entry.getExpiration());
                        } else {
                            update.setNull(4, java.sql.Types.BIGINT);
                        }
                        update.setInt(5, entry.getId());
                        update.addBatch();
                    }
                    update.executeBatch();
                }
            }
            if (!toDelete.isEmpty()) {
                deleteData(con, toDelete);
            }
        });
    }

    @Override
    public void saveDataEntry(@NotNull UUID user, @NotNull DataEntry<?> entry) {
        connect(con -> {
            if (entry.getValue() == null) {
                if (entry.isSaved()) {
                    try (PreparedStatement delete = con.prepareStatement(getDeleteStatement())) {
                        delete.setInt(1, entry.getId());
                        delete.execute();
                    }
                }
            } else if (entry.isSaved()) {
                try (PreparedStatement update = con.prepareStatement(getUpdateStatement())) {
                    update.setString(1, entry.getType().getTypeName());
                    update.setString(2, entry.getType().getId());
                    update.setString(3, entry.getSavedValue());
                    if (entry.isTemporary()) {
                        update.setLong(4, entry.getExpiration());
                    } else {
                        update.setNull(4, java.sql.Types.BIGINT);
                    }
                    update.setInt(5, entry.getId());
                    update.execute();
                }
            } else {
                try (PreparedStatement insert = con.prepareStatement(getInsertStatement())) {
                    insert.setString(1, user.toString());
                    insert.setString(2, entry.getType().getTypeName());
                    insert.setString(3, entry.getType().getId());
                    insert.setString(4, entry.getSavedValue());
                    if (entry.isTemporary()) {
                        insert.setLong(5, entry.getExpiration());
                    } else {
                        insert.setNull(5, java.sql.Types.BIGINT);
                    }
                    final int rows = insert.executeUpdate();
                    if (rows > 0) {
                        final ResultSet result = insert.getGeneratedKeys();
                        if (result.next()) {
                            entry.setId(result.getInt(1));
                        }
                    }
                }
            }
        });
    }

    @Override
    public void deleteData(@NotNull Map<String, Object> columns) {
        final List<String> keys = columns.keySet().stream().filter(key -> {
            if (COLUMNS.containsKey(key)) {
                return true;
            } else {
                SaveData.log(2, "Trying to delete data with invalid column name: '" + key + "'");
                return false;
            }
        }).collect(Collectors.toList());

        if (keys.isEmpty()) {
            return;
        }
        connect(con -> {
            try (PreparedStatement delete = con.prepareStatement(this.schema.getDelete(this.type, "delete:data", keys, "{table_name}", tableName))) {
                int index = 1;
                for (String key : keys) {
                    COLUMNS.get(key).setAny(delete, index, columns.get(key));
                    index++;
                }
                delete.execute();
            }
        });
    }

    private void deleteData(@NotNull Connection con, @NotNull Collection<Integer> entries) throws SQLException {
        try (PreparedStatement delete = con.prepareStatement(getDeleteStatement())) {
            for (Integer id : entries) {
                delete.setInt(1, id);
                delete.addBatch();
            }
            delete.executeBatch();
        }
    }

    private static boolean isTablePresent(@NotNull Connection con, @NotNull String tableName) throws SQLException {
        try (ResultSet set = con.getMetaData().getTables(con.getCatalog(), null, "%", null)) {
            while (set.next()) {
                if (set.getString(3).equalsIgnoreCase(tableName)) {
                    return true;
                }
            }
        }
        return false;
    }

    public void connect(@NotNull SqlConsumer consumer) {
        if (hikari == null || hikari.isClosed()) {
            return;
        }

        try (Connection connection = hikari.getConnection()) {
            consumer.accept(connection);
        } catch (SQLException e) {
            SaveData.logException(2, e);
        }
    }

    public <R> R connect(@NotNull SqlFunction<R> consumer) {
        if (hikari == null || hikari.isClosed()) {
            return null;
        }

        try (Connection connection = hikari.getConnection()) {
            return consumer.apply(connection);
        } catch (SQLException e) {
            SaveData.logException(2, e);
        }
        return null;
    }

    @FunctionalInterface
    public interface SqlConsumer {
        void accept(@NotNull Connection connection) throws SQLException;
    }

    @FunctionalInterface
    public interface SqlFunction<R> {
        @Nullable
        R apply(@NotNull Connection connection) throws SQLException;
    }

    @FunctionalInterface
    private interface ColumnFunction<T> {
        @NotNull
        static <T> ColumnFunction<T> of(@NotNull TypeParser<T> parser, @NotNull ColumnFunction<T> function) {
            return new ColumnFunction<T>() {
                @Override
                public void set(@NotNull PreparedStatement statement, int index, T value) throws SQLException {
                    function.set(statement, index, value);
                }

                @Override
                public void setAny(@NotNull PreparedStatement statement, int index, Object value) throws SQLException {
                    function.set(statement, index, parser.parse(value));
                }
            };
        }

        void set(@NotNull PreparedStatement statement, int index, T value) throws SQLException;

        default void setAny(@NotNull PreparedStatement statement, int index, Object value) throws SQLException {
            // empty default method
        }
    }
}
