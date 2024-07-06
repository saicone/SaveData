package com.saicone.savedata.core.command;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class DataCommand extends Command {

    public DataCommand(@NotNull String name) {
        super(name);
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
        return true;
    }

    @NotNull
    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args, @Nullable Location location) throws IllegalArgumentException {
        return super.tabComplete(sender, alias, args, location);
    }
}
