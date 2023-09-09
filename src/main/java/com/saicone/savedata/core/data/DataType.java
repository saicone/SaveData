package com.saicone.savedata.core.data;

import com.saicone.savedata.util.OptionalType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DataType<T> {

    private final String id;
    private final Class<T> typeClass;
    private final T defaultValue;
    private final String permission;

    @Nullable
    public static DataType<?> of(@NotNull String type, @NotNull String id, @Nullable Object defaultValue, @Nullable String permission, @NotNull ConfigurationSection config) {
        switch (type) {
            case "string":
            case "text":
                final boolean color = config.getBoolean("color", false);
                final boolean papi = config.getBoolean("papi", false);
                return new StringDataType(id, OptionalType.of(defaultValue).asString(), permission, color, papi);
            case "char":
            case "character":
                return new DataType<>(id, Character.class, OptionalType.of(defaultValue).asChar(), permission);
            case "bool":
            case "boolean":
                return new DataType<>(id, Boolean.class, OptionalType.of(defaultValue).asBoolean(), permission);
            default:
                final Object min = config.get("min");
                final Object max = config.get("max");
                final String format = config.getString("format");
                switch (type) {
                    case "byte":
                        return new NumberDataType<>(id, Byte.class, OptionalType.of(defaultValue).asByte(), permission,
                                OptionalType.of(min).asByte(),
                                OptionalType.of(max).asByte(),
                                format);
                    case "short":
                        return new NumberDataType<>(id, Short.class, OptionalType.of(defaultValue).asShort(), permission,
                                OptionalType.of(min).asShort(),
                                OptionalType.of(max).asShort(),
                                format);
                    case "int":
                    case "integer":
                        return new NumberDataType<>(id, Integer.class, OptionalType.of(defaultValue).asInt(), permission,
                                OptionalType.of(min).asInt(),
                                OptionalType.of(max).asInt(),
                                format);
                    case "long":
                        return new NumberDataType<>(id, Long.class, OptionalType.of(defaultValue).asLong(), permission,
                                OptionalType.of(min).asLong(),
                                OptionalType.of(max).asLong(),
                                format);
                    case "float":
                        return new NumberDataType<>(id, Float.class, OptionalType.of(defaultValue).asFloat(), permission,
                                OptionalType.of(min).asFloat(),
                                OptionalType.of(max).asFloat(),
                                format);
                    case "double":
                        return new NumberDataType<>(id, Double.class, OptionalType.of(defaultValue).asDouble(), permission,
                                OptionalType.of(min).asDouble(),
                                OptionalType.of(max).asDouble(),
                                format);
                    default:
                        return null;
                }
        }
    }

    public DataType(@NotNull String id, @NotNull Class<T> typeClass, @Nullable T defaultValue, @Nullable String permission) {
        this.id = id;
        this.typeClass = typeClass;
        this.defaultValue = defaultValue;
        this.permission = permission;
    }

    @NotNull
    public String getId() {
        return id;
    }

    @NotNull
    public Class<T> getTypeClass() {
        return typeClass;
    }

    @Nullable
    public T getDefaultValue() {
        return defaultValue;
    }

    @Nullable
    public String getPermission() {
        return permission;
    }

    @NotNull
    public String asString(@NotNull T t) {
        return String.valueOf(t);
    }

    @NotNull
    public T wrap(@NotNull T t) {
        return t;
    }

    @NotNull
    public T wrap(@NotNull T t, @Nullable Player player) {
        return wrap(t);
    }

    @NotNull
    public T add(@NotNull T a, @NotNull T b) {
        return a;
    }

    @NotNull
    public T substract(@NotNull T a, @NotNull T b) {
        return a;
    }
}
