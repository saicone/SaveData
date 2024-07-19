package com.saicone.savedata;

import com.saicone.savedata.core.command.SaveDataCommand;
import com.saicone.savedata.core.hook.SaveDataPlaceholder;
import com.saicone.savedata.module.command.BukkitCommand;
import com.saicone.savedata.module.lang.Lang;
import com.saicone.savedata.module.listener.BukkitListener;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.function.Supplier;
import java.util.logging.Level;

public class SaveDataBukkit extends JavaPlugin implements SaveDataPlugin {

    private SaveDataCommand command;
    private SaveDataPlaceholder placeholder;

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
        getServer().getPluginManager().registerEvents(new BukkitListener(), this);
        if (command == null) {
            command = new SaveDataCommand();
            BukkitCommand.register(command);
        }
        if (placeholder == null) {
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
        SaveData.get().onDisable();
    }

    public void onReload() {
        SaveData.get().onReload();
    }

    @NotNull
    public SaveDataCommand getCommand() {
        return command;
    }

    @NotNull
    public SaveDataPlaceholder getPlaceholder() {
        return placeholder;
    }

    @Override
    public @NotNull Path getFolder() {
        return getDataFolder().toPath();
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
