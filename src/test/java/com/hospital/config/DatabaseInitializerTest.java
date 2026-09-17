package com.hospital.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies that DatabaseInitializer creates the required tables, and that
 * calling initialize() multiple times is safe.
 */
class DatabaseInitializerTest {

    private DatabaseConnection db;

    @BeforeEach
    void setUp() throws IOException {
        db = TestDatabaseFactory.createTempDatabase();
    }

    @AfterEach
    void tearDown() throws IOException {
        TestDatabaseFactory.deleteDatabaseFile(db);
    }

    @Test
    void initializeCreatesRequiredTables() throws Exception {
        new DatabaseInitializer(db).initialize();

        Set<String> tables = listTables(db);
        assertTrue(tables.contains("users"), "users table must exist");
        assertTrue(tables.contains("departments"), "departments table must exist");
        assertTrue(tables.contains("doctors"), "doctors table must exist");
        assertTrue(tables.contains("patients"), "patients table must exist");
        assertTrue(tables.contains("appointments"), "appointments table must exist");
    }

    @Test
    void initializeIsIdempotent() throws Exception {
        DatabaseInitializer init = new DatabaseInitializer(db);
        init.initialize();
        init.initialize(); // second call should not throw

        Set<String> tables = listTables(db);
        assertTrue(tables.contains("users"));
        assertTrue(tables.contains("departments"));
        assertTrue(tables.contains("doctors"));
        assertTrue(tables.contains("patients"));
        assertTrue(tables.contains("appointments"));
    }

    @Test
    void usersTableHasExpectedColumns() throws Exception {
        new DatabaseInitializer(db).initialize();
        Set<String> cols = listColumns(db, "users");
        assertTrue(cols.contains("id"));
        assertTrue(cols.contains("username"));
        assertTrue(cols.contains("password_hash"));
        assertTrue(cols.contains("role"));
        assertTrue(cols.contains("is_active"), "users table must have is_active column");
        assertTrue(cols.contains("created_at"));
    }

    @Test
    void migrationAddsIsActiveColumnToExistingUsersTable() throws Exception {
        // Simulate a Phase 1 database (create the users table WITHOUT is_active)
        // and verify DatabaseInitializer migrates it safely.
        try (Connection c = db.getConnection(); Statement s = c.createStatement()) {
            s.execute("CREATE TABLE users (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "username TEXT NOT NULL UNIQUE," +
                    "password_hash TEXT NOT NULL," +
                    "role TEXT NOT NULL," +
                    "created_at TEXT NOT NULL DEFAULT (datetime('now')));");
            s.execute("INSERT INTO users (username, password_hash, role) VALUES " +
                    "('legacyadmin', 'h', 'ADMIN');");
        }
        new DatabaseInitializer(db).initialize();
        Set<String> cols = listColumns(db, "users");
        assertTrue(cols.contains("is_active"));

        // Existing rows should be active by default.
        try (Connection c = db.getConnection();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT is_active FROM users WHERE username='legacyadmin'")) {
            assertTrue(rs.next());
            assertEquals(1, rs.getInt(1));
        }
    }

    @Test
    void migrationIsIdempotent() throws Exception {
        DatabaseInitializer init = new DatabaseInitializer(db);
        init.initialize();
        init.initialize(); // second call must not throw
        Set<String> cols = listColumns(db, "users");
        assertTrue(cols.contains("is_active"));
    }

    @Test
    void departmentsTableHasExpectedColumns() throws Exception {
        new DatabaseInitializer(db).initialize();
        Set<String> cols = listColumns(db, "departments");
        assertTrue(cols.contains("id"));
        assertTrue(cols.contains("name"));
        assertTrue(cols.contains("description"));
    }

    @Test
    void doctorsTableHasExpectedColumns() throws Exception {
        new DatabaseInitializer(db).initialize();
        Set<String> cols = listColumns(db, "doctors");
        assertTrue(cols.contains("id"));
        assertTrue(cols.contains("user_id"));
        assertTrue(cols.contains("department_id"));
        assertTrue(cols.contains("full_name"));
        assertTrue(cols.contains("specialization"));
        assertTrue(cols.contains("phone"));
        assertTrue(cols.contains("email"));
        assertTrue(cols.contains("consultation_fee"));
        assertTrue(cols.contains("is_active"));
        assertTrue(cols.contains("created_at"));
        assertTrue(cols.contains("updated_at"));
    }

    @Test
    void doctorsMigrationPreservesExistingData() throws Exception {
        // Simulate a pre-Phase-2.3 DB with a user and department already present.
        // After initialization, the doctors table must exist and prior data remains intact.
        new DatabaseInitializer(db).initialize();
        try (Connection c = db.getConnection(); Statement s = c.createStatement()) {
            s.execute("INSERT INTO departments (name) VALUES ('Cardiology')");
            s.execute("INSERT INTO users (username, password_hash, role) VALUES ('alice', 'h', 'DOCTOR')");
        }
        // Re-run initializer (idempotent) — must not destroy departments or users.
        new DatabaseInitializer(db).initialize();

        Set<String> tables = listTables(db);
        assertTrue(tables.contains("doctors"));
        assertTrue(tables.contains("patients"));

        try (Connection c = db.getConnection();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT COUNT(*) AS c FROM departments")) {
            assertTrue(rs.next());
            assertEquals(1, rs.getInt("c"));
        }
        try (Connection c = db.getConnection();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT COUNT(*) AS c FROM users WHERE username='alice'")) {
            assertTrue(rs.next());
            assertEquals(1, rs.getInt("c"));
        }
    }

