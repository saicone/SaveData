package com.saicone.savedata.api.data;

import com.saicone.savedata.api.data.type.BigDecimalDataType;
import com.saicone.savedata.api.data.type.BigIntegerDataType;
import com.saicone.savedata.api.data.type.collection.BitFieldDataType;
import com.saicone.savedata.api.data.type.collection.ListDataType;
import com.saicone.savedata.api.data.type.collection.SetDataType;
import com.saicone.types.TypeParser;
import com.saicone.types.Types;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Objects;
import java.util.function.Function;

public class DataType<T> {

    private final String id;
    private final TypeParser<T> parser;
    private final T defaultValue;
    private final String permission;
    private final String expression;
    private final boolean userParseable;

    public DataType(@NotNull String id, @NotNull TypeParser<T> parser, @Nullable T defaultValue, @Nullable String permission, @Nullable String expression, boolean userParseable) {
        this.id = id;
        this.parser = parser;
        this.defaultValue = defaultValue;
        this.permission = permission;
        this.expression = expression;
        this.userParseable = userParseable;
    }

    @NotNull
    public static Builder<?> builder(@NotNull String id, @NotNull String type) {
        switch (type.toLowerCase()) {
            case "text":
                return builder(id, Types.TEXT);
            case "string":
                return builder(id, Types.STRING);
            case "char":
            case "character":
                return builder(id, Types.CHAR);
            case "bool":
            case "boolean":
                return builder(id, Types.BOOLEAN);
            case "byte":
                return BigIntegerDataType.builder(id, Types.BIG_INTEGER, BigInteger.valueOf(Byte.MIN_VALUE), BigInteger.valueOf(Byte.MAX_VALUE));
            case "short":
                return BigIntegerDataType.builder(id, Types.BIG_INTEGER, BigInteger.valueOf(Short.MIN_VALUE), BigInteger.valueOf(Short.MAX_VALUE));
            case "int":
            case "integer":
                return BigIntegerDataType.builder(id, Types.BIG_INTEGER, BigInteger.valueOf(Integer.MIN_VALUE), BigInteger.valueOf(Integer.MAX_VALUE));
            case "float":
                return BigDecimalDataType.builder(id, Types.BIG_DECIMAL, BigDecimal.valueOf(Float.MIN_VALUE), BigDecimal.valueOf(Float.MAX_VALUE));
            case "long":
                return BigIntegerDataType.builder(id, Types.BIG_INTEGER, BigInteger.valueOf(Long.MIN_VALUE), BigInteger.valueOf(Long.MAX_VALUE));
            case "double":
                return BigDecimalDataType.builder(id, Types.BIG_DECIMAL, BigDecimal.valueOf(Double.MIN_VALUE), BigDecimal.valueOf(Double.MAX_VALUE));
            case "number":
            case "biginteger":
                return BigIntegerDataType.builder(id, Types.BIG_INTEGER, null, null);
            case "bigdecimal":
                return BigDecimalDataType.builder(id, Types.BIG_DECIMAL, null, null);
            case "uuid":
                return builder(id, Types.UUID);
            case "url":
                return builder(id, Types.URL);
            case "date":
            case "localdate":
                return builder(id, Types.LOCAL_DATE);
            case "time":
            case "localtime":
                return builder(id, Types.LOCAL_TIME);
            case "datetime":
            case "localdatetime":
                return builder(id, Types.LOCAL_DATE_TIME);
            case "bitfield":
                return BitFieldDataType.builder(id, Types.BIG_INTEGER, Types.INTEGER);
            default:
                if (type.contains("<") && type.endsWith(">")) {
                    final int index = type.indexOf('<');
                    final TypeParser<Object> elementParser = (TypeParser<Object>) parser(type.substring(index + 1, type.length() - 1));
                    switch (type.substring(0, index).toLowerCase()) {
                        case "collection":
                        case "list":
                        case "array":
                        case "arraylist":
                            return ListDataType.builder(id, TypeParser.collection(elementParser, ArrayList::new), elementParser);
                        case "set":
                        case "hashset":
                            return SetDataType.builder(id, TypeParser.collection(elementParser, HashSet::new), elementParser);
                    }
                }
                if (type.contains(".")) {
                    try {
                        return builder(id, Class.forName(type));
                    } catch (ClassNotFoundException ignored) { }
                }
                return builder(id, Types.of(type));
        }
    }

    @NotNull
    @SuppressWarnings("unchecked")
    public static <T> Builder<T> builder(@NotNull String id, @NotNull Object type) {
        return new Builder<>(id, type instanceof TypeParser ? (TypeParser<T>) type : Types.of(type));
    }

    @NotNull
    public static TypeParser<?> parser(@NotNull String type) {
        switch (type.toLowerCase()) {
            case "text":
                return Types.TEXT;
            case "string":
                return Types.STRING;
            case "char":
            case "character":
                return Types.CHAR;
            case "bool":
            case "boolean":
                return Types.BOOLEAN;
            case "byte":
                return Types.BYTE;
            case "short":
                return Types.SHORT;
            case "int":
            case "integer":
                return Types.INTEGER;
            case "float":
                return Types.FLOAT;
            case "long":
                return Types.LONG;
            case "double":
                return Types.DOUBLE;
            case "number":
            case "biginteger":
                return Types.BIG_INTEGER;
            case "bigdecimal":
                return Types.BIG_DECIMAL;
            case "uuid":
                return Types.UUID;
            case "url":
                return Types.URL;
            case "date":
            case "localdate":
                return Types.LOCAL_DATE;
            case "time":
            case "localtime":
                return Types.LOCAL_TIME;
            case "datetime":
            case "localdatetime":
                return Types.LOCAL_DATE_TIME;
            default:
                if (type.contains(".")) {
                    try {
                        return Types.of(Class.forName(type));
                    } catch (ClassNotFoundException ignored) { }
                }
                return Types.of(type);
        }
    }

