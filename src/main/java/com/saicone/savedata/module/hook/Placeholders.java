package com.saicone.savedata.module.hook;

import com.google.common.base.Suppliers;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public class Placeholders {

    private static final Supplier<Boolean> ENABLED = Suppliers.memoize(() -> Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI"));

    public static boolean isEnabled() {
        return ENABLED.get();
    }

    @NotNull
    public static String parse(@NotNull OfflinePlayer player, @NotNull String s) {
        if (isEnabled()) {
            PlaceholderAPI.setPlaceholders(player, s);
        }
        return s;
    }
}
