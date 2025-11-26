/*
 * This file is part of PixelBuy, licensed under the MIT License
 *
 * Copyright (c) 2024 Rubenicos
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
package com.saicone.savedata.module.hook;

import com.earth2me.essentials.User;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import net.ess3.api.IEssentials;
import net.luckperms.api.LuckPerms;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class PlayerProvider {

    private static PlayerProvider INSTANCE = null;
    private static final Cache<String, UUID> ID_CACHE = CacheBuilder.newBuilder().expireAfterAccess(3L, TimeUnit.HOURS).build();
    private static final Cache<UUID, String> NAME_CACHE = CacheBuilder.newBuilder().expireAfterAccess(3L, TimeUnit.HOURS).build();
    private static final Map<String, Supplier<PlayerProvider>> SUPPLIERS = new LinkedHashMap<>();

    static {
        SUPPLIERS.put("LUCKPERMS", () -> {
            if (Bukkit.getPluginManager().getPlugin("LuckPerms") != null) {
                return new LuckPermsProvider();
            }
            return null;
        });
        SUPPLIERS.put("ESSENTIALS", () -> {
            if (Bukkit.getPluginManager().getPlugin("Essentials") != null) {
                return new EssentialsProvider();
            }
            return null;
        });
    }

    public static void compute(@NotNull String type) {
        if (type.equalsIgnoreCase("AUTO")) {
            for (var entry : SUPPLIERS.entrySet()) {
                final PlayerProvider provider = entry.getValue().get();
                if (provider != null) {
                    INSTANCE = provider;
                    return;
                }
            }
        } else {
            for (var entry : SUPPLIERS.entrySet()) {
                if (entry.getKey().equalsIgnoreCase(type)) {
                    final PlayerProvider provider = entry.getValue().get();
                    if (provider != null) {
                        INSTANCE = provider;
                        return;
                    }
                }
            }
        }
        INSTANCE = new PlayerProvider();
    }

    public static void supply(@NotNull String type, @NotNull Supplier<PlayerProvider> supplier) {
        SUPPLIERS.put(type, supplier);
    }

    @NotNull
    public static PlayerProvider get() {
        if (INSTANCE == null) {
            compute("AUTO");
        }
        return INSTANCE;
    }

    @NotNull
    public static UUID getUniqueId(@NotNull String name) {
        UUID cached = ID_CACHE.getIfPresent(name);
        if (cached == null) {
            var player = Bukkit.getPlayer(name);
            if (player != null) {
                cached = player.getUniqueId();
            } else {
                cached = get().uniqueId(name);
            }
            ID_CACHE.put(name, cached);
        }
        return cached;
    }

    @Nullable
    public static String getName(@NotNull UUID uniqueId) {
        String cached = NAME_CACHE.getIfPresent(uniqueId);
        if (cached == null) {
            final String name;
            var player = Bukkit.getPlayer(uniqueId);
            if (player != null) {
                name = player.getName();
            } else {
                name = get().name(uniqueId);
            }
            NAME_CACHE.put(uniqueId, name == null ? "" : name);
            cached = NAME_CACHE.getIfPresent(uniqueId);
        }
        return cached.isEmpty() ? null : cached;
    }

    @NotNull
    @SuppressWarnings("deprecation")
    public UUID uniqueId(@NotNull String name) {
        return Bukkit.getOfflinePlayer(name).getUniqueId();
    }

    @Nullable
    public String name(@NotNull UUID uniqueId) {
        return Bukkit.getOfflinePlayer(uniqueId).getName();
    }

    private static final class LuckPermsProvider extends PlayerProvider {

        private final LuckPerms luckPerms = net.luckperms.api.LuckPermsProvider.get();

        @Override
        public @NotNull UUID uniqueId(@NotNull String name) {
            final UUID uuid;
            try {
                uuid = luckPerms.getUserManager().lookupUniqueId(name).get();
            } catch (Throwable t) {
                return super.uniqueId(name);
            }
            return uuid == null ? super.uniqueId(name) : uuid;
        }

        @Override
        public @Nullable String name(@NotNull UUID uniqueId) {
            final String name;
            try {
                name = luckPerms.getUserManager().lookupUsername(uniqueId).get();
            } catch (Throwable t) {
                return super.name(uniqueId);
            }
            return name == null ? super.name(uniqueId) : name;
        }
    }

    private static final class EssentialsProvider extends PlayerProvider {

        private final IEssentials essentials = (IEssentials) Bukkit.getPluginManager().getPlugin("Essentials");

        @Override
        public @NotNull UUID uniqueId(@NotNull String name) {
            final User user = essentials.getOfflineUser(name);
            return user == null ? super.uniqueId(name) : user.getUUID();
        }

        @Override
        public @Nullable String name(@NotNull UUID uniqueId) {
            final User user = essentials.getUser(uniqueId);
            return user == null ? super.name(uniqueId) : user.getName();
        }
    }
}