    public boolean isUserParseable() {
        return userParseable;
    }

    @Nullable
    public String getTypeName() {
        final Type type = parser.getType();
        if (type instanceof Class) {
            return ((Class<?>) type).getName();
        }
        return null;
    }

    @NotNull
    public String getId() {
        return id;
    }

    @NotNull
    public TypeParser<T> getParser() {
        return parser;
    }

    @Nullable
    public T getDefaultValue() {
        return defaultValue;
    }

    @Nullable
    public String getPermission() {
        return permission;
    }

    @Nullable
    public String getExpression() {
        return expression;
    }

    @NotNull
    public T parse(@Nullable Object object, @NotNull Function<String, String> userParser) {
        return load(userParseable && object instanceof String ? userParser.apply((String) object) : object);
    }

    @NotNull
    public Object eval(@NotNull T t) {
        return t;
    }

    @NotNull
    public T load(@Nullable Object object) {
        Objects.requireNonNull(object, "The " + this.getClass().getSimpleName() + " instance doesn't allow null objects");
        final T t = parser.parse(object);
        if (t == null) {
            throw new IllegalArgumentException("The object '" + object + "' cannot be loaded by " + this.getClass().getSimpleName() + " instance");
        }
        return t;
    }

    @NotNull
    public String save(@NotNull T t) {
        return t.toString();
    }

    public boolean test(@NotNull T a, @NotNull Object b) {
        throw new UnsupportedOperationException("The " + this.getClass().getSimpleName() + " instance cannot make a test");
    }

    @NotNull
    public T add(@NotNull T a, @NotNull Object b) {
        throw new UnsupportedOperationException("The " + this.getClass().getSimpleName() + " instance cannot make an addition");
    }

    @NotNull
    public T remove(@NotNull T a, @NotNull Object b) {
        throw new UnsupportedOperationException("The " + this.getClass().getSimpleName() + " instance cannot make a subtraction");
    }

    @NotNull
    public T multiply(@NotNull T a, @NotNull Object b) {
        throw new UnsupportedOperationException("The " + this.getClass().getSimpleName() + " instance cannot make a multiplication");
    }

    @NotNull
    public T divide(@NotNull T a, @NotNull Object b) {
        throw new UnsupportedOperationException("The " + this.getClass().getSimpleName() + " instance cannot make a division");
    }

    @Override
    public String toString() {
        return "DataType{" +
                "id='" + id + '\'' +
                ", parser=" + (parser.getType() != null ? parser.getType().getTypeName() : parser) +
                ", defaultValue=" + defaultValue +
                ", permission='" + permission + '\'' +
                ", expression='" + expression + '\'' +
                ", userParseable=" + userParseable +
                '}';
    }

    public static class Builder<T> {
        private final String id;
        private final TypeParser<T> parser;

        private T defaultValue;
        private String permission;
        private String expression;
        private boolean userParseable;

        // String
        private boolean colored;

        // Number
        private T min;
        private T max;
        private String format; // Decimal

        public Builder(@NotNull String id, @NotNull TypeParser<T> parser) {
            this.id = id;
            this.parser = parser;
        }

        @NotNull
        public String id() {
            return id;
        }

        @NotNull
        public TypeParser<T> parser() {
            return parser;
        }

        @Nullable
        public T defaultValue() {
            return defaultValue;
        }

        @NotNull
        @Contract("_ -> this")
        public Builder<T> defaultValue(@Nullable T defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        @Nullable
        public String permission() {
            return permission;
        }

        @NotNull
        @Contract("_ -> this")
        public Builder<T> permission(@Nullable String permission) {
            this.permission = permission;
            return this;
        }

        @Nullable
        public String expression() {
            return expression;
        }

        @NotNull
        @Contract("_ -> this")
        public Builder<T> expression(@Nullable String expression) {
            this.expression = expression;
            return this;
        }

        public boolean userParseable() {
            return userParseable;
        }

        @NotNull
        @Contract("_ -> this")
        public Builder<T> userParseable(boolean userParseable) {
            this.userParseable = userParseable;
            return this;
        }

        public boolean colored() {
            return colored;
        }

        @NotNull
        @Contract("_ -> this")
        public Builder<T> colored(boolean colored) {
            this.colored = colored;
            return this;
        }

        @Nullable
        public T min() {
            return min;
        }

        @NotNull
        @Contract("_ -> this")
        public Builder<T> min(@Nullable T min) {
            this.min = min;
            return this;
        }

        @Nullable
        public T max() {
            return max;
        }

        @NotNull
        @Contract("_ -> this")
        public Builder<T> max(@Nullable T max) {
            this.max = max;
            return this;
        }

        @Nullable
        public String format() {
            return format;
        }

        @NotNull
        @Contract("_ -> this")
        public Builder<T> format(@Nullable String format) {
            this.format = format;
            return this;
        }

        @NotNull
        public DataType<T> build() {
            return new DataType<>(id, parser, defaultValue, permission, expression, userParseable);
        }
    }
}
