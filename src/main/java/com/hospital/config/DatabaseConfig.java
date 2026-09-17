package com.hospital.config;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Central place for database configuration.
 *
 * By default the SQLite database is stored in a local {@code data/} folder
 * relative to where the application is launched from. This keeps the
 * application 100% local and portable, as required.
 */
public class DatabaseConfig {

    /**
     * Default relative path to the SQLite database file.
     */
    public static final String DEFAULT_DB_PATH = "data" + java.io.File.separator + "hospital.db";

    private DatabaseConfig() {
        // utility class
    }

    /**
     * Returns the JDBC URL to connect to the default database.
     */
    public static String getJdbcUrl() {
        return "jdbc:sqlite:" + DEFAULT_DB_PATH;
    }

    /**
     * Returns the JDBC URL for a specific absolute or relative file path.
     * Useful for tests that need a temporary database.
     */
    public static String getJdbcUrl(String dbPath) {
        return "jdbc:sqlite:" + dbPath;
    }

    /**
     * Returns the Path of the database directory (the parent of the .db file).
     */
    public static Path getDatabaseDirectory() {
        return Paths.get(DEFAULT_DB_PATH).toAbsolutePath().getParent();
    }

    public static Path getDatabaseFile() {
        return Paths.get(DEFAULT_DB_PATH).toAbsolutePath();
    }
}
