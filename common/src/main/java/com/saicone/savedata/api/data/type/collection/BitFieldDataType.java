package com.saicone.savedata.api.data.type.collection;

import com.saicone.savedata.api.data.DataType;
import com.saicone.savedata.api.data.type.CollectionDataType;
import com.saicone.types.TypeParser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigInteger;

public class BitFieldDataType extends CollectionDataType<BigInteger, Integer> {

    public BitFieldDataType(@NotNull String id, @NotNull TypeParser<BigInteger> parser, @NotNull TypeParser<Integer> elementParser, @Nullable BigInteger defaultValue, @Nullable String permission, @Nullable String expression, boolean userParseable) {
        super(id, parser, elementParser, defaultValue, permission, expression, userParseable);
    }

    @NotNull
    public static Builder<BigInteger> builder(@NotNull String id, @NotNull TypeParser<BigInteger> parser, @NotNull TypeParser<Integer> elementParser) {
        return new Builder<>(id, parser) {
            @Override
            public @NotNull DataType<BigInteger> build() {
                return new BitFieldDataType(id(), parser(), elementParser, defaultValue(), permission(), expression(), userParseable());
            }
        };
    }

    @Override
    public @Nullable BigInteger getDefaultValue() {
        final BigInteger i = super.getDefaultValue();
        return i == null ? BigInteger.valueOf(0L) : i;
    }

    @Override
    public boolean test(@NotNull BigInteger a, @NotNull Object b) {
        return a.testBit(loadElement(b));
    }

    @Override
    public @NotNull BigInteger add(@NotNull BigInteger a, @NotNull Object b) {
        return a.setBit(loadElement(b));
    }

    @Override
    public @NotNull BigInteger remove(@NotNull BigInteger a, @NotNull Object b) {
        return a.clearBit(loadElement(b));
    }
}
