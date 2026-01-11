package com.saicone.savedata.core.delivery;

import com.saicone.delivery4j.AbstractMessenger;
import com.saicone.delivery4j.Broker;
import com.saicone.delivery4j.broker.HikariBroker;
import com.saicone.delivery4j.util.LogFilter;
import com.saicone.mcode.module.task.Task;
import com.saicone.mcode.module.task.TaskExecutor;
import com.saicone.savedata.SaveData;
import com.saicone.savedata.module.data.client.HikariClient;
import com.saicone.settings.node.MapNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.Executor;
import java.util.function.Consumer;

public class Messenger extends AbstractMessenger implements Executor {

    private final HikariClient client;

    private String prefix = "savedata_";
    private String channel = "savedata:main";

    public Messenger(@NotNull HikariClient client) {
        this.client = client;
    }

    public void onLoad(@NotNull MapNode config) {
        close();
        this.prefix = config.getRegex("(?i)(table-?)?prefix").asString("savedata_");
        this.channel = config.getIgnoreCase("channel").asString("savedata:main");
    }

    public void onStart() {
        start();
    }

    public void onClose() {
        close();
        clear();
    }

    @NotNull
    public String getPrefix() {
        return prefix;
    }

    @NotNull
    public String getChannel() {
        return channel;
    }

    public void subscribe(@NotNull Consumer<String[]> incomingConsumer) {
        subscribe(this.channel).consume((channel, lines) -> incomingConsumer.accept(lines));
    }

    public void sendAny(@Nullable Object... lines) {
        send(this.channel, lines);
    }

    @Override
    protected @NotNull Broker loadBroker() {
        final HikariBroker broker = new HikariBroker(this.client.getHikari());
        broker.setTablePrefix(this.prefix);
        broker.setExecutor(new TaskExecutor());
        broker.setLogger(LogFilter.valueOf(SaveData.bootstrap().logger(), () -> SaveData.get().getLogLevel()));
        return broker;
    }

    @Override
    public void execute(@NotNull Runnable command) {
        Task.async(command);
    }
}
