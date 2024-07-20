package com.saicone.savedata;

import com.saicone.savedata.api.data.DataUser;
import com.saicone.savedata.core.command.SaveDataCommand;
import com.saicone.savedata.core.data.DataOperator;
import com.saicone.savedata.module.command.BukkitCommand;
import com.saicone.savedata.module.hook.Placeholders;
import com.saicone.savedata.module.hook.PlayerProvider;
import com.saicone.savedata.module.lang.Lang;
import com.saicone.savedata.module.listener.BukkitListener;
import com.saicone.types.Types;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.logging.Level;

public class SaveDataBukkit extends JavaPlugin implements SaveDataPlugin {

    private SaveDataCommand command;

    private Set<String> placeholderNames;

    public SaveDataBukkit() {
        SaveData.init(new SaveData(this));
    }

    @Override
    public void onLoad() {
        SaveData.get().onLoad();
    }

    @Override
    public void onEnable() {
        SaveData.get().onEnable();
        Lang.onReload();
        PlayerProvider.compute(SaveData.settings().getIgnoreCase("plugin", "playerprovider").asString("AUTO"));
        getServer().getPluginManager().registerEvents(new BukkitListener(), this);
        if (command == null) {
            command = new SaveDataCommand(this);
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
        SaveData.get().onDisable();
    }

    public void onReload() {
        SaveData.get().onReload();
        PlayerProvider.compute(SaveData.settings().getIgnoreCase("plugin", "playerprovider").asString("AUTO"));
        unregisterPlaceholders();
        registerPlaceholders();
    }

    @NotNull
    public SaveDataCommand getCommand() {
        return command;
    }

    @Override
    public @NotNull Path getFolder() {
        return getDataFolder().toPath();
    }

    private void registerPlaceholders() {
        if (Placeholders.isEnabled() && SaveData.settings().getIgnoreCase("hook", "placeholderapi", "enabled").asBoolean(true)) {
            this.placeholderNames = Placeholders.registerOffline(this, SaveData.settings().getIgnoreCase("hook", "placeholderapi", "names").asSet(Types.STRING), (player, s) -> {
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
                    value = params[5];
                } else {
                    operator = DataOperator.GET;
                    database = params[1];
                    dataType = params[2];
                    value = null;
                }
                return SaveData.get().getDataCore().userValue(uniqueId, operator, database, dataType, value, str -> Placeholders.parse(player, str)).join();
            });
        }
    }

    private void unregisterPlaceholders() {
        if (this.placeholderNames != null) {
            Placeholders.unregister(this.placeholderNames);
            this.placeholderNames = null;
        }
    }

    @Override
    public void log(int level, @NotNull Supplier<String> msg) {
        switch (level) {
            case 1:
                getLogger().log(Level.SEVERE, msg);
                break;
            case 2:
                getLogger().log(Level.WARNING, msg);
                break;
            case 3:
            case 4:
            default:
                getLogger().log(Level.INFO, msg);
                break;
        }
    }

    @Override
    public void logException(int level, @NotNull Throwable throwable) {
        switch (level) {
            case 1:
                getLogger().log(Level.SEVERE, throwable, () -> "");
                break;
            case 2:
                getLogger().log(Level.WARNING, throwable, () -> "");
                break;
            case 3:
            case 4:
            default:
                getLogger().log(Level.INFO, throwable, () -> "");
                break;
        }
    }

    @Override
    public void logException(int level, @NotNull Throwable throwable, @NotNull Supplier<String> msg) {
        switch (level) {
            case 1:
                getLogger().log(Level.SEVERE, throwable, msg);
                break;
            case 2:
                getLogger().log(Level.WARNING, throwable, msg);
                break;
            case 3:
            case 4:
            default:
                getLogger().log(Level.INFO, throwable, msg);
                break;
        }
    }
}
