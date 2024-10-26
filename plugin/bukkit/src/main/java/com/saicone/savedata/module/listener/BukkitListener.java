package com.saicone.savedata.module.listener;

import com.saicone.mcode.env.Awake;
import com.saicone.mcode.env.Executes;
import com.saicone.mcode.module.task.Task;
import com.saicone.savedata.SaveData;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
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

    @Awake(when = { Executes.ENABLE, Executes.RELOAD })
    private static void loadPlayers() {
        SaveData.log(4, "Loading all online players...");
        for (Player player : Bukkit.getOnlinePlayers()) {
            SaveData.get().getDataCore().loadUser(player.getUniqueId());
        }
    }
}
