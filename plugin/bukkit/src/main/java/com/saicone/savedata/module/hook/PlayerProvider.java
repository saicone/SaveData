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
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public abstract class PlayerProvider {

    private static final PlayerProvider ONLINE = new PlayerProvider() {
        @Override
        public @Nullable UUID computeUniqueId(@NotNull String name) {
            final Player player = Bukkit.getPlayerExact(name);
            return player == null ? null : player.getUniqueId();
        }

        @Override
        public @Nullable String computeName(@NotNull UUID uniqueId) {
            final Player player = Bukkit.getPlayer(uniqueId);
            return player == null ? null : player.getName();
        }
    };
    private static final PlayerProvider OFFLINE = new PlayerProvider() {
        @Override
        @SuppressWarnings("deprecation")
        public @NotNull UUID computeUniqueId(@NotNull String name) {
            return Bukkit.getOfflinePlayer(name).getUniqueId();
        }

        @Override
        public @Nullable String computeName(@NotNull UUID uniqueId) {
            return Bukkit.getOfflinePlayer(uniqueId).getName();
        }
    };

    private static final Cache<String, UUID> ID_CACHE = CacheBuilder.newBuilder().expireAfterAccess(3L, TimeUnit.HOURS).build();
    private static final Cache<UUID, String> NAME_CACHE = CacheBuilder.newBuilder().expireAfterAccess(3L, TimeUnit.HOURS).build();

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
    public static UUID getUniqueId(@NotNull String name) {
        UUID cached = ID_CACHE.getIfPresent(name);
        if (cached == null) {
            for (PlayerProvider provider : PROVIDERS) {
                cached = provider.computeUniqueId(name);
                if (cached != null) break;
            }
            if (cached == null) {
                throw new IllegalArgumentException("Cannot get player unique id from player name: " + name);
            }
        }
        return cached;
    }

    @NotNull
    public static String getName(@NotNull UUID uniqueId) {
        String cached = NAME_CACHE.getIfPresent(uniqueId);
        if (cached == null) {
            for (PlayerProvider provider : PROVIDERS) {
                cached = provider.computeName(uniqueId);
                if (cached != null && !cached.isBlank()) break;
            }
            if (cached == null) {
                throw new IllegalArgumentException("Cannot get player name from player unique id: " + uniqueId);
            }
        }
        return cached;
    }

    @Nullable
    public abstract UUID computeUniqueId(@NotNull String name);

    @Nullable
    public abstract String computeName(@NotNull UUID uniqueId);
}
