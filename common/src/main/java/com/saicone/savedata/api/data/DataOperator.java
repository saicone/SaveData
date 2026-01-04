package com.saicone.savedata.api.data;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public enum DataOperator {

    GET,
    CONTAINS,
    EXPIRY,
    DELETE,
    SET,
    ADD,
    SUBTRACT,
    MULTIPLY,
    DIVIDE,
    RAW_SET,
    RAW_ADD,
    RAW_SUBTRACT,
    RAW_MULTIPLY,
    RAW_DIVIDE;

    private static final Map<String, DataOperator> ALIASES = new HashMap<>();

    static {
        for (DataOperator value : values()) {
            ALIASES.put(value.name(), value);
            ALIASES.put(value.name().replace("_", ""), value);
            ALIASES.put(value.name().replace("_", "-"), value);
        }
        ALIASES.put("SUBSTRACT", SUBTRACT);
        ALIASES.put("RAW_SUBSTRACT", SUBTRACT);
        ALIASES.put("RAWSUBSTRACT", SUBTRACT);
        ALIASES.put("RAW-SUBSTRACT", SUBTRACT);
    }

    public boolean isEval() {
        return this == GET || this == CONTAINS || this == EXPIRY;
    }

    public boolean isUpdate() {
        switch (this) {
            case DELETE:
            case SET:
            case ADD:
            case SUBTRACT:
            case MULTIPLY:
            case DIVIDE:
            case RAW_SET:
            case RAW_ADD:
            case RAW_SUBTRACT:
            case RAW_MULTIPLY:
            case RAW_DIVIDE:
                return true;
            default:
                return false;
        }
    }

    public boolean isSet() {
        return this == SET || this == RAW_SET;
    }

    public boolean isRaw() {
        switch (this) {
            case RAW_SET:
            case RAW_ADD:
            case RAW_SUBTRACT:
            case RAW_MULTIPLY:
            case RAW_DIVIDE:
                return true;
            default:
                return false;
        }
    }

    @NotNull
    public static DataOperator of(@NotNull String s) {
        final DataOperator value = ALIASES.get(s.toUpperCase());
        if (value == null) {
            throw new IllegalArgumentException("The string '" + s + "' is not a data operator");
        }
        return value;
    }
}
