package com.saicone.savedata.api.data.type;

import com.ezylang.evalex.EvaluationException;
import com.ezylang.evalex.Expression;
import com.ezylang.evalex.parser.ParseException;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.saicone.savedata.api.data.DataType;
import com.saicone.types.TypeParser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public abstract class NumberDataType<T extends Number> extends DataType<T> implements Comparator<T> {

    private static final Cache<String, Number> RESULT_CACHE = CacheBuilder.newBuilder().expireAfterAccess(1, TimeUnit.MINUTES).build();

    private final T min;
    private final T max;

    public NumberDataType(@NotNull String id, @NotNull TypeParser<T> parser, @Nullable T defaultValue, @Nullable String permission, @Nullable String expression, boolean userParseable, @Nullable T min, @Nullable T max) {
        super(id, parser, defaultValue, permission, expression, userParseable);
        this.min = min;
        this.max = max;
    }

    public boolean hasMin() {
        return min != null;
    }

    public boolean hasMax() {
        return max != null;
    }

    @NotNull
    public T getMin() {
        return min == null ? getMinValue() : min;
    }

    @NotNull
    protected T getMinValue() throws NullPointerException {
        throw new NullPointerException("The " + this.getClass().getSimpleName() + " instance doesn't have a minimum value");
    }

    @NotNull
    public T getMax() {
        return max == null ? getMaxValue() : max;
    }

    @NotNull
    protected T getMaxValue() throws NullPointerException {
        throw new NullPointerException("The " + this.getClass().getSimpleName() + " instance doesn't have a maximum value");
    }

    @Override
    public @NotNull T parse(@Nullable Object object, @NotNull Function<String, String> userParser) {
        String s = getExpression();
        if (s == null) {
            return super.parse(object, userParser);
        }
        if (object != null) {
            s = s.replace("{value}", object.toString());
        }
        if (this.isUserParseable()) {
            s = userParser.apply(s);
        }
        Number result = RESULT_CACHE.getIfPresent(s);
        if (result == null) {
            try {
                if (this instanceof BigIntegerDataType) {
                    result = new Expression(s).evaluate().getNumberValue().toBigInteger();
                } else {
                    result = new Expression(s).evaluate().getNumberValue();
                }
            } catch (EvaluationException | ParseException e) {
                throw new RuntimeException(e);
            }
            RESULT_CACHE.put(s, result);
        }
        return load(result);
    }
}
