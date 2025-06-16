package com.saicone.savedata;

import com.saicone.mcode.bootstrap.Addon;
import com.saicone.mcode.bootstrap.PluginDescription;
import com.saicone.mcode.bukkit.lang.BukkitLang;
import com.saicone.mcode.module.lang.AbstractLang;
import com.saicone.mcode.module.task.Task;
import com.saicone.mcode.platform.PlatformType;
import com.saicone.savedata.api.data.DataUser;
import com.saicone.savedata.api.top.TopEntry;
import com.saicone.savedata.core.Lang;
import com.saicone.savedata.core.command.SaveDataCommand;
import com.saicone.savedata.api.data.DataOperator;
import com.saicone.savedata.core.data.Database;
import com.saicone.savedata.module.command.BukkitCommand;
import com.saicone.savedata.module.hook.Placeholders;
import com.saicone.savedata.module.hook.PlayerProvider;
import com.saicone.savedata.module.listener.BukkitListener;
import com.saicone.settings.update.NodeUpdate;
import com.saicone.types.Types;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@PluginDescription(
        name = "SaveData",
        version = "${version}",
        authors = { "Rubenicos" },
        platform = { PlatformType.BUKKIT },
        addons = { Addon.MODULE_TASK, Addon.MODULE_LANG },
        compatibility = "1.8.8 - 1.21.1",
        foliaSupported = true,
        softDepend = { "PlaceholderAPI", "LuckPerms", "Essentials" }
)
public class SaveDataBukkit extends SaveData {

    @NotNull
    public static JavaPlugin plugin() {
        return bootstrap();
    }

    private SaveDataCommand command;

    private Set<String> placeholderNames;

    @Override
    protected @NotNull AbstractLang<?> initLang() {
        return new BukkitLang(plugin(), new Lang());
    }

    @Override
    protected void initUpdates(@NotNull List<NodeUpdate> updates) {
        updates.add(NodeUpdate.move().from("placeholder", "register").to("placeholder", "Enabled"));
        updates.add(NodeUpdate.move().from("placeholder", "names").to("placeholder", "Names"));
        updates.add(NodeUpdate.move().from("placeholder").to("Hook", "PlaceholderAPI"));
    }

    @Override
    public void onEnable() {
        super.onEnable();
        PlayerProvider.compute(SaveData.settings().getIgnoreCase("plugin", "playerprovider").asString("AUTO"));
        Bukkit.getPluginManager().registerEvents(new BukkitListener(), plugin());
        if (command == null) {
            command = new SaveDataCommand();
            BukkitCommand.register(command);
        }
        registerPlaceholders();
    }

    @Override
    public void onDisable() {
        unregisterPlaceholders();
        if (command != null) {
            BukkitCommand.unregister(command);
        }
        super.onDisable();
    }

    @Override
    public void onReload() {
        super.onReload();
        PlayerProvider.compute(SaveData.settings().getIgnoreCase("plugin", "playerprovider").asString("AUTO"));
        Task.sync(() -> {
            unregisterPlaceholders();
            registerPlaceholders();
        });
    }

    @NotNull
    public SaveDataCommand getCommand() {
        return command;
    }

    private void registerPlaceholders() {
        if (Placeholders.isEnabled() && SaveData.settings().getIgnoreCase("hook", "placeholderapi", "enabled").asBoolean(true)) {
            // <type>_<database>_<data type>
            // <type>_<database>_<data type>_[value]
            // <type>_<database>_<data type>_<operator>_[value]
            // top_<database>_<data type>_value
            // top_<database>_<data type>_position
            // top_<database>_<data type>_<index>_<value>
            //
            // type = global | server | player
            this.placeholderNames = Placeholders.registerOffline(plugin(), SaveData.settings().getIgnoreCase("hook", "placeholderapi", "names").asSet(Types.STRING), (player, s) -> {
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
                        final Database database = getDataCore().getDatabases().get(params[1]);
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
                return getDataCore().userValue(uniqueId, operator, database, dataType, value, str -> Placeholders.parse(player, str), SaveData.get().getLang().getLanguageFor(player)).join();
            });
        }
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
                    return PlayerProvider.getName(user);
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

    private void unregisterPlaceholders() {
        if (this.placeholderNames != null) {
            Placeholders.unregister(this.placeholderNames);
            this.placeholderNames = null;
        }
    }
}
