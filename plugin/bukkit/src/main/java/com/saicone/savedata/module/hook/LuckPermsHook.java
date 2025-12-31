package com.saicone.savedata.module.hook;

import com.google.common.base.Suppliers;
import com.saicone.mcode.env.Awake;
import com.saicone.mcode.env.Executes;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

@Awake(when = Executes.LOAD, dependsOn = "LuckPerms")
public class LuckPermsHook extends PlayerProvider {

    private static final Supplier<LuckPerms> LUCKPERMS = Suppliers.memoize(LuckPermsProvider::get);

    public LuckPermsHook() {
        PlayerProvider.register("LuckPerms", this);
    }

    @Override
    public @NotNull CompletableFuture<UUID> computeUniqueId(@NotNull String name) {
        return LUCKPERMS.get().getUserManager().lookupUniqueId(name);
    }

    @Override
    public @NotNull CompletableFuture<String> computeName(@NotNull UUID uniqueId) {
        return LUCKPERMS.get().getUserManager().lookupUsername(uniqueId);
    }
}
