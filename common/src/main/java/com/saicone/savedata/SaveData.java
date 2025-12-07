package com.saicone.savedata;

import com.saicone.ezlib.Dependencies;
import com.saicone.ezlib.Dependency;
import com.saicone.mcode.Plugin;
import com.saicone.mcode.module.lang.AbstractLang;
import com.saicone.savedata.core.data.DataCore;
import com.saicone.savedata.module.settings.DatabaseUpdater;
import com.saicone.settings.Settings;
import com.saicone.settings.SettingsData;
import com.saicone.settings.data.DataType;
import com.saicone.settings.update.NodeUpdate;
import com.saicone.settings.update.SettingsUpdater;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@Dependencies(value = {
        // EvalEx
        @Dependency(value = "com{}ezylang:EvalEx:3.5.0", relocate = {
                "com{}ezylang{}evalex", "{package}.libs.evalex"
        }),
        // Settings
        @Dependency("com{}saicone{}settings:settings:1.0.4"),
        @Dependency("com{}saicone{}settings:settings-gson:1.0.4"),
        @Dependency("com{}saicone{}settings:settings-hocon:1.0.4"),
        @Dependency("com{}saicone{}settings:settings-toml:1.0.4"),
        @Dependency("com{}saicone{}settings:settings-yaml:1.0.4"),
        // Delivery4j
        @Dependency("com{}saicone{}delivery4j:delivery4j:1.1.4"),
        @Dependency(value = "com{}saicone{}delivery4j:broker-sql-hikari:1.1.4",
                relocate = {
                "com{}zaxxer{}hikari", "{package}.libs.hikari",
                "org{}slf4j", "{package}.libs.slf4j"
        }),
        @Dependency(value = "com{}google{}guava:guava:33.5.0-jre", relocate = {
                "com{}google{}common", "{package}.libs.guava"
        }),
        @Dependency(value = "org{}slf4j:slf4j-nop:1.7.36", relocate = {
                "org{}slf4j", "{package}.libs.slf4j"
        }),
}, relocations = {
        "com{}saicone{}types", "{package}.libs.types",
        "com{}saicone{}settings", "{package}.libs.settings",
        "com{}saicone{}delivery4j", "{package}.libs.delivery4j"
})
public abstract class SaveData extends Plugin {

    private static SaveData instance;

    private final SettingsData<Settings> settings;
    private final SettingsUpdater updater;
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
        this.updater = new SettingsUpdater(initUpdates());
        this.lang = initLang();
        this.lang.setUseSettings(true);
        this.dataCore = new DataCore();
    }

    @NotNull
    protected abstract AbstractLang<?> initLang();

    @NotNull
    private List<NodeUpdate> initUpdates() {
        final List<NodeUpdate> updates = new ArrayList<>();
        updates.add(NodeUpdate.move().from("plugin", "log-level").to("Plugin", "LogLevel"));
        updates.add(new DatabaseUpdater());
        initUpdates(updates);
        return updates;
    }

    protected void initUpdates(@NotNull List<NodeUpdate> updates) {
        // empty default method
    }

    @Override
    public void onLoad() {
        this.settings.load(this.getFolder().toFile());
        // TODO: Implement updater after tests
        //if (this.updater.update(this.settings.getLoaded(), null)) {
        //    this.settings.save();
        //}
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
