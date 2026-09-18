package com.hospital.exception;

/**
 * Thrown when login fails because the username was not found
 * or the password did not match.
 */
public class AuthenticationException extends RuntimeException {
    public AuthenticationException(String message) {
        super(message);
    }
}
