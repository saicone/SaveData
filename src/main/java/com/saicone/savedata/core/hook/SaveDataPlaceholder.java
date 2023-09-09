package com.saicone.savedata.core.hook;

import com.saicone.savedata.SaveData;
import com.saicone.savedata.core.data.DataCore;
import com.saicone.savedata.module.hook.Placeholders;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SaveDataPlaceholder {
    private final Map<String, Bridge> registeredNames = new HashMap<>();

    public void onReload() {
        if (!Placeholders.isEnabled()) {
            return;
        }
        if (SaveData.SETTINGS.getBoolean("placeholder.register", true)) {
            final Set<String> placeholderNames = new HashSet<>(SaveData.SETTINGS.getStringList("placeholder.names"));
            final Set<String> toDelete = new HashSet<>();
            for (Map.Entry<String, Bridge> entry : registeredNames.entrySet()) {
                if (!placeholderNames.contains(entry.getKey())) {
                    entry.getValue().unregister();
                    toDelete.add(entry.getKey());
                }
            }
            for (String name : toDelete) {
                registeredNames.remove(name);
            }
            for (String name : placeholderNames) {
                if (!registeredNames.containsKey(name)) {
                    final Bridge bridge = new Bridge(name);
                    bridge.register();
                    registeredNames.put(name, bridge);
                }
            }
            SaveData.log(4, "The placeholder expansion was loaded with the names: " + String.join(", ", registeredNames.keySet()));
        } else {
            onDisable();
        }
    }

    public void onDisable() {
        if (!registeredNames.isEmpty()) {
            for (Map.Entry<String, Bridge> entry : registeredNames.entrySet()) {
                entry.getValue().unregister();
            }
            registeredNames.clear();
        }
    }

    private static class Bridge extends PlaceholderExpansion {

        private final String name;

        public Bridge(@NotNull String name) {
            this.name = name;
        }

        @Override
        public @NotNull String getIdentifier() {
            return name;
        }

        @Override
        public @NotNull String getAuthor() {
            return "Rubenicos";
        }

        @Override
        public @NotNull String getVersion() {
            return "1.0";
        }

        @Override
        public boolean persist() {
            return true;
        }

        @Override
        public @Nullable String onPlaceholderRequest(Player player, @NotNull String s) {
            final String[] params = s.split("_", 4);
            if (params.length < 3) {
                return "[Pre-Check] Not enough args";
            }
            switch (params[0].toLowerCase()) {
                case "global":
                    if (params.length < 4) {
                        return "[Global] Not enough args";
                    }
                    return SaveData.get().getDataCore().getDataString(player, DataCore.Type.GLOBAL, params[1], params[2], params[3]);
                case "player":
                    if (player == null) {
                        return "[Player] Invalid player";
                    }
                    return SaveData.get().getDataCore().getDataString(player, DataCore.Type.PLAYER, player.getUniqueId(), params[1], params[2]);
                default:
                    return "[Check] The type '" + params[0] + "' is not a valid type";
            }
        }
    }
}
