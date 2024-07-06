package com.saicone.savedata.module.data;

import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public interface DataClient {

    void start(@NotNull String... tables);

    void close();

    void save(@NotNull String table, @NotNull String id, @NotNull Map<String, Object> data);

    void delete(@NotNull String table, @NotNull String id);

    void load(@NotNull String table, @NotNull String id, @NotNull Consumer<Map<String, Object>> consumer);

    void loadAll(@NotNull String table, @NotNull BiConsumer<String, Map<String, Object>> consumer);
}
