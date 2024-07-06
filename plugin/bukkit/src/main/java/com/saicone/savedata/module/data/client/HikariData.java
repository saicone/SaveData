package com.saicone.savedata.module.data.client;

import com.saicone.savedata.module.data.DataClient;
import com.zaxxer.hikari.HikariDataSource;
import org.jetbrains.annotations.NotNull;

import java.sql.*;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class HikariData implements DataClient {

    private final HikariDataSource hikari;
    private final String create;
    private final String insert;
    private final String delete;
    private final String select;
    private final String selectAll;

    public HikariData(@NotNull HikariDataSource hikari, @NotNull String create, @NotNull String insert, @NotNull String delete, @NotNull String select, @NotNull String selectAll) {
        this.hikari = hikari;
        this.create = create;
        this.insert = insert;
        this.delete = delete;
        this.select = select;
        this.selectAll = selectAll;
    }

    @NotNull
    public HikariDataSource getHikari() {
        return hikari;
    }

    @Override
    public void start(@NotNull String... tables) {
        connect(connection -> {
            try (Statement statement = connection.createStatement()) {
                for (String table : tables) {
                    if (table.isBlank()) {
                        continue;
                    }
                    statement.execute(create.replace("<name>", table));
                }
            }
        });
    }

    @Override
    public void close() {
        hikari.close();
    }

    @Override
    public void save(@NotNull String table, @NotNull String id, @NotNull Map<String, Object> data) {
        connect(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(insert.replace("<name>", table))) {
                statement.setString(1, id);
                statement.setString(2, FileData.GSON.toJson(data));
                statement.execute();
            }
        });
    }

    @Override
    public void delete(@NotNull String table, @NotNull String id) {
        connect(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(delete.replace("<name>", table))) {
                statement.setString(1, id);
                statement.execute();
            }
        });
    }

    @Override
    public void load(@NotNull String table, @NotNull String id, @NotNull Consumer<Map<String, Object>> consumer) {
        connect(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(select.replace("<name>", table))) {
                statement.setString(1, id);
                try (ResultSet rs = statement.executeQuery()) {
                    while (rs.next()) {
                        consumer.accept(FileData.GSON.fromJson(rs.getString("data"), FileData.MAP_TYPE));
                    }
                }
            }
        });
    }

    @Override
    public void loadAll(@NotNull String table, @NotNull BiConsumer<String, Map<String, Object>> consumer) {
        connect(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(selectAll.replace("<name>", table))) {
                try (ResultSet rs = statement.executeQuery()) {
                    while (rs.next()) {
                        consumer.accept(rs.getString("id"), FileData.GSON.fromJson(rs.getString("data"), FileData.MAP_TYPE));
                    }
                }
            }
        });
    }

    private void connect(@NotNull SqlConsumer consumer) {
        try (Connection connection = hikari.getConnection()) {
            consumer.accept(connection);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FunctionalInterface
    public interface SqlConsumer {
        void accept(@NotNull Connection connection) throws SQLException;
    }
}
