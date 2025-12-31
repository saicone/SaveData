package com.saicone.savedata.module.hook;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.saicone.mcode.env.Awake;
import com.saicone.mcode.env.Executes;
import com.saicone.savedata.SaveData;
import com.saicone.types.Types;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public abstract class PlayerProvider {

    private static final PlayerProvider ONLINE = new PlayerProvider() {
        @Override
        public @NotNull CompletableFuture<UUID> computeUniqueId(@NotNull String name) {
            final Player player = Bukkit.getPlayerExact(name);
            return CompletableFuture.completedFuture(player == null ? null : player.getUniqueId());
        }

        @Override
        public @NotNull CompletableFuture<String> computeName(@NotNull UUID uniqueId) {
            final Player player = Bukkit.getPlayer(uniqueId);
            return CompletableFuture.completedFuture(player == null ? null : player.getName());
        }
    };
    private static final PlayerProvider OFFLINE = new PlayerProvider() {
        @Override
        @SuppressWarnings("deprecation")
        public @NotNull CompletableFuture<UUID> computeUniqueId(@NotNull String name) {
            return CompletableFuture.supplyAsync(() -> Bukkit.getOfflinePlayer(name).getUniqueId());
        }

        @Override
        public @NotNull CompletableFuture<String> computeName(@NotNull UUID uniqueId) {
            return CompletableFuture.supplyAsync(() -> Bukkit.getOfflinePlayer(uniqueId).getName());
        }
    };

    private static final Cache<String, UUID> ID_CACHE = CacheBuilder.newBuilder().expireAfterAccess(3L, TimeUnit.HOURS).build();
    private static final Cache<UUID, String> NAME_CACHE = CacheBuilder.newBuilder().expireAfterAccess(3L, TimeUnit.HOURS).build();
    private static final Map<String, CompletableFuture<UUID>> ASYNC_ID_CACHE = new HashMap<>();
    private static final Map<UUID, CompletableFuture<String>> ASYNC_NAME_CACHE = new HashMap<>();

    private static final Map<String, PlayerProvider> REGISTRY = new HashMap<>();
    private static final List<PlayerProvider> PROVIDERS = new ArrayList<>(List.of(ONLINE, OFFLINE));

    @Awake(when = {Executes.LOAD, Executes.RELOAD}, priority = 1)
    public static void compute() {
        final Set<String> preference = SaveData.settings().getIgnoreCase("plugin", "playerprovider").asSet(Types.STRING);
        if (preference.contains("AUTO")) {
            compute(Set.of("LuckPerms", "Essentials"));
        } else {
            compute(preference);
        }
    }

    public static void compute(@NotNull Collection<String> preference) {
        PROVIDERS.clear();
        PROVIDERS.add(ONLINE);
        for (String key : preference) {
            final PlayerProvider provider = REGISTRY.get(key.toLowerCase());
            if (provider != null) {
                PROVIDERS.add(provider);
            }
        }
        PROVIDERS.add(OFFLINE);
    }

    @Nullable
    public static PlayerProvider register(@NotNull String id, @NotNull PlayerProvider provider) {
        return REGISTRY.put(id.toLowerCase(), provider);
    }

    @Nullable
    public static PlayerProvider unregister(@NotNull String id) {
        return REGISTRY.remove(id.toLowerCase());
    }

    @NotNull
    public static Optional<UUID> getUniqueId(@NotNull String name) {
        final UUID uniqueId = ID_CACHE.getIfPresent(name);
        if (uniqueId != null) {
            return Optional.of(uniqueId);
        }
        return Optional.of(getUniqueIdAsync0(name).join());
    }

    @NotNull
    public static CompletableFuture<UUID> getUniqueIdAsync(@NotNull String name) {
        final UUID uniqueId = ID_CACHE.getIfPresent(name);
        if (uniqueId != null) {
            return CompletableFuture.completedFuture(uniqueId);
        }
        return getUniqueIdAsync0(name);
    }

    @NotNull
    private static CompletableFuture<UUID> getUniqueIdAsync0(@NotNull String name) {
        CompletableFuture<UUID> future = ASYNC_ID_CACHE.get(name);
        if (future == null) {
            for (PlayerProvider provider : PROVIDERS) {
                if (future == null) {
                    future = provider.computeUniqueId(name);
                } else {
                    future = future.thenCompose(id -> {
                        if (id != null) {
                            ID_CACHE.put(name, id);
                            ASYNC_ID_CACHE.remove(name);
                            return CompletableFuture.completedFuture(id);
                        } else {
                            return provider.computeUniqueId(name);
                        }
                    });
                }
            }
            ASYNC_ID_CACHE.put(name, future);
        }
        return future;
    }

    @NotNull
    public static Optional<String> getName(@NotNull UUID uniqueId) {
        final String name = NAME_CACHE.getIfPresent(uniqueId);
        if (name != null) {
            return Optional.of(name);
        }
        return Optional.of(getNameAsync0(uniqueId).join());
    }

    @NotNull
    public static CompletableFuture<String> getNameAsync(@NotNull UUID uniqueId) {
        final String name = NAME_CACHE.getIfPresent(uniqueId);
        if (name != null) {
            return CompletableFuture.completedFuture(name);
        }
        return getNameAsync0(uniqueId);
    }

    @NotNull
    private static CompletableFuture<String> getNameAsync0(@NotNull UUID uniqueId) {
        CompletableFuture<String> future = ASYNC_NAME_CACHE.get(uniqueId);
        if (future == null) {
            for (PlayerProvider provider : PROVIDERS) {
                if (future == null) {
                    future = provider.computeName(uniqueId);
                } else {
                    future = future.thenCompose(name -> {
                        if (name != null) {
                            NAME_CACHE.put(uniqueId, name);
                            ASYNC_NAME_CACHE.remove(uniqueId);
                            return CompletableFuture.completedFuture(name);
                        } else {
                            return provider.computeName(uniqueId);
                        }
                    });
                }
            }
            ASYNC_NAME_CACHE.put(uniqueId, future);
        }
        return future;
    }

    @NotNull
    public abstract CompletableFuture<UUID> computeUniqueId(@NotNull String name);

    @NotNull
    public abstract CompletableFuture<String> computeName(@NotNull UUID uniqueId);
}
