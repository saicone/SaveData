package com.saicone.savedata.core.command;

import com.saicone.mcode.module.task.Task;
import com.saicone.savedata.SaveData;
import com.saicone.savedata.core.Lang;
import com.saicone.savedata.module.hook.Placeholders;
import com.saicone.savedata.module.hook.PlayerProvider;
import com.saicone.types.parser.BooleanParser;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class SaveDataCommand extends Command implements MainCommand {

    public SaveDataCommand() {
        super("savedata", "Main command for SaveData plugin", "/savedata", List.of("sd", "sdata"));
        setPermission("savedata.use;savedata.*");
    }

    @Override
    public @NotNull UUID getUniqueId(@NotNull String name) {
        return PlayerProvider.getUniqueId(name);
    }

    @Override
    public void getUniqueId(@NotNull String condition, @NotNull BiConsumer<UUID, Function<String, String>> consumer) {
        Task.async(() -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (!player.hasPermission(condition)) {
                    final Boolean result = BooleanParser.INSTANCE.parse(Placeholders.parse(player, condition));
                    if (result == null || !result) {
                        continue;
                    }
                }
                consumer.accept(player.getUniqueId(), s -> Placeholders.parse(player, s));
            }
        });
    }

    @Override
    public @NotNull Function<String, String> getUserParser(@Nullable UUID uniqueId) {
        if (uniqueId == null) {
            return s -> Placeholders.parse(null, s);
        } else {
            final OfflinePlayer player = Bukkit.getOfflinePlayer(uniqueId);
            return s -> Placeholders.parse(player, s);
        }
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String cmd, @NotNull String[] args) {
        SaveData.log(4, "Perm");
        if (!testPermission(sender)) {
            return true;
        }

        run(sender, cmd, args);
        return true;
    }

    @NotNull
    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args, @Nullable Location location) throws IllegalArgumentException {
        if (args.length < 1) {
            return TYPE;
        }
        if (!TYPE.contains(args[0])) {
            return List.of();
        }
        if (args.length == 1) {
            return super.tabComplete(sender, alias, args, location);
        }
        if (args.length < 3) {
            return OPERATOR;
        }
        if (!OPERATOR.contains(args[2])) {
            return List.of();
        }
        if (args.length == 4) {
            return new ArrayList<>(SaveData.get().getDataCore().getDatabases().keySet());
        }
        if (args.length == 5) {
            return new ArrayList<>(SaveData.get().getDataCore().getDataTypes().keySet());
        }
        return List.of();
    }

    @Override
    public boolean testPermission(@NotNull CommandSender target) {
        if (testPermissionSilent(target)) {
            return true;
        }
        Lang.NO_PERMISSION.sendTo(target);
        return false;
    }
}
