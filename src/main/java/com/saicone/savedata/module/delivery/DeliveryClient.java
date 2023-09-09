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

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiConsumer;

public abstract class DeliveryClient {

    protected BiConsumer<String, byte[]> consumer = null;
    protected BiConsumer<Integer, String> logConsumer = null;
    protected final Set<String> subscribedChannels = new HashSet<>();
    protected boolean enabled = false;

    public void onStart() {
    }

    public void onClose() {
    }

    public void onSubscribe(@NotNull String... channels) {
    }

    public void onUnsubscribe(@NotNull String... channels) {
    }

    public void onSend(@NotNull String channel, byte[] data) {
    }

    public void onReceive(@NotNull String channel, byte[] data) {
    }

    @Nullable
    public BiConsumer<String, byte[]> getConsumer() {
        return consumer;
    }

    public Set<String> getSubscribedChannels() {
        return subscribedChannels;
    }

    public boolean isEnabled() {
        return enabled;
    }

    @NotNull
    @Contract("_ -> this")
    public DeliveryClient setConsumer(@Nullable BiConsumer<String, byte[]> consumer) {
        this.consumer = consumer;
        return this;
    }

    @NotNull
    @Contract("_ -> this")
    public DeliveryClient setLogConsumer(@Nullable BiConsumer<Integer, String> logConsumer) {
        this.logConsumer = logConsumer;
        return this;
    }

    @NotNull
    @Contract("_ -> this")
    public DeliveryClient setEnabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public void start() {
        close();
        onStart();
        enabled = true;
    }

    public void close() {
        if (isEnabled()) {
            onClose();
        }
        enabled = false;
    }

    public void clear() {
        subscribedChannels.clear();
    }

    public boolean subscribe(@NotNull String... channels) {
        final List<String> list = new ArrayList<>();
        for (String channel : channels) {
            if (subscribedChannels.add(channel)) {
                list.add(channel);
            }
        }
        if (list.isEmpty()) {
            return false;
        }
        onSubscribe(list.toArray(new String[0]));
        return true;
    }

    public boolean unsubscribe(@NotNull String... channels) {
        final List<String> list = new ArrayList<>();
        for (String channel : channels) {
            if (subscribedChannels.remove(channel)) {
                list.add(channel);
            }
        }
        if (list.isEmpty()) {
            return false;
        }
        onUnsubscribe(list.toArray(new String[0]));
        return true;
    }

    public void send(@NotNull String channel, byte[] data) {
        onSend(channel, data);
    }

    public void receive(@NotNull String channel, byte[] data) {
        if (consumer != null) {
            consumer.accept(channel, data);
        }
        onReceive(channel, data);
    }

    protected void log(int level, @NotNull String msg) {
        if (logConsumer != null) {
            logConsumer.accept(level, msg);
        }
    }

    @NotNull
    protected String toBase64(byte[] data) {
        return Base64.getEncoder().encodeToString(data);
    }

    protected byte[] fromBase64(@NotNull String s) {
        return Base64.getDecoder().decode(s);
    }

}
