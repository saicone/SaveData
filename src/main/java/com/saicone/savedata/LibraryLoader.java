package com.saicone.savedata;

import com.saicone.ezlib.EzlibLoader;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;

public class LibraryLoader extends EzlibLoader {

    private static final Dependency HIKARI = new Dependency()
            .path("com{}zaxxer:HikariCP:5.0.1")
            .relocate("com{}zaxxer{}hikari", "com{}saicone{}savedata{}libs{}hikari");
    private static final Dependency EVAL_EX = new Dependency()
            .path("com{}ezylang:EvalEx:3.2.0")
            .relocate("com{}ezylang{}evalex", "com{}saicone{}savedata{}libs{}evalex");

    public LibraryLoader() {
        super(new String[] { null });
        logger(SaveData::log);
    }

    public boolean loadDependency(@NotNull String path) {
        SaveData.log(4, "The library '" + path + "' was read");
        return super.loadDependency(new Dependency().path(path));
    }

    public boolean loadDependency(@NotNull ConfigurationSection section) {
        final Dependency dependency = new Dependency();
        dependency.path(section.getString("path", ""));
        if (section.contains("repository")) {
            dependency.repository(section.getString("repository", ""));
        }
        dependency.inner(section.getBoolean("inner", false));
        dependency.transitive(section.getBoolean("transitive", true));
        dependency.snapshot(section.getBoolean("snapshot", false));
        dependency.loadOptional(section.getBoolean("loadOptional", false));
        dependency.optional(section.getBoolean("optional", false));
        if (section.contains("scopes")) {
            dependency.scopes(new HashSet<>(section.getStringList("scopes")));
        }
        if (section.contains("test")) {
            dependency.test(new HashSet<>(section.getStringList("test")));
        }
        if (section.contains("condition")) {
            dependency.condition(new HashSet<>(section.getStringList("condition")));
        }
        if (section.contains("exclude")) {
            dependency.exclude(new HashSet<>(section.getStringList("exclude")));
        }
        if (section.contains("relocate")) {
            for (Object relocate : section.getList("relocate", new ArrayList<>())) {
                if (relocate instanceof Map) {
                    dependency.relocate(String.valueOf(((Map<?, ?>) relocate).get("from")), String.valueOf(((Map<?, ?>) relocate).get("to")));
                }
            }
        }
        SaveData.log(4, "The library '" + section.getString("path", "") + "' was read");
        return loadDependency(dependency);
    }
}
