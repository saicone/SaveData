package com.saicone.savedata.core.delivery;

import com.saicone.delivery4j.AbstractMessenger;
import com.saicone.delivery4j.DeliveryClient;
import com.saicone.delivery4j.client.HikariDelivery;
import com.saicone.mcode.scheduler.Task;
import com.saicone.savedata.SaveData;
import com.saicone.savedata.module.data.client.HikariClient;
import com.saicone.settings.node.MapNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class Messenger extends AbstractMessenger {

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
        subscribe(this.channel, incomingConsumer);
    }

    public boolean sendAny(@Nullable Object... lines) {
        return send(this.channel, lines);
    }

    @Override
    protected @NotNull DeliveryClient loadDeliveryClient() {
        return new HikariDelivery(this.client.getHikari(), this.prefix);
    }

    @Override
    public void log(int level, @NotNull Throwable t) {
        SaveData.logException(level, t);
    }

    @Override
    public void log(int level, @NotNull String msg) {
        SaveData.log(level, msg);
    }

    @Override
    public @NotNull Runnable async(@NotNull Runnable runnable) {
        final Object task = Task.runAsync(runnable);
        return () -> Task.stop(task);
    }

    @Override
    public @NotNull Runnable asyncRepeating(@NotNull Runnable runnable, long time, @NotNull TimeUnit unit) {
        final Object task = Task.timerAsync(runnable, time, time, unit);
        return () -> Task.stop(task);
    }
}
