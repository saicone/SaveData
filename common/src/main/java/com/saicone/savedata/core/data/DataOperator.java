package com.saicone.savedata.core.data;

public enum DataOperator {

    GET,
    CONTAINS,
    DELETE,
    SET,
    ADD,
    SUBSTRACT,
    MULTIPLY,
    DIVIDE;

    public boolean isEval() {
        return this == GET
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
