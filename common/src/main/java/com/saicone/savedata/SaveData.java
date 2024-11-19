package com.saicone.savedata;

import com.saicone.ezlib.Dependencies;
import com.saicone.ezlib.Dependency;
import com.saicone.mcode.Plugin;
import com.saicone.mcode.module.lang.AbstractLang;
import com.saicone.savedata.core.data.DataCore;
import com.saicone.settings.Settings;
import com.saicone.settings.SettingsData;
import com.saicone.settings.data.DataType;
import org.jetbrains.annotations.NotNull;

@Dependencies(value = {
        // Javatuples
        @Dependency(value = "org.javatuples:javatuples:1.2", relocate = {"org.javatuples", "{package}.libs.javatuples"}),
        // Hikari
        @Dependency(value = "com.zaxxer:HikariCP:6.1.0", relocate = {
                "com.zaxxer.hikari", "{package}.libs.hikari",
                "org.slf4j", "{package}.libs.slf4j"
        }),
        @Dependency(value = "org.slf4j:slf4j-nop:1.7.36", relocate = {"org.slf4j", "{package}.libs.slf4j"}),
        // EvalEx
        @Dependency(value = "com.ezylang:EvalEx:3.2.0", relocate = {"com.ezylang.evalex", "{package}.libs.evalex"}),
        // Settings
        @Dependency("com.saicone.settings:settings:1.0.1"),
        @Dependency("com.saicone.settings:settings-gson:1.0.1"),
        @Dependency("com.saicone.settings:settings-hocon:1.0.1"),
        @Dependency("com.saicone.settings:settings-toml:1.0.1"),
        @Dependency(value = "com.saicone.settings:settings-yaml:1.0.1", transitive = false),
        // Delivery4j
        @Dependency("com.saicone.delivery4j:delivery4j:1.1"),
        @Dependency(value = "com.saicone.delivery4j:broker-sql-hikari:1.1", transitive = false)
}, relocations = {
        "com.saicone.types", "{package}.libs.types",
        "com.saicone.settings", "{package}.libs.settings",
        "com.saicone.delivery4j", "{package}.libs.delivery4j"
})
public abstract class SaveData extends Plugin {

    private static SaveData instance;

    private final SettingsData<Settings> settings;
    private final AbstractLang<?> lang;
    private final DataCore dataCore;

    @NotNull
    public static SaveData get() {
        return instance;
    }

    @NotNull
    public static Settings settings() {
        return instance.settings.getLoaded();
    }

    public SaveData() {
        if (SaveData.instance != null) {
            throw new RuntimeException("SaveData is already initialized");
        }
        instance = this;
        this.settings = SettingsData.of("settings.yml").or(DataType.INPUT_STREAM, "settings.yml");
        this.lang = initLang();
        this.lang.setUseSettings(true);
        this.dataCore = new DataCore();
    }

    @NotNull
    protected abstract AbstractLang<?> initLang();

    @Override
    public void onLoad() {
        this.settings.load(this.getFolder().toFile());
        this.lang.load();
        setLogLevel(settings().getIgnoreCase("plugin", "loglevel").asInt(3));
        this.dataCore.onLoad();
    }

    @Override
    public void onEnable() {
        this.dataCore.onEnable();
    }

    @Override
    public void onDisable() {
        this.dataCore.onDisable();
    }

    @Override
    public void onReload() {
        this.settings.load(this.getFolder().toFile());
        this.lang.load();
        setLogLevel(settings().getIgnoreCase("plugin", "loglevel").asInt(3));
        this.dataCore.onReload();
    }

    @NotNull
    public SettingsData<Settings> getSettings() {
        return settings;
    }

    @NotNull
    public AbstractLang<?> getLang() {
        return lang;
    }

    @NotNull
    public DataCore getDataCore() {
        return dataCore;
    }
}
