package com.saicone.savedata.module.hook;

import com.earth2me.essentials.User;
import com.google.common.base.Suppliers;
import com.saicone.mcode.env.Awake;
import com.saicone.mcode.env.Executes;
import net.ess3.api.IEssentials;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

@Awake(when = Executes.LOAD, dependsOn = "Essentials")
public class EssentialsHook extends PlayerProvider {

    private static final Supplier<IEssentials> ESSENTIALS = Suppliers.memoize(() -> (IEssentials) Bukkit.getPluginManager().getPlugin("Essentials"));

    public EssentialsHook() {
        PlayerProvider.register("Essentials", this);
    }

    @Override
    public @NotNull CompletableFuture<UUID> computeUniqueId(@NotNull String name) {
        final User user = ESSENTIALS.get().getOfflineUser(name);
        return CompletableFuture.completedFuture(user == null ? null : user.getUUID());
    }

    @Override
    public @NotNull CompletableFuture<String> computeName(@NotNull UUID uniqueId) {
        final User user = ESSENTIALS.get().getUser(uniqueId);
        return CompletableFuture.completedFuture(user == null ? null : user.getName());
    }
}