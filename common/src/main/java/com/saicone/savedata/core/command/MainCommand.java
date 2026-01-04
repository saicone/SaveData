package com.saicone.savedata.core.command;

import com.saicone.mcode.module.task.Task;
import com.saicone.mcode.util.Dual;
import com.saicone.savedata.SaveData;
import com.saicone.savedata.api.data.DataOperator;
import com.saicone.savedata.api.data.DataResult;
import com.saicone.savedata.api.data.DataUser;
import com.saicone.savedata.core.Lang;
import com.saicone.savedata.util.DurationFormatter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public interface MainCommand {

    List<String> TYPE = List.of(
            "reload",
            "player",
            "players",
            "global",
            "server"
    );

    List<String> OPERATOR = Arrays.stream(DataOperator.values()).map(value -> value.name().toLowerCase()).collect(Collectors.toList());

    @NotNull
    UUID getUniqueId(@NotNull String name);

    void getUniqueId(@NotNull String condition, @NotNull BiConsumer<UUID, Function<String, String>> consumer);

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

        final Object uniqueId;
        final String database;
        final String dataType;
        final DataOperator operator;
        final Dual<String, Long> value;
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
            operator = DataOperator.of(args[4]);
            value = parseValue(5, args);
            userParser = getUserParser((UUID) uniqueId);
        } else if (args[0].equalsIgnoreCase("players")) {
            SaveData.log(4, "Using player condition");
            if (args.length < 5 || !OPERATOR.contains(args[4].toLowerCase())) {
                Lang.COMMAND_HELP.sendTo(sender, cmd);
                return;
            }
            uniqueId = args[1];
            database = args[2];
            dataType = args[3];
            operator = DataOperator.of(args[4]);
            value = parseValue(5, args);
            userParser = null;
        } else {
            SaveData.log(4, "Using server ID");
            if (!OPERATOR.contains(args[3].toLowerCase())) {
                Lang.COMMAND_HELP.sendTo(sender, cmd);
                return;
            }
            uniqueId = DataUser.SERVER_ID;
            database = args[1];
            dataType = args[2];
            operator = DataOperator.of(args[3]);
            value = parseValue(4, args);
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
            if (!(uniqueId instanceof UUID)) {
                Lang.COMMAND_DATA_ERROR_OPERATOR.sendTo(sender, args[2]);
                return;
            }
            SaveData.get().getDataCore().userValue((UUID) uniqueId, operator, database, dataType, value.getLeft(), userParser, SaveData.get().getLang().getLanguageFor(sender)).thenAccept(result -> {
                if (operator == DataOperator.GET) {
                    Lang.COMMAND_DATA_GET.sendTo(sender, uniqueId == DataUser.SERVER_ID ? "GLOBAL" : args[1], database, dataType, result);
                } else if (result instanceof Boolean) {
                    Lang.COMMAND_DATA_CONTAINS.sendTo(sender, result);
                } else if (result instanceof DataResult) {
                    Lang.COMMAND_DATA_ERROR_VALUE.sendTo(sender, value.getLeft());
                }
            });
            return;
        }

        if (uniqueId instanceof UUID) {
            final long before = System.currentTimeMillis();
            final boolean getResult = args[args.length - 1].equalsIgnoreCase("-result");
            SaveData.get().getDataCore().executeUpdate((UUID) uniqueId, operator, database, dataType, value.getLeft(), value.getRight(), userParser).thenAccept(result -> {
                if (result instanceof DataResult) {
                    switch ((DataResult) result) {
                        case INVALID_OPERATOR:
                            Lang.COMMAND_DATA_ERROR_OPERATOR.sendTo(sender, operator.name());
                            break;
                        case INVALID_TYPE:
                            Lang.COMMAND_DATA_ERROR_ID.sendTo(sender, args[4]);
                            break;
                        case INVALID_VALUE:
                            Lang.COMMAND_DATA_ERROR_VALUE.sendTo(sender, value.getLeft());
                            break;
                        case CANNOT_MODIFY:
                            Lang.COMMAND_DATA_ERROR_MODIFY.sendTo(sender);
                            break;
                        default:
                            break;
                    }
                } else if (getResult && result instanceof Dual) {
                    final long time = System.currentTimeMillis() - before;
                    Lang.COMMAND_DATA_EDIT.sendTo(sender, uniqueId == DataUser.SERVER_ID ? "GLOBAL" : args[1], database, dataType, ((Dual<?, ?>) result).getLeft(), ((Dual<?, ?>) result).getRight(), time);
                }
            });
        } else {
            getUniqueId((String) uniqueId, (uuid, parser) -> {
                SaveData.get().getDataCore().executeUpdate(uuid, operator, database, dataType, value.getLeft(), value.getRight(), parser);
            });
        }
    }

    @NotNull
    private Dual<String, Long> parseValue(int start, @NotNull String... args) {
        if (args.length <= start) {
            return Dual.of(null, null);
        }
        if (args.length <= start + 1) {
            String value = args[start];
            if (value.length() > 2 && value.charAt(0) == '`' && value.charAt(value.length() - 1) == '`') {
                value = value.substring(1, value.length() - 1);
            }
            return Dual.of(value, null);
        }
        if (args[start].startsWith("`")) {
            int end = 0;
            for (int i = start; i < args.length; i++) {
                if (args[i].endsWith("`")) {
                    end = i + 1;
                    break;
                }
            }
            if (end > 0) {
                String value = String.join(" ", Arrays.copyOfRange(args, start, end));
                value = value.substring(1, value.length() - 1);
                final Long expiration = end + 1 >= args.length ? null : parseExpiration(String.join(" ", Arrays.copyOfRange(args, end + 1, args.length)));
                return Dual.of(value, expiration);
            }
        }
        return Dual.of(args[start], parseExpiration(String.join(" ", Arrays.copyOfRange(args, start + 1, args.length))));
    }

    @Nullable
    private Long parseExpiration(@NotNull String expiration) {
        try {
            final long time = DurationFormatter.format(expiration, TimeUnit.MILLISECONDS);
            if (time <= 0L) {
                return null;
            }
            return System.currentTimeMillis() + time;
        } catch (IllegalArgumentException e) {
            SaveData.log(2, "Cannot parse expiration '" + expiration + "', if you are trying to set a String with spaces encapsulate it inside ``, for example `this is a string with spaces`");
            return null;
        }
    }
}
