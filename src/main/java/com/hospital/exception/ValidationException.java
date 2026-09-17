package com.hospital.exception;

/**
 * Thrown when user-supplied input fails business-rule validation
 * (e.g. empty department name, name too long, duplicate name).
 * The message is safe to display directly in the UI.
 */
public class ValidationException extends RuntimeException {
    public ValidationException(String message) {
        super(message);
    }
}
