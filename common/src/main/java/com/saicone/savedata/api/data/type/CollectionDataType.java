package com.saicone.savedata.api.data.type;

import com.saicone.savedata.api.data.DataType;
import com.saicone.types.TypeParser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Base64;
import java.util.Objects;

public abstract class CollectionDataType<T, E> extends DataType<T> {

    private final TypeParser<E> elementParser;

    public CollectionDataType(@NotNull String id, @NotNull TypeParser<T> parser, @NotNull TypeParser<E> elementParser, @Nullable T defaultValue, @Nullable String permission, @Nullable String expression, boolean userParseable) {
        super(id, parser, defaultValue, permission, expression, userParseable);
        this.elementParser = elementParser;
    }

    @Override
    public @NotNull T load(@Nullable Object object) {
        if (object == null) {
            return getParser().parse(new Object[0]);
        }
        try (ByteArrayInputStream in = new ByteArrayInputStream(Base64.getDecoder().decode(String.valueOf(object))); ObjectInputStream input = new ObjectInputStream(in)) {
            return getParser().parse(input.readObject());
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @NotNull
    public E loadElement(@Nullable Object object) {
        Objects.requireNonNull(object, "The " + this.getClass().getSimpleName() + " instance doesn't allow null objects");
        final E t = elementParser.parse(object);
        if (t == null) {
            throw new IllegalArgumentException("The object cannot be parse by " + this.getClass().getSimpleName() + " instance");
        }
        return t;
    }

    @Override
    public @NotNull String save(@NotNull T t) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream(); ObjectOutputStream output = new ObjectOutputStream(out)) {
            output.writeObject(t);
            return Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
