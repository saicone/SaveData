package com.saicone.savedata.module.lang;

import com.saicone.savedata.SaveData;
import com.saicone.savedata.module.settings.BukkitSettings;
import com.saicone.savedata.util.MStrings;
import com.saicone.savedata.util.OptionalType;
import com.saicone.savedata.util.Strings;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.stream.Collectors;

public enum Lang {

    NO_PERMISSION("plugin.no-permission"),
    COMMAND_HELP("command.help"),
    COMMAND_RELOAD("command.reload"),
    COMMAND_DATA_GET("command.data.get"),
    COMMAND_DATA_EDIT("command.data.edit"),
    COMMAND_DATA_ERROR_OPERATOR("command.data.error.operator"),
    COMMAND_DATA_ERROR_ID("command.data.error.id"),
    COMMAND_DATA_ERROR_VALUE("command.data.error.value"),
    COMMAND_DATA_ERROR_MODIFY("command.data.error.modify"),
    COMMAND_ERROR_PLAYER("command.error.player"),
    COMMAND_ERROR_DATABASE("command.error.database"),
    COMMAND_ERROR_DATATYPE("command.error.datatype");

    public static final Lang[] VALUES = values();
    public static final BukkitSettings MESSAGES = new BukkitSettings("messages.yml");

    private final String path;

    private List<String> text;

    Lang(@NotNull String path) {
        this.path = path;
    }

    @NotNull
    public String getPath() {
        return path;
    }

    @NotNull
    public List<String> getText() {
        return text;
    }

    public void sendTo(@NotNull CommandSender sender, @Nullable Object... args) {
        for (String s : getText()) {
            sender.sendMessage(Strings.replaceArgs(s, args));
        }
    }

    public static void onReload() {
        SaveData.log(4, "Reloading messages...");
        MESSAGES.loadFrom(SaveData.get().getDataFolder(), true);
        for (Lang value : VALUES) {
            value.text = MESSAGES.getOptional(value.getPath()).asList(OptionalType::asString).stream().map(MStrings::color).collect(Collectors.toList());
        }
    }
}
