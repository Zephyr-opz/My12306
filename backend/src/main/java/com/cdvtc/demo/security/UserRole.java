package com.cdvtc.demo.security;

public enum UserRole {
    ADMIN,
    USER;

    public static UserRole from(String value) {
        if (value == null) {
            return USER;
        }
        try {
            return valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return USER;
        }
    }

    public String authority() {
        return "ROLE_" + name();
    }
}
