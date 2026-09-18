package com.hospital.config;

import com.hospital.exception.DatabaseException;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Creates the initial schema (tables) if they do not already exist, and
 * applies small, safe migrations as the project grows. Safe to run on every
 * startup.
 */
public class DatabaseInitializer {

    private final DatabaseConnection dbConnection;

    public DatabaseInitializer(DatabaseConnection dbConnection) {
        this.dbConnection = dbConnection;
    }

    public void initialize() {
        try (Connection conn = dbConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute("PRAGMA foreign_keys = ON;");

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id              INTEGER PRIMARY KEY AUTOINCREMENT,
                    username        TEXT    NOT NULL UNIQUE,
                    password_hash   TEXT    NOT NULL,
                    role            TEXT    NOT NULL CHECK (role IN ('ADMIN','DOCTOR','RECEPTIONIST')),
                    is_active       INTEGER NOT NULL DEFAULT 1,
                    created_at      TEXT    NOT NULL DEFAULT (datetime('now'))
                );
                """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS departments (
                    id              INTEGER PRIMARY KEY AUTOINCREMENT,
                    name            TEXT    NOT NULL UNIQUE,
                    description     TEXT
                );
                """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS doctors (
                    id               INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id          INTEGER,
                    department_id    INTEGER NOT NULL,
                    full_name        TEXT    NOT NULL,
                    specialization   TEXT    NOT NULL,
                    phone            TEXT,
                    email            TEXT,
                    consultation_fee REAL    NOT NULL DEFAULT 0,
                    is_active        INTEGER NOT NULL DEFAULT 1,
                    created_at       TEXT    NOT NULL DEFAULT (datetime('now')),
                    updated_at       TEXT    NOT NULL DEFAULT (datetime('now')),
                    FOREIGN KEY (user_id)       REFERENCES users(id),
                    FOREIGN KEY (department_id) REFERENCES departments(id)
                );
                """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS patients (
                    id                      INTEGER PRIMARY KEY AUTOINCREMENT,
                    patient_code            TEXT    NOT NULL UNIQUE,
                    full_name               TEXT    NOT NULL,
                    date_of_birth           TEXT,
                    gender                  TEXT,
                    phone                   TEXT,
                    email                   TEXT,
                    address                 TEXT,
                    emergency_contact_name  TEXT,
                    emergency_contact_phone TEXT,
                    blood_group             TEXT,
                    is_active               INTEGER NOT NULL DEFAULT 1,
                    created_at              TEXT    NOT NULL DEFAULT (datetime('now')),
                    updated_at              TEXT    NOT NULL DEFAULT (datetime('now'))
                );
                """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS appointments (
                    id               INTEGER PRIMARY KEY AUTOINCREMENT,
                    patient_id       INTEGER NOT NULL,
                    doctor_id        INTEGER NOT NULL,
                    appointment_date TEXT    NOT NULL,
                    appointment_time TEXT    NOT NULL,
                    reason           TEXT,
                    status           TEXT    NOT NULL DEFAULT 'SCHEDULED',
                    notes            TEXT,
                    created_at       TEXT    NOT NULL DEFAULT (datetime('now')),
                    updated_at       TEXT    NOT NULL DEFAULT (datetime('now')),
                    FOREIGN KEY (patient_id) REFERENCES patients(id),
                    FOREIGN KEY (doctor_id)  REFERENCES doctors(id)
                );
                """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS medical_records (
                    id               INTEGER PRIMARY KEY AUTOINCREMENT,
                    appointment_id   INTEGER NOT NULL UNIQUE,
                    patient_id       INTEGER NOT NULL,
                    doctor_id        INTEGER NOT NULL,
                    diagnosis        TEXT    NOT NULL,
                    symptoms         TEXT,
                    examination      TEXT,
                    treatment_notes  TEXT,
                    record_date      TEXT    NOT NULL,
                    created_at       TEXT    NOT NULL DEFAULT (datetime('now')),
                    updated_at       TEXT    NOT NULL DEFAULT (datetime('now')),
                    FOREIGN KEY (appointment_id) REFERENCES appointments(id),
                    FOREIGN KEY (patient_id)     REFERENCES patients(id),
                    FOREIGN KEY (doctor_id)      REFERENCES doctors(id)
                );
                """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS prescriptions (
                    id                INTEGER PRIMARY KEY AUTOINCREMENT,
                    medical_record_id INTEGER NOT NULL UNIQUE,
                    patient_id        INTEGER NOT NULL,
                    doctor_id         INTEGER NOT NULL,
                    prescription_date TEXT    NOT NULL,
                    notes             TEXT,
                    created_at        TEXT    NOT NULL DEFAULT (datetime('now')),
                    updated_at        TEXT    NOT NULL DEFAULT (datetime('now')),
                    FOREIGN KEY (medical_record_id) REFERENCES medical_records(id),
                    FOREIGN KEY (patient_id)        REFERENCES patients(id),
                    FOREIGN KEY (doctor_id)         REFERENCES doctors(id)
                );
                """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS prescription_items (
                    id              INTEGER PRIMARY KEY AUTOINCREMENT,
                    prescription_id INTEGER NOT NULL,
                    medicine_name   TEXT    NOT NULL,
                    dosage          TEXT    NOT NULL,
                    frequency       TEXT    NOT NULL,
                    duration        TEXT    NOT NULL,
                    instructions    TEXT,
                    created_at      TEXT    NOT NULL DEFAULT (datetime('now')),
                    updated_at      TEXT    NOT NULL DEFAULT (datetime('now')),
                    FOREIGN KEY (prescription_id) REFERENCES prescriptions(id) ON DELETE CASCADE
                );
                """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS bills (
                    id              INTEGER PRIMARY KEY AUTOINCREMENT,
                    bill_number     TEXT    NOT NULL UNIQUE,
                    patient_id      INTEGER NOT NULL,
                    appointment_id  INTEGER,
                    bill_date       TEXT    NOT NULL,
                    status          TEXT    NOT NULL DEFAULT 'UNPAID'
                                    CHECK (status IN ('UNPAID','PARTIALLY_PAID','PAID','CANCELLED')),
                    notes           TEXT,
                    subtotal        REAL    NOT NULL DEFAULT 0,
                    discount        REAL    NOT NULL DEFAULT 0,
                    total_amount    REAL    NOT NULL DEFAULT 0,
                    created_at      TEXT    NOT NULL DEFAULT (datetime('now')),
                    updated_at      TEXT    NOT NULL DEFAULT (datetime('now')),
                    FOREIGN KEY (patient_id)     REFERENCES patients(id),
                    FOREIGN KEY (appointment_id) REFERENCES appointments(id)
                );
                """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS bill_items (
                    id              INTEGER PRIMARY KEY AUTOINCREMENT,
                    bill_id         INTEGER NOT NULL,
                    description     TEXT    NOT NULL,
                    quantity        INTEGER NOT NULL DEFAULT 1,
                    unit_price      REAL    NOT NULL,
                    amount          REAL    NOT NULL,
                    created_at      TEXT    NOT NULL DEFAULT (datetime('now')),
                    updated_at      TEXT    NOT NULL DEFAULT (datetime('now')),
                    FOREIGN KEY (bill_id) REFERENCES bills(id) ON DELETE CASCADE
                );
                """);

            stmt.execute("CREATE INDEX IF NOT EXISTS idx_bills_patient_id ON bills(patient_id);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_bills_appointment_id ON bills(appointment_id);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_bills_status ON bills(status);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_bill_items_bill_id ON bill_items(bill_id);");

            // Phase 2.2 migration: ensure is_active column exists on legacy users DBs.
            if (!columnExists(conn, "users", "is_active")) {
                stmt.execute("ALTER TABLE users ADD COLUMN is_active INTEGER NOT NULL DEFAULT 1;");
                stmt.execute("UPDATE users SET is_active = 1 WHERE is_active IS NULL OR is_active <> 1;");
            }

        } catch (SQLException e) {
            throw new DatabaseException("Failed to initialize database schema", e);
        }
    }

    private boolean columnExists(Connection conn, String tableName, String columnName) throws SQLException {
        DatabaseMetaData meta = conn.getMetaData();
        try (ResultSet rs = meta.getColumns(null, null, tableName, null)) {
            while (rs.next()) {
                String existing = rs.getString("COLUMN_NAME");
                if (existing != null && existing.equalsIgnoreCase(columnName)) {
                    return true;
                }
            }
        }
        return false;
    }
}
