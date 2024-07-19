package com.saicone.savedata.api.data.type.collection;

import com.saicone.savedata.api.data.DataType;
import com.saicone.savedata.api.data.type.CollectionDataType;
import com.saicone.types.TypeParser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public class SetDataType<E> extends CollectionDataType<Set<E>, E> {

    public SetDataType(@NotNull String id, @NotNull TypeParser<Set<E>> parser, @NotNull TypeParser<E> elementParser, @Nullable Set<E> defaultValue, @Nullable String permission, @Nullable String expression, boolean userParseable) {
        super(id, parser, elementParser, defaultValue, permission, expression, userParseable);
    }

    @NotNull
    public static <E> Builder<Set<E>> builder(@NotNull String id, @NotNull TypeParser<Set<E>> parser, @NotNull TypeParser<E> elementParser) {
        return new Builder<>(id, parser) {
            @Override
            public @NotNull DataType<Set<E>> build() {
                return new SetDataType<>(id(), parser(), elementParser, defaultValue(), permission(), expression(), userParseable());
            }
        };
    }

    @Override
    public boolean test(@NotNull Set<E> a, @NotNull Object b) {
        return a.contains(loadElement(b));
    }

    @Override
    public @NotNull Set<E> add(@NotNull Set<E> a, @NotNull Object b) {
        a.add(loadElement(b));
        return a;
    }

    @Override
    public @NotNull Set<E> remove(@NotNull Set<E> a, @NotNull Object b) {
        a.remove(loadElement(b));
        return a;
    }
}
