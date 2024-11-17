package com.saicone.savedata.api.data;

public enum DataOperator {

    GET,
    CONTAINS,
    EXPIRY,
    DELETE,
    SET,
    ADD,
    SUBSTRACT,
    MULTIPLY,
    DIVIDE;

    public boolean isEval() {
        return this == GET || this == CONTAINS || this == EXPIRY;
    }

    public boolean isUpdate() {
        switch (this) {
            case DELETE:
            case SET:
            case ADD:
            case SUBSTRACT:
            case MULTIPLY:
            case DIVIDE:
                return true;
            default:
                return false;
        }
    }
}
