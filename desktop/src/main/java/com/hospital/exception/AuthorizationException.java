package com.hospital.exception;

/**
 * Thrown when a logged-in user attempts an action their role does not permit
 * (e.g. a DOCTOR trying to create a department).
 */
public class AuthorizationException extends RuntimeException {
    public AuthorizationException(String message) {
        super(message);
    }
}
