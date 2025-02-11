package com.saicone.savedata.api.data.type;

import com.saicone.mcode.util.text.MStrings;
import com.saicone.savedata.api.data.DataType;
import com.saicone.types.TypeParser;
import com.saicone.types.Types;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class StringDataType extends DataType<String> {

    private final boolean colored;

    public StringDataType(@NotNull String id, @Nullable String defaultValue, @Nullable String permission, @Nullable String expression, boolean userParseable, boolean colored) {
        this(id, Types.STRING, defaultValue, permission, expression, userParseable, colored);
    }

    public StringDataType(@NotNull String id, @NotNull TypeParser<String> parser, @Nullable String defaultValue, @Nullable String permission, @Nullable String expression, boolean userParseable, boolean colored) {
        super(id, parser, defaultValue, permission, expression, userParseable);
        this.colored = colored;
    }

    @NotNull
    public static Builder<String> builder(@NotNull String id, @NotNull TypeParser<String> parser) {
        return new Builder<>(id, parser) {
            @Override
            public @NotNull DataType<String> build() {
                return new StringDataType(id(), parser(), defaultValue(), permission(), expression(), userParseable(), colored());
            }
        };
    }

    public boolean isColored() {
        return colored;
    }

    @Override
    public @NotNull Object eval(@NotNull String s) {
        return colored ? MStrings.color(s) : s;
    }

    @Override
    public boolean test(@NotNull String a, @NotNull Object b) {
        return a.contains(load(b));
    }

    @Override
    public @NotNull String add(@NotNull String a, @NotNull Object b) {
        return a + load(b);
    }

    @Override
    public @NotNull String remove(@NotNull String a, @NotNull Object b) {
        return a.replace(load(b), "");
    }

    @Override
    public @NotNull String multiply(@NotNull String a, @NotNull Object b) {
        return a.repeat(Types.INTEGER.parse(b, 0));
    }
}
