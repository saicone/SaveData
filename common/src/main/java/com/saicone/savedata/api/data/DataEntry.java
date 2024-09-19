package com.saicone.savedata.api.data;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public class DataEntry<T> {

    private Integer id;

    private final DataType<T> type;
    private T value;
    private Long expiration = 0L;

    private transient Object userValue;
    private transient boolean edited;

    public DataEntry(@NotNull DataType<T> type) {
        this(null, type, null, null);
    }

    public DataEntry(@NotNull DataType<T> type, @Nullable T value, @Nullable Long expiration) {
        this(null, type, value, expiration);
    }

    public DataEntry(@Nullable Integer id, @NotNull DataType<T> type, @Nullable T value, @Nullable Long expiration) {
        this.id = id;
        this.type = type;
        this.value = value;
        this.expiration = expiration;
    }

    public boolean isSaved() {
        return id > 0;
    }

    public boolean isTemporary() {
        return expiration != null && expiration > 0;
    }

    public boolean isEdited() {
        return edited;
    }

    @Nullable
    public Integer getId() {
        return id;
    }

    @NotNull
    public DataType<T> getType() {
        return type;
    }

    @Nullable
    public T getValue() {
        if (expiration != null && expiration > 0 && System.currentTimeMillis() >= expiration) {
            setValue(null);
        }
        return value;
    }

    @Nullable
    public String getSavedValue() {
        if (value == null) {
            return null;
        }
        return type.save(value);
    }

    @NotNull
    @SuppressWarnings("unchecked")
    public Object getUserValue(@NotNull Function<String, String> userParser) {
        if (userValue == null) {
            T tempValue = value;
            if (tempValue instanceof String && type.isUserParseable()){
                tempValue = (T) userParser.apply((String) tempValue);
            }
            userValue = type.eval(tempValue);
        }
        return userValue;
    }

    public long getExpiration() {
        return expiration;
    }

    public void setId(@Nullable Integer id) {
        this.id = id;
    }

    public void setValue(@Nullable T value) {
        this.value = value;
        this.edited = true;
        this.userValue = null;
    }

    public void setExpiration(long expiration) {
        this.expiration = expiration;
    }

    public void setEdited(boolean edited) {
        this.edited = edited;
    }

    @Override
    public String toString() {
        return "DataEntry{" +
                "id=" + id +
                ", type=" + type +
                ", value=" + value +
                ", expiration=" + expiration +
                ", userValue=" + userValue +
                ", edited=" + edited +
                '}';
    }
}
