package com.hospital.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Helper for JUnit tests that need a fresh, temporary SQLite database.
 */
public final class TestDatabaseFactory {

    private TestDatabaseFactory() {
    }

    /**
     * Create a new DatabaseConnection pointing at a unique temporary file.
     * The caller is responsible for deleting the file when done.
     */
    public static DatabaseConnection createTempDatabase() throws IOException {
        Path tmp = Files.createTempFile("hospital-test-", ".db");
        // Delete immediately so SQLite creates a fresh file, otherwise reuse path.
        Files.deleteIfExists(tmp);
        return new DatabaseConnection(DatabaseConfig.getJdbcUrl(tmp.toAbsolutePath().toString()));
    }

    public static void deleteDatabaseFile(DatabaseConnection db) throws IOException {
        String url = db.getJdbcUrl();
        if (url.startsWith("jdbc:sqlite:")) {
            Path p = Path.of(url.substring("jdbc:sqlite:".length()));
            Files.deleteIfExists(p);
            // SQLite also creates -shm/-wal files when using WAL mode
            Files.deleteIfExists(Path.of(p + "-shm"));
            Files.deleteIfExists(Path.of(p + "-wal"));
        }
    }
}
