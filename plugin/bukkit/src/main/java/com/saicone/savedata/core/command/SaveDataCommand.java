package com.saicone.savedata.core.command;

import com.saicone.savedata.SaveData;
import com.saicone.savedata.core.data.DataCore.Type;
import com.saicone.savedata.core.data.DataCore.Operator;
import com.saicone.savedata.core.data.DataCore.Result;
import com.saicone.savedata.module.lang.Lang;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SaveDataCommand extends Command {

    private static final List<String> TYPE = List.of("reload", "player", "global");
    private static final List<String> OPERATOR = List.of("set", "get", "add", "delete", "substract", "multiply", "divide");

    private final SaveData plugin = SaveData.get();

    public SaveDataCommand() {
        super("savedata", "Main command for SaveData plugin", "/savedata", List.of("sd", "sdata"));
        setPermission("savedata.use;savedata.*");
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String cmd, @NotNull String[] args) {
        if (args.length < 5 || !TYPE.contains(args[0]) || !OPERATOR.contains(args[2])) {
            Lang.COMMAND_HELP.sendTo(sender, cmd);
            return true;
        }

        final long before = System.currentTimeMillis();
        if (args[0].equalsIgnoreCase("reload")) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                plugin.onReload();
                final long time = System.currentTimeMillis() - before;
                Lang.COMMAND_RELOAD.sendTo(sender, time);
            });
            return true;
        }

        if (!plugin.getDataCore().getDatabases().containsKey(args[3])) {
            Lang.COMMAND_ERROR_DATABASE.sendTo(sender, args[3]);
            return true;
        }
        if (!plugin.getDataCore().getDataTypes().containsKey(args[4])) {
            Lang.COMMAND_ERROR_DATATYPE.sendTo(sender, args[4]);
            return true;
        }
        final boolean getResult = args[args.length - 1].equalsIgnoreCase("-result");
        if (getResult) {
            args = Arrays.copyOf(args, args.length - 1);
        }

        final Type type;
        final Object name;
        if (args[0].equalsIgnoreCase("player")) {
            type = Type.PLAYER;
            final Player player = Bukkit.getPlayer(args[1]);
            if (player != null) {
                name = player.getUniqueId();
            } else {
                name = plugin.getDataCore().getLinkedPlayers().get(args[1]);
                if (name == null) {
                    Lang.COMMAND_ERROR_PLAYER.sendTo(sender, args[1]);
                    return true;
                }
            }
        } else {
            type = Type.GLOBAL;
            name = args[1];
        }
        final Operator operator = Operator.valueOf(args[2].toUpperCase());
        final String value;
        if (args.length < 6) {
            value = null;
        } else if (args.length == 6) {
            value = args[5];
        } else {
            value = String.join(" ", Arrays.copyOfRange(args, 5, args.length));
        }
        final Object result = plugin.getDataCore().onExecute(type, name, operator, args[3], args[4], value);

        if (operator == Operator.GET) {
            Lang.COMMAND_DATA_GET.sendTo(sender, args[1], args[3], args[4], result);
        } else {
            if (result instanceof Result) {
                switch ((Result) result) {
                    case INVALID_OPERATOR:
                        Lang.COMMAND_DATA_ERROR_OPERATOR.sendTo(sender, args[2]);
                        break;
                    case INVALID_ID:
                        Lang.COMMAND_DATA_ERROR_ID.sendTo(sender, args[4]);
                        break;
                    case INVALID_VALUE:
                        Lang.COMMAND_DATA_ERROR_VALUE.sendTo(sender, value);
                        break;
                    case CANNOT_MODIFY:
                        Lang.COMMAND_DATA_ERROR_MODIFY.sendTo(sender);
                        break;
                    default:
                        break;
                }
            } else if (getResult) {
                final long time = System.currentTimeMillis() - before;
                Lang.COMMAND_DATA_EDIT.sendTo(sender, args[1], args[3], args[4], result, plugin.getDataCore().getDataValue(type, name, args[3], args[4]), time);
            }
        }
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
            return new ArrayList<>(plugin.getDataCore().getDatabases().keySet());
        }
        if (args.length == 5) {
            return new ArrayList<>(plugin.getDataCore().getDataTypes().keySet());
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
