package com.saicone.savedata;

import com.saicone.ezlib.Dependencies;
import com.saicone.ezlib.Dependency;
import com.saicone.ezlib.EzlibLoader;
import com.saicone.mcode.util.Strings;
import com.saicone.savedata.core.data.DataCore;
import com.saicone.settings.Settings;
import com.saicone.settings.SettingsData;
import com.saicone.settings.data.DataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

@Dependencies(value = {
        // Javatuples
        @Dependency(value = "org.javatuples:javatuples:1.2", relocate = {"org.javatuples", "{package}.libs.javatuples"}),
        // Hikari
        @Dependency(value = "com.zaxxer:HikariCP:5.1.0", relocate = {
                "com.zaxxer.hikari", "{package}.libs.hikari",
                "org.slf4j", "{package}.libs.slf4j"
        }),
        @Dependency(value = "org.slf4j:slf4j-nop:1.7.36", relocate = {"org.slf4j", "{package}.libs.slf4j"}),
        // EvalEx
        @Dependency(value = "com.ezylang:EvalEx:3.2.0", relocate = {"com.ezylang.evalex", "{package}.libs.evalex"}),
        // Settings
        @Dependency("com.saicone.settings:settings:1.0"),
        @Dependency("com.saicone.settings:settings-gson:1.0"),
        @Dependency("com.saicone.settings:settings-hocon:1.0"),
        @Dependency("com.saicone.settings:settings-toml:1.0"),
        @Dependency(value = "com.saicone.settings:settings-yaml:1.0", transitive = false),
        // Delivery4j
        @Dependency("com.saicone.delivery4j:delivery4j:1.0"),
        @Dependency(value = "com.saicone.delivery4j:delivery4j-hikari:1.0", transitive = false)
}, relocations = {
        "com.saicone.types", "{package}.libs.types",
        "com.saicone.settings", "{package}.libs.settings",
        "com.saicone.delivery4j", "{package}.libs.delivery4j"
})
public class SaveData {

    private static SaveData instance;

    private final SaveDataPlugin plugin;
    private final EzlibLoader libraryLoader;
    private final SettingsData<Settings> settings;
    private final DataCore dataCore;

    private int logLevel = 3;

    static void init(@NotNull SaveData instance) {
        if (SaveData.instance != null) {
            throw new RuntimeException("SaveData is already initialized");
        }
        SaveData.instance = instance;
    }

    @NotNull
    public static SaveData get() {
        return instance;
    }

    @NotNull
    public static SaveDataPlugin plugin() {
        return instance.plugin;
    }

    @NotNull
    public static Settings settings() {
        return instance.settings.getLoaded();
    }

    public static void log(int level, @NotNull Supplier<String> msg) {
        if (level > get().logLevel) {
            return;
        }
        plugin().log(level, msg);
    }

    public static void log(int level, @NotNull String msg, @Nullable Object... args) {
        if (level > get().logLevel) {
            return;
        }
        plugin().log(level, () -> Strings.replaceArgs(msg, args));
    }

    public static void logException(int level, @NotNull Throwable throwable) {
        if (level > get().logLevel) {
            return;
        }
        plugin().logException(level, throwable);
    }

    public static void logException(int level, @NotNull Throwable throwable, @NotNull Supplier<String> msg) {
        if (level > get().logLevel) {
            return;
        }
        plugin().logException(level, throwable, msg);
    }

    public static void logException(int level, @NotNull Throwable throwable, @NotNull String msg, @Nullable Object... args) {
        if (level > get().logLevel) {
            return;
        }
        plugin().logException(level, throwable, () -> Strings.replaceArgs(msg, args));
    }

    public SaveData(@NotNull SaveDataPlugin plugin) {
        this.plugin = plugin;
        this.libraryLoader = new EzlibLoader()
                .logger((level, msg) -> {
                    if (level < 4) {
                        plugin.log(level, () -> msg);
                    }
                })
                .replace("{package}", "com.saicone.savedata")
                .load();
        this.settings = SettingsData.of("settings.yml").or(DataType.INPUT_STREAM, "settings.yml");
        this.dataCore = new DataCore();
    }

    public void onLoad() {
        this.settings.load(this.plugin.getFolder().toFile());
        logLevel = settings().getIgnoreCase("plugin", "loglevel").asInt(3);
        this.dataCore.onLoad();
    }

    public void onEnable() {
        this.dataCore.onEnable();
    }

    public void onDisable() {
        this.dataCore.onDisable();
    }

    public void onReload() {
        this.settings.load(this.plugin.getFolder().toFile());
        logLevel = settings().getIgnoreCase("plugin", "loglevel").asInt(3);
        this.dataCore.onReload();
    }

    @NotNull
    public SaveDataPlugin getPlugin() {
        return plugin;
    }

    @NotNull
    public EzlibLoader getLibraryLoader() {
        return libraryLoader;
    }

    @NotNull
    public SettingsData<Settings> getSettings() {
        return settings;
    }

    @NotNull
    public DataCore getDataCore() {
        return dataCore;
    }
}
