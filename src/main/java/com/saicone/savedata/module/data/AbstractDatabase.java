package com.saicone.savedata.module.data;

import org.jetbrains.annotations.NotNull;

public abstract class AbstractDatabase {

    protected DataClient dataClient;

    @NotNull
    public DataClient getDataClient() {
        return dataClient;
    }

    @NotNull
    protected abstract DataClient loadDataClient();

    public void start(@NotNull String... tables) {
        start(loadDataClient(), tables);
    }

    public void start(@NotNull DataClient dataClient, @NotNull String... tables) {
        close();
        this.dataClient = dataClient;
        this.dataClient.start(tables);
    }

    public void close() {
        if (dataClient != null) {
            dataClient.close();
        }
    }
}
