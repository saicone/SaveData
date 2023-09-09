package com.saicone.savedata.core.data;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

public class NumberDataType<T extends Number> extends DataType<T> {


    private static final Map<Class<? extends Number>, Number> MIN_VALUES = new HashMap<>();
    private static final Map<Class<? extends Number>, Number> MAX_VALUES = new HashMap<>();
    private static final Map<Class<? extends Number>, Function<Number, ? extends Number>> TYPE_VALUES = new HashMap<>();

    static {
        MIN_VALUES.put(Byte.class, Byte.MIN_VALUE);
        MIN_VALUES.put(byte.class, Byte.MIN_VALUE);
        MIN_VALUES.put(Short.class, Short.MIN_VALUE);
        MIN_VALUES.put(short.class, Short.MIN_VALUE);
        MIN_VALUES.put(Integer.class, Integer.MIN_VALUE);
        MIN_VALUES.put(int.class, Integer.MIN_VALUE);
        MIN_VALUES.put(Float.class, Float.MIN_VALUE);
        MIN_VALUES.put(float.class, Float.MIN_VALUE);
        MIN_VALUES.put(Long.class, Long.MIN_VALUE);
        MIN_VALUES.put(long.class, Long.MIN_VALUE);
        MIN_VALUES.put(Double.class, Double.MIN_VALUE);
        MIN_VALUES.put(double.class, Double.MIN_VALUE);

        MAX_VALUES.put(Byte.class, Byte.MAX_VALUE);
        MAX_VALUES.put(byte.class, Byte.MAX_VALUE);
        MAX_VALUES.put(Short.class, Short.MAX_VALUE);
        MAX_VALUES.put(short.class, Short.MAX_VALUE);
        MAX_VALUES.put(Integer.class, Integer.MAX_VALUE);
        MAX_VALUES.put(int.class, Integer.MAX_VALUE);
        MAX_VALUES.put(Float.class, Float.MAX_VALUE);
        MAX_VALUES.put(float.class, Float.MAX_VALUE);
        MAX_VALUES.put(Long.class, Long.MAX_VALUE);
        MAX_VALUES.put(long.class, Long.MAX_VALUE);
        MAX_VALUES.put(Double.class, Double.MAX_VALUE);
        MAX_VALUES.put(double.class, Double.MAX_VALUE);

        TYPE_VALUES.put(Byte.class, Number::byteValue);
        TYPE_VALUES.put(byte.class, Number::byteValue);
        TYPE_VALUES.put(Short.class, Number::shortValue);
        TYPE_VALUES.put(short.class, Number::shortValue);
        TYPE_VALUES.put(Integer.class, Number::intValue);
        TYPE_VALUES.put(int.class, Number::intValue);
        TYPE_VALUES.put(Float.class, Number::floatValue);
        TYPE_VALUES.put(float.class, Number::floatValue);
        TYPE_VALUES.put(Long.class, Number::longValue);
        TYPE_VALUES.put(long.class, Number::longValue);
        TYPE_VALUES.put(Double.class, Number::doubleValue);
        TYPE_VALUES.put(double.class, Number::doubleValue);
    }

    @NotNull
    @SuppressWarnings("unchecked")
    public static <T extends Number> T minValue(@NotNull Class<T> type) {
        return (T) Objects.requireNonNull(MIN_VALUES.get(type), "The class " + type.getName() + " doesn't have a minimum value");
    }

    @NotNull
    @SuppressWarnings("unchecked")
    public static <T extends Number> T maxValue(@NotNull Class<T> type) {
        return (T) Objects.requireNonNull(MAX_VALUES.get(type), "The class " + type.getName() + " doesn't have a maximum value");
    }

    @NotNull
    @SuppressWarnings("unchecked")
    public static <T extends Number> Function<Number, T> typeFunction(@NotNull Class<T> type) {
        return (Function<Number, T>) Objects.requireNonNull(TYPE_VALUES.get(type), "The class " + type.getName() + " doesn't have a type value function");
    }

    @NotNull
    @SuppressWarnings("unchecked")
    public static <T extends Number> T typeValue(@NotNull Class<T> type, @NotNull Number number) {
        return typeFunction(type).apply(number);
    }

    private final T min;
    private final T max;
    private final String format;

    private final T minValue;
    private final T maxValue;
    private final Function<Number, T> typeFunction;
    private final DecimalFormat decimalFormat;

    public NumberDataType(@NotNull String id, @NotNull Class<T> typeClass, @Nullable T defaultValue, @Nullable String permission, @Nullable T min, @Nullable T max, @Nullable String format) {
        super(id, typeClass, defaultValue, permission);
        this.min = min;
        this.max = max;
        this.format = format;
        this.minValue = minValue(typeClass);
        this.maxValue = maxValue(typeClass);
        this.typeFunction = typeFunction(typeClass);
        this.decimalFormat = format == null ? null : new DecimalFormat(format);
    }

    @NotNull
    public T getMin() {
        return min == null ? minValue : min;
    }

    @NotNull
    public T getMax() {
        return max == null ? maxValue : max;
    }

    @NotNull
    public Function<Number, T> getTypeFunction() {
        return typeFunction;
    }

    @Nullable
    public String getFormat() {
        return format;
    }

    @Override
    public @NotNull String asString(@NotNull T t) {
        return decimalFormat == null ? super.asString(t) : decimalFormat.format(t.doubleValue());
    }

    @Override
    public @NotNull T wrap(@NotNull T t) {
        if (max == null && min == null) {
            return t;
        }
        final double value = t.doubleValue();
        if (max != null) {
            if (value >= max.doubleValue()) {
                return max;
            }
        } else if (value <= min.doubleValue()) {
            return min;
        }
        return t;
    }

    @NotNull
    public T wrap(double t) {
        final T max = getMax();
        if (t >= max.doubleValue()) {
            return max;
        }
        final T min = getMin();
        if (t <= min.doubleValue()) {
            return min;
        }
        return typeFunction.apply(t);
    }

    @Override
    public @NotNull T add(@NotNull T a, @NotNull T b) {
        if (a.equals(maxValue)) {
            return wrap(a);
        }
        try {
            // Overflow check
            Math.addExact(a.longValue(), b.longValue());
            return wrap(a.doubleValue() + b.doubleValue());
        } catch (ArithmeticException e) {
            return getMax();
        }
    }

    @Override
    public @NotNull T substract(@NotNull T a, @NotNull T b) {
        if (a.equals(minValue)) {
            return wrap(a);
        }
        try {
            // Overflow check
            Math.subtractExact(a.longValue(), b.longValue());
            return wrap(a.doubleValue() - b.doubleValue());
        } catch (ArithmeticException e) {
            return getMin();
        }
    }

    @NotNull
    public T multiply(@NotNull T a, @NotNull T b) {
        if (a.equals(maxValue)) {
            return wrap(a);
        }
        try {
            // Overflow check
            final long r = Math.multiplyExact(a.longValue(), b.longValue());
            if (r == 0) {
                return typeFunction.apply(0);
            }
            return wrap(a.doubleValue() * b.doubleValue());
        } catch (ArithmeticException e) {
            return getMax();
        }
    }

    @NotNull
    public T divide(@NotNull T a, @NotNull T b) {
        final double x = a.doubleValue();
        final double y = b.doubleValue();
        if (x == 0.0 || y == 0.0) {
            return typeFunction.apply(0);
        }
        return wrap(x / y);
    }
}
