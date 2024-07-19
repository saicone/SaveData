package com.saicone.savedata.api.data.type.collection;

import com.saicone.savedata.api.data.DataType;
import com.saicone.savedata.api.data.type.CollectionDataType;
import com.saicone.types.TypeParser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ListDataType<E> extends CollectionDataType<List<E>, E> {

    public ListDataType(@NotNull String id, @NotNull TypeParser<List<E>> parser, @NotNull TypeParser<E> elementParser, @Nullable List<E> defaultValue, @Nullable String permission, @Nullable String expression, boolean userParseable) {
        super(id, parser, elementParser, defaultValue, permission, expression, userParseable);
    }

    @NotNull
    public static <E> Builder<List<E>> builder(@NotNull String id, @NotNull TypeParser<List<E>> parser, @NotNull TypeParser<E> elementParser) {
        return new Builder<>(id, parser) {
            @Override
            public @NotNull DataType<List<E>> build() {
                return new ListDataType<>(id(), parser(), elementParser, defaultValue(), permission(), expression(), userParseable());
            }
        };
    }

    @Override
    public boolean test(@NotNull List<E> a, @NotNull Object b) {
        return a.contains(loadElement(b));
    }

    @Override
    public @NotNull List<E> add(@NotNull List<E> a, @NotNull Object b) {
        a.add(loadElement(b));
        return a;
    }

    @Override
    public @NotNull List<E> remove(@NotNull List<E> a, @NotNull Object b) {
        a.remove(loadElement(b));
        return a;
    }
}
