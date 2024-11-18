package com.saicone.savedata.api.data.type;

import com.saicone.savedata.api.data.DataType;
import com.saicone.types.TypeParser;
import com.saicone.types.Types;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;

public class BigDecimalDataType extends NumberDataType<BigDecimal> {

    private final String format;

    private transient final DecimalFormat decimalFormat;

    public BigDecimalDataType(@NotNull String id, @Nullable BigDecimal defaultValue, @Nullable String permission, @Nullable String expression, boolean userParseable, @Nullable BigDecimal min, @Nullable BigDecimal max, @Nullable String format) {
        this(id, Types.BIG_DECIMAL, defaultValue, permission, expression, userParseable, min, max, format);
    }

    public BigDecimalDataType(@NotNull String id, @NotNull TypeParser<BigDecimal> parser, @Nullable BigDecimal defaultValue, @Nullable String permission, @Nullable String expression, boolean userParseable, @Nullable BigDecimal min, @Nullable BigDecimal max, @Nullable String format) {
        super(id, parser, defaultValue, permission, expression, userParseable, min, max);
        this.format = format;
        this.decimalFormat = format == null ? null : new DecimalFormat(format);
    }

    @NotNull
    public static Builder<BigDecimal> builder(@NotNull String id, @NotNull TypeParser<BigDecimal> parser, @Nullable BigDecimal min, @Nullable BigDecimal max) {
        return new Builder<>(id, parser) {
            @Override
            public @NotNull DataType<BigDecimal> build() {
                return new BigDecimalDataType(id(), parser(), defaultValue(), permission(), expression(), userParseable(), min(), max(), format());
            }
        }.min(min).max(max);
    }

    @Nullable
    public String getFormat() {
        return format;
    }

    @Override
    public @NotNull Object eval(@NotNull BigDecimal bigDecimal) {
        return decimalFormat == null ? super.eval(bigDecimal) : decimalFormat.format(bigDecimal);
    }

    @Override
    public @NotNull String save(@NotNull BigDecimal t) {
        return t.toString();
    }

    @Override
    public @NotNull BigDecimal add(@NotNull BigDecimal a, @NotNull Object b) {
        return check(a.add(load(b)));
    }

    @Override
    public @NotNull BigDecimal remove(@NotNull BigDecimal a, @NotNull Object b) {
        return check(a.subtract(load(b)));
    }

    @Override
    public @NotNull BigDecimal multiply(@NotNull BigDecimal a, @NotNull Object b) {
        return check(a.multiply(load(b)));
    }

    @Override
    public @NotNull BigDecimal divide(@NotNull BigDecimal a, @NotNull Object b) {
        return check(a.divide(load(b), RoundingMode.HALF_EVEN));
    }

    @NotNull
    private BigDecimal check(@NotNull BigDecimal i) {
        if (hasMin() && i.compareTo(getMin()) < 0) {
            return getMin();
        }
        if (hasMax() && i.compareTo(getMax()) > 0) {
            return getMax();
        }
        return i;
    }

    @Override
    public int compare(BigDecimal o1, BigDecimal o2) {
        return o1.compareTo(o2);
    }
}
