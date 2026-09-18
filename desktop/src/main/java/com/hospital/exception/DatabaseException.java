package com.hospital.exception;

/**
 * Runtime wrapper around SQL / database errors so layers above the DAO
 * do not have to catch SQLException everywhere.
 */
public class DatabaseException extends RuntimeException {
    public DatabaseException(String message) {
        super(message);
    }

    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}
