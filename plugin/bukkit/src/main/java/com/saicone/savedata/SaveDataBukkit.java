package com.saicone.savedata;

import com.saicone.mcode.bootstrap.Addon;
import com.saicone.mcode.bootstrap.PluginDescription;
import com.saicone.mcode.bukkit.lang.BukkitLang;
import com.saicone.mcode.module.lang.AbstractLang;
import com.saicone.mcode.platform.PlatformType;
import com.saicone.savedata.core.Lang;
import com.saicone.savedata.core.command.SaveDataCommand;
import com.saicone.savedata.module.command.BukkitCommand;
import com.saicone.savedata.module.listener.BukkitListener;
import com.saicone.settings.update.NodeUpdate;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@PluginDescription(
        name = "SaveData",
        version = "${version}",
        authors = { "Rubenicos" },
        platform = { PlatformType.BUKKIT },
        addons = { Addon.MODULE_TASK, Addon.MODULE_LANG },
        compatibility = "1.8.8 - 1.21.8",
        foliaSupported = true,
        softDepend = { "PlaceholderAPI", "LuckPerms", "Essentials" },
        loadBefore = "QuickShop-Hikari"
)
public class SaveDataBukkit extends SaveData {

    @NotNull
    public static JavaPlugin plugin() {
        return bootstrap();
    }

    private SaveDataCommand command;

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
        Bukkit.getPluginManager().registerEvents(new BukkitListener(), plugin());
        if (command == null) {
            command = new SaveDataCommand();
            BukkitCommand.register(plugin(), command);
        }
    }

    @Override
    public void onDisable() {
        if (command != null) {
            BukkitCommand.unregister(command);
        }
        super.onDisable();
    }

    @NotNull
    public SaveDataCommand getCommand() {
        return command;
    }
}
