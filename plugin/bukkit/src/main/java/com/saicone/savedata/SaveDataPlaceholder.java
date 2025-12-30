package com.saicone.savedata;

import com.saicone.mcode.env.Awake;
import com.saicone.mcode.env.Executes;
import com.saicone.mcode.module.task.Task;
import com.saicone.savedata.api.data.DataOperator;
import com.saicone.savedata.api.data.DataUser;
import com.saicone.savedata.api.top.TopEntry;
import com.saicone.savedata.core.data.Database;
import com.saicone.savedata.module.hook.Placeholders;
import com.saicone.savedata.module.hook.PlayerProvider;
import com.saicone.types.Types;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.UUID;
import java.util.function.BiFunction;

// <type>_<database>_<data type>
// <type>_<database>_<data type>_[value]
// <type>_<database>_<data type>_<operator>_[value]
// top_<database>_<data type>_value
// top_<database>_<data type>_position
// top_<database>_<data type>_<index>_<value>
//
// type = global | server | player
public class SaveDataPlaceholder implements BiFunction<OfflinePlayer, String, Object> {

    private Set<String> names = Set.of();

    @NotNull
    public Set<String> getNames() {
        return names;
    }

    @Awake(when = Executes.ENABLE, dependsOn = "PlaceholderAPI")
    public void enable() {
        this.names = SaveData.settings().getIgnoreCase("hook", "placeholderapi", "names").asSet(Types.STRING);
        if (SaveData.settings().getIgnoreCase("hook", "placeholderapi", "enabled").asBoolean(true)) {
            Placeholders.registerOffline(SaveData.bootstrap(), names, this);
        }
    }

    @Awake(when = Executes.DISABLE, dependsOn = "PlaceholderAPI")
    public void disable() {
        Placeholders.unregister(this.names);
    }

    @Awake(when = Executes.RELOAD, dependsOn = "PlaceholderAPI")
    public void reload() {
        // For some reason, all the PlaceholderAPI events are set to run synchronously
        Task.sync(() -> {
            disable();
            enable();
        });
    }

    @Override
    public Object apply(OfflinePlayer player, String s) {
        final String[] params = s.split("_", 5);
        if (params.length < 3) {
            return "Not enough args";
        }

        final UUID uniqueId;
        switch (params[0].toLowerCase()) {
            case "global":
            case "server":
                uniqueId = DataUser.SERVER_ID;
                break;
            case "player":
                if (player == null) {
                    return "Cannot get player information without player";
                }
                uniqueId = player.getUniqueId();
                break;
            case "top":
                final Database database = SaveData.get().getDataCore().getDatabases().get(params[1]);
                if (database == null) {
                    return "The database '" + params[1] + "' doesn't exist";
                }
                final TopEntry<?> top = database.getTop(params[2]);
                if (top == null) {
                    return "The data type '" + params[2] + "' doesn't have a top on '" + params[1] + "' database";
                }
                if (params.length < 5) {
                    uniqueId = player == null ? DataUser.SERVER_ID : player.getUniqueId();
                    if (params.length > 3 && params[3].equalsIgnoreCase("value")) {
                        return top.value(uniqueId);
                    }
                    return top.get(uniqueId);
                }
                uniqueId = top.get(Integer.parseInt(params[3]));
                if (params[4].equalsIgnoreCase("value")) {
                    return top.formatted(uniqueId);
                }
                if (uniqueId == null) {
                    return getUserValue(params[4]);
                }
                return getUserValue(uniqueId, params[4]);
            default:
                return "The type '" + params[0] + "' is not a valid type";
        }
        final DataOperator operator;
        final String database;
        final String dataType;
        final String value;
        if (params.length > 4) {
            try {
                operator = DataOperator.valueOf(params[3].toUpperCase());
            } catch (IllegalArgumentException e) {
                return "The string '" + params[3] + "' is not a valid operator";
            }
            if (!operator.isEval()) {
                return "Cannot execute data update using placeholders";
            }
            database = params[1];
            dataType = params[2];
            value = params[4];
        } else {
            operator = DataOperator.GET;
            database = params[1];
            dataType = params[2];
            value = params.length > 3 ? params[3] : null;
        }
        return SaveData.get().getDataCore().userValue(uniqueId, operator, database, dataType, value, str -> Placeholders.parse(player, str), SaveData.get().getLang().getLanguageFor(player)).join();
    }

    @Nullable
    private Object getUserValue(@NotNull String key) {
        switch (key.toLowerCase()) {
            case "uuid":
                return DataUser.SERVER_ID;
            case "id":
                return DataUser.SERVER_ID.toString().replace('-', '\0');
            case "name":
            case "display_name":
                return "---";
            default:
                return null;
        }
    }

    @Nullable
    private Object getUserValue(@NotNull UUID user, @NotNull String key) {
        if (DataUser.SERVER_ID.equals(user)) {
            switch (key.toLowerCase()) {
                case "uuid":
                    return user;
                case "name":
                    return "@server";
                default:
                    return null;
            }
        }
        final Player player = Bukkit.getPlayer(user);
        if (player == null) {
            switch (key.toLowerCase()) {
                case "uuid":
                    return user;
                case "id":
                    return user.toString().replace('-', '\0');
                case "name":
                    try {
                        return PlayerProvider.getName(user);
                    } catch (Throwable t) {
                        return "<unknown>";
                    }
                default:
                    return null;
            }
        }
        switch (key.toLowerCase()) {
            case "uuid":
                return user;
            case "id":
                return user.toString().replace('-', '\0');
            case "name":
                return player.getName();
            case "display_name":
                return player.getDisplayName();
            default:
                return null;
        }
    }
}