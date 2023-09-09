/*
 * This file is part of mcode, licensed under the MIT License
 *
 * Copyright (c) Rubenicos
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.saicone.savedata.module.delivery;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public abstract class AbstractMessenger {

    private static final boolean USE_CACHE_SET;

    static {
        boolean bool = false;
        try {
            Class.forName("com.saicone.mcode.util.CacheSet");
            bool = true;
        } catch (ClassNotFoundException ignored) { }
        USE_CACHE_SET = bool;
    }

    protected DeliveryClient deliveryClient;
    protected final Map<String, Set<Consumer<String>>> incomingConsumers = new HashMap<>();
    protected final Cache<Integer, Boolean> cachedIds = CacheBuilder.newBuilder().expireAfterWrite(10, TimeUnit.SECONDS).build();

    @Nullable
    public DeliveryClient getDeliveryClient() {
        return deliveryClient;
    }

    @NotNull
    public Set<String> getSubscribedChannels() {
        return incomingConsumers.keySet();
    }

    @NotNull
    public Map<String, Set<Consumer<String>>> getIncomingConsumers() {
        return incomingConsumers;
    }

    @NotNull
    protected abstract DeliveryClient loadDeliveryClient();

    public void start() {
        start(loadDeliveryClient());
    }

    public void start(@NotNull DeliveryClient deliveryClient) {
        close();

        deliveryClient.getSubscribedChannels().addAll(getSubscribedChannels());
        deliveryClient.setConsumer(this::receive);
        deliveryClient.setLogConsumer(this::log);

        this.deliveryClient = deliveryClient;
        this.deliveryClient.start();
    }

    public void close() {
        if (deliveryClient != null) {
            deliveryClient.close();
        }
    }

    public void clear() {
        if (deliveryClient != null) {
            deliveryClient.clear();
        }
        incomingConsumers.clear();
        cacheClear();
    }

    public void subscribe(@NotNull String channel, @NotNull Consumer<String> incomingConsumer) {
        if (!incomingConsumers.containsKey(channel)) {
            incomingConsumers.put(channel, new HashSet<>());
        }
        incomingConsumers.get(channel).add(incomingConsumer);
        if (deliveryClient != null) {
            deliveryClient.subscribe(channel);
        }
    }

    public boolean send(@NotNull String channel, @NotNull String message) {
        if (deliveryClient == null) {
            return false;
        }

        final int id = cacheId();
        cacheAdd(id);

        final ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeInt(id);
        out.writeUTF(message);
        deliveryClient.send(channel, out.toByteArray());
        return true;
    }

    public boolean receive(@NotNull String channel, byte[] bytes) {
        final Set<Consumer<String>> consumers = incomingConsumers.get(channel);
        if (consumers == null || consumers.isEmpty()) {
            return false;
        }

        final ByteArrayDataInput in = ByteStreams.newDataInput(bytes);
        try {
            if (cacheContains(in.readInt())) {
                return false;
            }

            final String message = in.readUTF();
            for (Consumer<String> consumer : consumers) {
                consumer.accept(message);
            }
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    protected void log(int level, @NotNull String msg) {
    }

    protected int cacheId() {
        return ThreadLocalRandom.current().nextInt(0, 999999 + 1);
    }

    protected void cacheAdd(int id) {
        cachedIds.put(id, true);
    }

    protected boolean cacheContains(int id) {
        return cachedIds.getIfPresent(id) != null;
    }

    protected void cacheClear() {
        cachedIds.invalidateAll();
    }
}