    @Test
    void patientsTableHasExpectedColumns() throws Exception {
        new DatabaseInitializer(db).initialize();
        Set<String> cols = listColumns(db, "patients");
        assertTrue(cols.contains("id"));
        assertTrue(cols.contains("patient_code"));
        assertTrue(cols.contains("full_name"));
        assertTrue(cols.contains("date_of_birth"));
        assertTrue(cols.contains("gender"));
        assertTrue(cols.contains("phone"));
        assertTrue(cols.contains("email"));
        assertTrue(cols.contains("address"));
        assertTrue(cols.contains("emergency_contact_name"));
        assertTrue(cols.contains("emergency_contact_phone"));
        assertTrue(cols.contains("blood_group"));
        assertTrue(cols.contains("is_active"));
        assertTrue(cols.contains("created_at"));
        assertTrue(cols.contains("updated_at"));
    }

    @Test
    void patientsMigrationPreservesExistingData() throws Exception {
        new DatabaseInitializer(db).initialize();
        try (Connection c = db.getConnection(); Statement s = c.createStatement()) {
            s.execute("INSERT INTO departments (name) VALUES ('Cardiology')");
            s.execute("INSERT INTO users (username, password_hash, role) VALUES ('alice', 'h', 'DOCTOR')");
        }
        new DatabaseInitializer(db).initialize();
        Set<String> tables = listTables(db);
        assertTrue(tables.contains("patients"));
        try (Connection c = db.getConnection();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT COUNT(*) AS c FROM departments")) {
            assertTrue(rs.next());
            assertEquals(1, rs.getInt("c"));
        }
        try (Connection c = db.getConnection();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT COUNT(*) AS c FROM users WHERE username='alice'")) {
            assertTrue(rs.next());
            assertEquals(1, rs.getInt("c"));
        }
    }

    @Test
    void appointmentsTableHasExpectedColumns() throws Exception {
        new DatabaseInitializer(db).initialize();
        Set<String> cols = listColumns(db, "appointments");
        assertTrue(cols.contains("id"));
        assertTrue(cols.contains("patient_id"));
        assertTrue(cols.contains("doctor_id"));
        assertTrue(cols.contains("appointment_date"));
        assertTrue(cols.contains("appointment_time"));
        assertTrue(cols.contains("reason"));
        assertTrue(cols.contains("status"));
        assertTrue(cols.contains("notes"));
        assertTrue(cols.contains("created_at"));
        assertTrue(cols.contains("updated_at"));
    }

    @Test
    void appointmentsMigrationPreservesExistingData() throws Exception {
        new DatabaseInitializer(db).initialize();
        try (Connection c = db.getConnection(); Statement s = c.createStatement()) {
            s.execute("INSERT INTO departments (name) VALUES ('Cardiology')");
            s.execute("INSERT INTO users (username, password_hash, role) VALUES ('alice', 'h', 'DOCTOR')");
        }
        new DatabaseInitializer(db).initialize();
        Set<String> tables = listTables(db);
        assertTrue(tables.contains("appointments"));
        try (Connection c = db.getConnection();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT COUNT(*) AS c FROM departments")) {
            assertTrue(rs.next());
            assertEquals(1, rs.getInt("c"));
        }
    }

    @Test
    void foreignKeysAreEnforced() throws Exception {
        new DatabaseInitializer(db).initialize();
        // Inserting an appointment with a bad doctor_id should fail because FKs are enabled.
        try (Connection c = db.getConnection(); Statement s = c.createStatement()) {
            // Insert a patient first so only doctor FK is invalid
            s.execute("INSERT INTO patients (patient_code, full_name) VALUES ('PAT-000001', 'X')");
            assertThrows(Exception.class, () ->
                    s.executeUpdate("INSERT INTO appointments (patient_id, doctor_id, appointment_date, appointment_time) " +
                            "VALUES (1, 9999, '2030-01-01', '09:00')"));
        }
    }

    private Set<String> listTables(DatabaseConnection conn) throws Exception {
        Set<String> tables = new HashSet<>();
        try (Connection c = conn.getConnection();
             ResultSet rs = c.getMetaData().getTables(null, null, "%", new String[]{"TABLE"})) {
            while (rs.next()) {
                tables.add(rs.getString("TABLE_NAME").toLowerCase());
            }
        }
        return tables;
    }

    private Set<String> listColumns(DatabaseConnection conn, String table) throws Exception {
        Set<String> cols = new HashSet<>();
        try (Connection c = conn.getConnection();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT * FROM " + table + " LIMIT 0")) {
            var meta = rs.getMetaData();
            for (int i = 1; i <= meta.getColumnCount(); i++) {
                cols.add(meta.getColumnName(i).toLowerCase());
            }
        }
        return cols;
    }
}
