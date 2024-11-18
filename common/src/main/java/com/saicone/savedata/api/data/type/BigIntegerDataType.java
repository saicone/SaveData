package com.saicone.savedata.api.data.type;

import com.saicone.savedata.api.data.DataType;
import com.saicone.types.TypeParser;
import com.saicone.types.Types;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigInteger;

public class BigIntegerDataType extends NumberDataType<BigInteger> {

    public BigIntegerDataType(@NotNull String id, @Nullable BigInteger defaultValue, @Nullable String permission, @Nullable String expression, boolean userParseable, @Nullable BigInteger min, @Nullable BigInteger max) {
        this(id, Types.BIG_INTEGER, defaultValue, permission, expression, userParseable, min, max);
    }

    public BigIntegerDataType(@NotNull String id, @NotNull TypeParser<BigInteger> parser, @Nullable BigInteger defaultValue, @Nullable String permission, @Nullable String expression, boolean userParseable, @Nullable BigInteger min, @Nullable BigInteger max) {
        super(id, parser, defaultValue, permission, expression, userParseable, min, max);
    }

    @NotNull
    public static Builder<BigInteger> builder(@NotNull String id, @NotNull TypeParser<BigInteger> parser, @Nullable BigInteger min, @Nullable BigInteger max) {
        return new Builder<>(id, parser) {
            @Override
            public @NotNull DataType<BigInteger> build() {
                return new BigIntegerDataType(id(), parser(), defaultValue(), permission(), expression(), userParseable(), min(), max());
            }
        }.min(min).max(max);
    }

    @Override
    public @NotNull BigInteger add(@NotNull BigInteger a, @NotNull Object b) {
        return check(a.add(load(b)));
    }

    @Override
    public @NotNull BigInteger remove(@NotNull BigInteger a, @NotNull Object b) {
        return check(a.subtract(load(b)));
    }

    @Override
    public @NotNull BigInteger multiply(@NotNull BigInteger a, @NotNull Object b) {
        return check(a.multiply(load(b)));
    }

    @Override
    public @NotNull BigInteger divide(@NotNull BigInteger a, @NotNull Object b) {
        return check(a.divide(load(b)));
    }

    @NotNull
    private BigInteger check(@NotNull BigInteger i) {
        if (hasMin() && i.compareTo(getMin()) < 0) {
            return getMin();
        }
        if (hasMax() && i.compareTo(getMax()) > 0) {
            return getMax();
        }
        return i;
    }

    @Override
    public int compare(BigInteger o1, BigInteger o2) {
        return o1.compareTo(o2);
    }
}
