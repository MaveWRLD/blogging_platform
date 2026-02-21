package org.amalitech.enums;

public enum TokenType {
    ACCESS,
    REFRESH;

    public static TokenType from(String value) {
        return TokenType.valueOf(value.toUpperCase());
    }
}
