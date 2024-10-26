package com.saicone.savedata.core;

import com.saicone.mcode.module.lang.LangSupplier;
import com.saicone.savedata.SaveData;
import com.saicone.settings.SettingsNode;
import com.saicone.types.Types;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Lang implements LangSupplier {

    public static final Path NO_PERMISSION = Path.of("plugin.no-permission");
    public static final Path COMMAND_HELP = Path.of("command.help");
    public static final Path COMMAND_RELOAD = Path.of("command.reload");
    public static final Path COMMAND_DATA_GET = Path.of("command.data.get");
    public static final Path COMMAND_DATA_CONTAINS = Path.of("command.data.contains");
    public static final Path COMMAND_DATA_EDIT = Path.of("command.data.edit");
    public static final Path COMMAND_DATA_ERROR_OPERATOR = Path.of("command.data.error.operator");
    public static final Path COMMAND_DATA_ERROR_ID = Path.of("command.data.error.id");
    public static final Path COMMAND_DATA_ERROR_VALUE = Path.of("command.data.error.value");
    public static final Path COMMAND_DATA_ERROR_MODIFY = Path.of("command.data.error.modify");
    public static final Path COMMAND_ERROR_PLAYER = Path.of("command.error.player");
    public static final Path COMMAND_ERROR_DATABASE = Path.of("command.error.database");
    public static final Path COMMAND_ERROR_DATATYPE = Path.of("command.error.datatype");

    private String consoleLanguage = "en_us";
    private String defaultLanguage = "en_us";
    private final Map<String, String> languageAliases = new HashMap<>();

    @Override
    public void load() {
        this.languageAliases.clear();

        this.consoleLanguage = SaveData.settings().getIgnoreCase("plugin", "language").asString("en_us").toLowerCase();
        this.defaultLanguage = SaveData.settings().getIgnoreCase("lang", "default").asString("en_us").toLowerCase();
        for (Map.Entry<String, SettingsNode> entry : SaveData.settings().getIgnoreCase("lang", "aliases").asMapNode()) {
            for (String locale : entry.getValue().asList(Types.STRING)) {
                this.languageAliases.put(locale, entry.getKey());
            }
        }
    }

    @Override
    public @NotNull String getLanguage() {
        return defaultLanguage;
    }

    @Override
    public @NotNull String getLanguageFor(@Nullable Object object) {
        if (object == null) {
            return consoleLanguage;
        }
        return LangSupplier.super.getLanguageFor(object);
    }

    @Override
    public @NotNull Set<String> getLanguageTypes() {
        return Set.of("en_us", "es_es");
    }

    @Override
    @NotNull
    public Map<String, String> getLanguageAliases() {
        return languageAliases;
    }

    @Override
    public int getLogLevel() {
        return SaveData.get().getLogLevel();
    }
}
