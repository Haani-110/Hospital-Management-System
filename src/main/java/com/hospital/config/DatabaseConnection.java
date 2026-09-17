package com.hospital.config;

import com.hospital.exception.DatabaseException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Handles opening JDBC connections to the local SQLite database.
 *
 * Ensures that the {@code data/} directory and database file exist before
 * connections are handed out, so the app works the first time it is launched.
 */
public class DatabaseConnection {

    private final String jdbcUrl;

    /**
     * Create a DatabaseConnection that points at the default
     * {@code data/hospital.db} file.
     */
    public DatabaseConnection() {
        this(DatabaseConfig.getJdbcUrl());
        ensureDataDirectoryExists();
    }

    /**
     * Create a DatabaseConnection that points at an arbitrary JDBC URL.
     * Useful for tests that point at a temporary file.
     */
    public DatabaseConnection(String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
    }

    /**
     * Open a new JDBC connection to SQLite.
     * Caller is responsible for closing the connection.
     */
    public Connection getConnection() {
        try {
            // Load SQLite driver explicitly (safe for Java 21 service loader too)
            Class.forName("org.sqlite.JDBC");
            Connection conn = DriverManager.getConnection(jdbcUrl);
            // Enable foreign keys on every connection (SQLite disables them by default)
            try (var stmt = conn.createStatement()) {
                stmt.execute("PRAGMA foreign_keys = ON;");
            }
            return conn;
        } catch (ClassNotFoundException e) {
            throw new DatabaseException("SQLite JDBC driver not found on classpath", e);
        } catch (SQLException e) {
            throw new DatabaseException("Could not connect to database: " + jdbcUrl, e);
        }
    }

    public String getJdbcUrl() {
        return jdbcUrl;
    }

    /**
     * Make sure the {@code data/} directory exists so SQLite can create the
     * database file inside it on the first connection.
     */
    private void ensureDataDirectoryExists() {
        Path dir = DatabaseConfig.getDatabaseDirectory();
        try {
            if (dir != null) {
                Files.createDirectories(dir);
            }
        } catch (IOException e) {
            throw new DatabaseException("Could not create database directory: " + dir, e);
        }
    }
}
