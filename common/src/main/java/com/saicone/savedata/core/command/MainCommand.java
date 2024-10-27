package com.saicone.savedata.core.command;

import com.saicone.mcode.module.task.Task;
import com.saicone.savedata.SaveData;
import com.saicone.savedata.api.data.DataOperator;
import com.saicone.savedata.api.data.DataResult;
import com.saicone.savedata.api.data.DataUser;
import com.saicone.savedata.core.Lang;
import org.javatuples.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public interface MainCommand {

    List<String> TYPE = List.of(
            "reload",
            "player",
            "global",
            "server"
    );

    List<String> OPERATOR = List.of(
            "get",
            "contains",
            "delete",
            "set",
            "add",
            "substract",
            "multiply",
            "divide"
    );

    @NotNull
    UUID getUniqueId(@NotNull String name);

    @NotNull
    Function<String, String> getUserParser(@Nullable UUID uniqueId);

    default void run(@NotNull Object sender, @NotNull String cmd, @NotNull String... args) {
        SaveData.log(4, "Before argument check");
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            final long before = System.currentTimeMillis();
            if (args[0].equalsIgnoreCase("reload")) {
                Task.runAsync(() -> {
                    SaveData.reload();
                    final long time = System.currentTimeMillis() - before;
                    Lang.COMMAND_RELOAD.sendTo(sender, time);
                });
                return;
            }
        }

        SaveData.log(4, "Check");
        if (args.length < 4 || !TYPE.contains(args[0].toLowerCase())) {
            Lang.COMMAND_HELP.sendTo(sender, cmd);
            return;
        }

        final UUID uniqueId;
        final String database;
        final String dataType;
        final DataOperator operator;
        final String value;
        final Long expiration;
        final Function<String, String> userParser;
        if (args[0].equalsIgnoreCase("player")) {
            SaveData.log(4, "Using player ID");
            if (args.length < 5 || !OPERATOR.contains(args[4].toLowerCase())) {
                Lang.COMMAND_HELP.sendTo(sender, cmd);
                return;
            }
            SaveData.log(4, "Obtaining player ID using PlayerProvider");
            uniqueId = args[1].contains("-") ? UUID.fromString(args[1]) : getUniqueId(args[1]);
            database = args[2];
            dataType = args[3];
            operator = DataOperator.valueOf(args[4].toUpperCase());
            value = args.length > 5 ? args[5] : null;
            expiration = args.length > 6 ? parseExpiration(String.join(" ", Arrays.copyOfRange(args, 6, args.length))) : null;
            userParser = getUserParser(uniqueId);
        } else {
            SaveData.log(4, "Using server ID");
            if (!OPERATOR.contains(args[3].toLowerCase())) {
                Lang.COMMAND_HELP.sendTo(sender, cmd);
                return;
            }
            uniqueId = DataUser.SERVER_ID;
            database = args[1];
            dataType = args[2];
            operator = DataOperator.valueOf(args[3].toUpperCase());
            value = args.length > 4 ? args[4] : null;
            expiration = args.length > 5 ? parseExpiration(String.join(" ", Arrays.copyOfRange(args, 5, args.length))) : null;
            userParser = getUserParser(null);
        }

        if (!SaveData.get().getDataCore().getDatabases().containsKey(database)) {
            Lang.COMMAND_ERROR_DATABASE.sendTo(sender, database);
            return;
        }

        if (!SaveData.get().getDataCore().getDataTypes().containsKey(dataType)) {
            Lang.COMMAND_ERROR_DATATYPE.sendTo(sender, dataType);
            return;
        }

        if (operator.isEval()) {
            SaveData.log(4, "The operator is an evaluation");
            SaveData.get().getDataCore().userValue(uniqueId, operator, database, dataType, value, userParser).thenAccept(result -> {
                if (operator == DataOperator.GET) {
                    Lang.COMMAND_DATA_GET.sendTo(sender, uniqueId == DataUser.SERVER_ID ? "GLOBAL" : args[1], database, dataType, result);
                } else if (result instanceof Boolean) {
                    Lang.COMMAND_DATA_CONTAINS.sendTo(sender, result);
                } else if (result instanceof DataResult) {
                    Lang.COMMAND_DATA_ERROR_VALUE.sendTo(sender, value);
                }
            });
            return;
        }

        final long before = System.currentTimeMillis();
        final boolean getResult = args[args.length - 1].equalsIgnoreCase("-result");
        SaveData.get().getDataCore().executeUpdate(uniqueId, operator, database, dataType, value, expiration, userParser).thenAccept(result -> {
            if (result instanceof DataResult) {
                switch ((DataResult) result) {
                    case INVALID_OPERATOR:
                        Lang.COMMAND_DATA_ERROR_OPERATOR.sendTo(sender, args[2]);
                        break;
                    case INVALID_TYPE:
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
            } else if (getResult && result instanceof Pair) {
                final long time = System.currentTimeMillis() - before;
                Lang.COMMAND_DATA_EDIT.sendTo(sender, uniqueId == DataUser.SERVER_ID ? "GLOBAL" : args[1], database, dataType, ((Pair<?, ?>) result).getValue0(), ((Pair<?, ?>) result).getValue1(), time);
            }
        });
    }

    @Nullable
    private Long parseExpiration(@NotNull String s) {
        final String[] split = s.split(" ", 2);
        if (split.length < 2) {
            return null;
        }
        try {
            return System.currentTimeMillis() + TimeUnit.valueOf(split[1].toUpperCase()).toMillis(Long.parseLong(split[0]));
        } catch (Throwable t) {
            return null;
        }
    }
}
