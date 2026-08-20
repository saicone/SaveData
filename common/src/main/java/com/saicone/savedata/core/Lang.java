package com.saicone.savedata.core;

import com.saicone.mcode.module.lang.LangSupplier;
import com.saicone.mcode.platform.MC;
import com.saicone.mcode.util.MLocale;
import com.saicone.savedata.SaveData;
import com.saicone.settings.SettingsNode;
import com.saicone.types.Types;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Locale;
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

    private Locale consoleLocale = LangSupplier.DEFAULT_LOCALE;
    private Locale defaultLocale = LangSupplier.DEFAULT_LOCALE;
    private final Set<Locale> defaultLocaleTypes = Set.of(
            MLocale.fromMinecraftLocale("en_us"),
            MLocale.fromMinecraftLocale("es_es")
    );
    private final Map<Locale, Locale> localeAliases = new HashMap<>();

    @Override
    public void load() {
        this.localeAliases.clear();

        this.consoleLocale = MLocale.fromMinecraftLocale(SaveData.settings().getIgnoreCase("plugin", "language").asString("en_us"));
        this.defaultLocale = MLocale.fromMinecraftLocale(SaveData.settings().getIgnoreCase("lang", "default").asString("en_us").toLowerCase());
        for (Map.Entry<String, SettingsNode> entry : SaveData.settings().getIgnoreCase("lang", "aliases").asMapNode()) {
            for (String locale : entry.getValue().asList(Types.STRING)) {
                this.localeAliases.put(MLocale.fromMinecraftLocale(locale), MLocale.fromMinecraftLocale(entry.getKey()));
            }
        }
    }

    @Override
    public @NotNull Locale getDefaultLocale() {
        return defaultLocale;
    }

    @Override
    public @NotNull Locale getHolderLocale(@Nullable Object holder) {
        if (holder == null || MC.version().isOlderThanOrEquals(MC.V_1_11_2)) {
            return consoleLocale;
        } else {
            return LangSupplier.super.getHolderLocale(holder);
        }
    }

    @Override
    public @NotNull Set<Locale> getLocaleTypes() {
        return defaultLocaleTypes;
    }

    @Override
    public @NotNull Map<Locale, Locale> getLocaleAliases() {
        return localeAliases;
    }

    @Override
    public int getLogLevel() {
        return SaveData.get().getLogLevel();
    }
}
