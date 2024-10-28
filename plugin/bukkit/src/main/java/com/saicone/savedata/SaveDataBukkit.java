package com.saicone.savedata;

import com.saicone.mcode.bootstrap.Addon;
import com.saicone.mcode.bootstrap.PluginDescription;
import com.saicone.mcode.bukkit.lang.BukkitLang;
import com.saicone.mcode.module.lang.AbstractLang;
import com.saicone.mcode.module.task.Task;
import com.saicone.mcode.platform.PlatformType;
import com.saicone.savedata.api.data.DataUser;
import com.saicone.savedata.core.Lang;
import com.saicone.savedata.core.command.SaveDataCommand;
import com.saicone.savedata.api.data.DataOperator;
import com.saicone.savedata.module.command.BukkitCommand;
import com.saicone.savedata.module.hook.Placeholders;
import com.saicone.savedata.module.hook.PlayerProvider;
import com.saicone.savedata.module.listener.BukkitListener;
import com.saicone.types.Types;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

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
                        if (player == null || !player.isOnline()) {
                            return "Player is not online";
                        }
                        uniqueId = player.getUniqueId();
                        break;
                    default:
                        return "The type '" + params[0] + "' is not a valid type";
                }
                final DataOperator operator;
                final String database;
                final String dataType;
                final String value;
                if (params.length > 4) {
                    if (params[3].equalsIgnoreCase("GET")) {
                        operator = DataOperator.GET;
                    } else if (params[3].equalsIgnoreCase("CONTAINS")) {
                        operator = DataOperator.CONTAINS;
                    } else {
                        return "The string '" + params[3] + "' is not a valid operator";
                    }
                    database = params[1];
                    dataType = params[2];
                    value = params[4];
                } else {
                    operator = DataOperator.GET;
                    database = params[1];
                    dataType = params[2];
                    value = null;
                }
                return getDataCore().userValue(uniqueId, operator, database, dataType, value, str -> Placeholders.parse(player, str)).join();
            });
        }
    }

    private void unregisterPlaceholders() {
        if (this.placeholderNames != null) {
            Placeholders.unregister(this.placeholderNames);
            this.placeholderNames = null;
        }
    }
}
