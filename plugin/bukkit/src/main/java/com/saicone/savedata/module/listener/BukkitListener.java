package com.saicone.savedata.module.listener;

import com.saicone.mcode.module.task.Task;
import com.saicone.savedata.SaveData;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class BukkitListener implements Listener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Task.runAsync(() -> {
            SaveData.get().getDataCore().loadUser(event.getPlayer().getUniqueId());
        });
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Task.runAsync(() -> {
            SaveData.get().getDataCore().saveUser(event.getPlayer().getUniqueId());
        });
    }
}
