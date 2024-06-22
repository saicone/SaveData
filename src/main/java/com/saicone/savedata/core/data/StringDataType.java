package com.saicone.savedata.core.data;

import com.saicone.savedata.module.hook.Placeholders;
import com.saicone.savedata.util.MStrings;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class StringDataType extends DataType<String> {

    private final boolean color;

    public StringDataType(@NotNull String id, @Nullable String defaultValue, @Nullable String permission, @Nullable String parse, boolean papi, boolean color) {
        super(id, String.class, defaultValue, permission, parse, papi);
        this.color = color;
    }

    public boolean isColor() {
        return color;
    }

    @Override
    public @NotNull String wrap(@NotNull String s) {
        return color ? MStrings.color(s) : s;
    }

    @Override
    public @NotNull String wrap(@NotNull String s, @Nullable Player player) {
        return wrap(isPapi() && player != null ? Placeholders.parse(player, s) : s);
    }

    @Override
    public @NotNull Object parse(@NotNull String s) {
        return color ? MStrings.color(s) : s;
    }

    @Override
    public @NotNull String add(@NotNull String a, @NotNull String b) {
        return a + b;
    }

    @Override
    public @NotNull String substract(@NotNull String a, @NotNull String b) {
        return a.replace(b, "");
    }
}
