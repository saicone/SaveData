package com.saicone.savedata.module.data;

import com.saicone.savedata.api.data.DataEntry;
import com.saicone.savedata.api.data.DataNode;
import com.saicone.savedata.api.data.DataType;
import com.saicone.settings.node.MapNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

public interface DataClient {

    void onLoad(@NotNull MapNode settings);

    void onStart();

    void onClose();

    @NotNull
    String getDatabaseName();

    @NotNull
    ClientType getType();

    @NotNull
    DataNode loadData(@NotNull UUID user, @NotNull Function<String, DataType<Object>> dataProvider);

    @Nullable
    <T> DataEntry<T> loadDataEntry(@NotNull UUID user, @NotNull String key, @NotNull DataType<T> dataType);

    void saveData(@NotNull UUID user, @NotNull DataNode node);

    void saveDataEntry(@NotNull UUID user, @NotNull DataEntry<?> entry);

    void deleteData(@NotNull Map<String, Object> columns);
}
