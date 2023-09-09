package com.saicone.savedata;

import com.saicone.savedata.core.data.DataCore;
import com.saicone.savedata.core.command.SaveDataCommand;
import com.saicone.savedata.core.hook.SaveDataPlaceholder;
import com.saicone.savedata.module.command.BukkitCommand;
import com.saicone.savedata.module.lang.Lang;
import com.saicone.savedata.module.settings.BukkitSettings;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;

public class SaveData extends JavaPlugin {

    private static SaveData instance;
    public static final BukkitSettings SETTINGS = new BukkitSettings("settings.yml");

    private LibraryLoader libraryLoader;
    private int logLevel = 3;
    private DataCore dataCore;
    private SaveDataCommand command;
    private SaveDataPlaceholder placeholder;

    @NotNull
    public static SaveData get() {
        return instance;
    }

    public static void log(int level, @NotNull String msg) {
        if (level > get().logLevel) {
            return;
        }
        switch (level) {
            case 1: // error
                get().getLogger().severe(msg);
                break;
            case 2: // warning
                get().getLogger().warning(msg);
                break;
            case 3: // info
            case 4: // debug
            default:
                get().getLogger().info(msg);
                break;
        }
    }

    @Override
    public void onLoad() {
        instance = this;
        dataCore = new DataCore();
        reloadSettings();
    }

    @Override
    public void onEnable() {
        Lang.onReload();
        final File file = new File(getDataFolder(), "datatypes");
        if (!file.exists()) {
            SaveData.log(4, "Saving default data type...");
            saveResource("datatypes/default.yml", false);
        }
        dataCore.onReload();
        if (command == null) {
            SaveData.log(4, "Loading main command...");
            command = new SaveDataCommand();
            BukkitCommand.register(command);
        }
        if (placeholder == null) {
            SaveData.log(4, "Loading placeholder expansion...");
            placeholder = new SaveDataPlaceholder();
        }
        placeholder.onReload();
    }

    @Override
    public void onDisable() {
        if (command != null) {
            BukkitCommand.unregister(command);
        }
        if (placeholder != null) {
            placeholder.onDisable();
        }
        if (dataCore != null) {
            dataCore.onDisable();
        }
    }

    public void onReload() {
        reloadSettings();
        onEnable();
    }

    private void reloadSettings() {
        SETTINGS.loadFrom(getDataFolder(), true);
        logLevel = SETTINGS.getInt("plugin.log-level", 3);
        if (libraryLoader == null) {
            libraryLoader = new LibraryLoader();
            final ConfigurationSection section = SETTINGS.getConfigurationSection("library");
            if (section != null) {
                for (String key : section.getKeys(false)) {
                    final Object value = section.get(key);
                    if (value instanceof ConfigurationSection) {
                        libraryLoader.loadDependency((ConfigurationSection) value);
                    } else if (value instanceof String) {
                        libraryLoader.loadDependency((String) value);
                    }
                }
            } else {
                log(2, "The library section doesn't exists");
            }
            libraryLoader.load();
        }
    }

    @NotNull
    public LibraryLoader getLibraryLoader() {
        return libraryLoader;
    }

    public int getLogLevel() {
        return logLevel;
    }

    @NotNull
    public DataCore getDataCore() {
        return dataCore;
    }

    @NotNull
    public SaveDataCommand getCommand() {
        return command;
    }

    @NotNull
    public SaveDataPlaceholder getPlaceholder() {
        return placeholder;
    }
}
