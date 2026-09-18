package com.hospital.model;

/**
 * Enum representing the possible roles a user can have in the system.
 */
public enum Role {
    ADMIN,
    DOCTOR,
    RECEPTIONIST;

    /**
     * Safely convert a database string back to a Role enum.
     * Returns null if the value is unknown.
     */
    public static Role fromString(String value) {
        if (value == null) return null;
        try {
            return Role.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
