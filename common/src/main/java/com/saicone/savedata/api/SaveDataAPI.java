package com.saicone.savedata.api;

import com.saicone.savedata.SaveData;
import com.saicone.savedata.api.data.DataOperator;
import com.saicone.savedata.api.data.DataType;
import com.saicone.savedata.api.data.DataUser;
import com.saicone.savedata.core.data.Database;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class SaveDataAPI {

    SaveDataAPI() {
    }

    @NotNull
    public static Map<String, Database> getDatabases() {
        return Collections.unmodifiableMap(SaveData.get().getDataCore().getDatabases());
    }

    @NotNull
    public static Map<String, DataType<?>> getDataTypes() {
        return Collections.unmodifiableMap(SaveData.get().getDataCore().getDataTypes());
    }

    @NotNull
    public static CompletableFuture<Object> value(@NotNull DataOperator operator, @NotNull String database, @NotNull String dataType, @Nullable Object value) {
        return value(DataUser.SERVER_ID, operator, database, dataType, value, s -> s);
    }

    @NotNull
    public static CompletableFuture<Object> value(@NotNull DataOperator operator, @NotNull String database, @NotNull String dataType, @Nullable Object value, @NotNull Function<String, String> userParser) {
        return value(DataUser.SERVER_ID, operator, database, dataType, value, userParser);
    }

    @NotNull
    public static CompletableFuture<Object> value(@NotNull UUID uniqueId, @NotNull DataOperator operator, @NotNull String database, @NotNull String dataType, @Nullable Object value) {
        return value(uniqueId, operator, database, dataType, value, s -> s);
    }

    @NotNull
    public static CompletableFuture<Object> value(@NotNull UUID uniqueId, @NotNull DataOperator operator, @NotNull String database, @NotNull String dataType, @Nullable Object value, @NotNull Function<String, String> userParser) {
        return SaveData.get().getDataCore().userValue(uniqueId, operator, database, dataType, value, userParser);
    }

    @NotNull
    public static CompletableFuture<Object> update(@NotNull DataOperator operator, @NotNull String database, @NotNull String dataType, @Nullable Object value, @Nullable Long expiration) {
        return update(DataUser.SERVER_ID, operator, database, dataType, value, expiration, s -> s);
    }

    @NotNull
    public static CompletableFuture<Object> update(@NotNull DataOperator operator, @NotNull String database, @NotNull String dataType, @Nullable Object value, @Nullable Long expiration, @NotNull Function<String, String> userParser) {
        return update(DataUser.SERVER_ID, operator, database, dataType, value, expiration, userParser);
    }

    @NotNull
    public static CompletableFuture<Object> update(@NotNull UUID uniqueId, @NotNull DataOperator operator, @NotNull String database, @NotNull String dataType, @Nullable Object value, @Nullable Long expiration) {
        return update(uniqueId, operator, database, dataType, value, expiration, s -> s);
    }

    @NotNull
    public static CompletableFuture<Object> update(@NotNull UUID uniqueId, @NotNull DataOperator operator, @NotNull String database, @NotNull String dataType, @Nullable Object value, @Nullable Long expiration, @NotNull Function<String, String> userParser) {
        return SaveData.get().getDataCore().executeUpdate(uniqueId, operator, database, dataType, value, expiration, userParser);
    }
}
