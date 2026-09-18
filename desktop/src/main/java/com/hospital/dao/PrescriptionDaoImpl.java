package com.hospital.dao;

import com.hospital.config.DatabaseConnection;
import com.hospital.exception.DatabaseException;
import com.hospital.model.Prescription;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PrescriptionDaoImpl implements PrescriptionDao {

    private static final String BASE_SELECT =
            "SELECT pr.id, pr.medical_record_id, pr.patient_id, pr.doctor_id, pr.prescription_date, pr.notes, " +
            "pr.created_at, pr.updated_at, " +
            "p.full_name AS patient_name, p.patient_code AS patient_code, " +
            "d.full_name AS doctor_name, " +
            "(SELECT COUNT(*) FROM prescription_items pi WHERE pi.prescription_id = pr.id) AS item_count " +
            "FROM prescriptions pr " +
            "LEFT JOIN patients p ON p.id = pr.patient_id " +
            "LEFT JOIN doctors d  ON d.id = pr.doctor_id ";

    private final DatabaseConnection dbConnection;

    public PrescriptionDaoImpl(DatabaseConnection dbConnection) {
        this.dbConnection = dbConnection;
    }

    @Override
    public int create(int medicalRecordId, int patientId, int doctorId, String prescriptionDate, String notes) {
        try (Connection conn = dbConnection.getConnection()) {
            return create(conn, medicalRecordId, patientId, doctorId, prescriptionDate, notes);
        } catch (SQLException e) {
            throw new DatabaseException("Error creating prescription", e);
        }
    }

    public int create(Connection conn, int medicalRecordId, int patientId, int doctorId,
                      String prescriptionDate, String notes) {
        final String sql = "INSERT INTO prescriptions " +
                "(medical_record_id, patient_id, doctor_id, prescription_date, notes) " +
                "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, medicalRecordId);
            ps.setInt(2, patientId);
            ps.setInt(3, doctorId);
            ps.setString(4, prescriptionDate);
            setNullableString(ps, 5, notes);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
                throw new DatabaseException("Failed to retrieve generated prescription id");
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error creating prescription", e);
        }
    }

    @Override
    public Optional<Prescription> findById(int id) {
        try (Connection conn = dbConnection.getConnection()) {
            return findById(conn, id);
        } catch (SQLException e) {
            throw new DatabaseException("Error looking up prescription id=" + id, e);
        }
    }

    public Optional<Prescription> findById(Connection conn, int id) {
        final String sql = BASE_SELECT + "WHERE pr.id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error looking up prescription id=" + id, e);
        }
    }

    @Override
    public Optional<Prescription> findByMedicalRecordId(int medicalRecordId) {
        try (Connection conn = dbConnection.getConnection()) {
            return findByMedicalRecordId(conn, medicalRecordId);
        } catch (SQLException e) {
            throw new DatabaseException("Error looking up prescription for medical record id=" + medicalRecordId, e);
        }
    }

    public Optional<Prescription> findByMedicalRecordId(Connection conn, int medicalRecordId) {
        final String sql = BASE_SELECT + "WHERE pr.medical_record_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, medicalRecordId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error looking up prescription for medical record id=" + medicalRecordId, e);
        }
    }

    @Override
    public List<Prescription> findAll() {
        return search(null);
    }

    @Override
    public void update(int id, String prescriptionDate, String notes) {
        try (Connection conn = dbConnection.getConnection()) {
            update(conn, id, prescriptionDate, notes);
        } catch (SQLException e) {
            throw new DatabaseException("Error updating prescription id=" + id, e);
        }
    }

    public void update(Connection conn, int id, String prescriptionDate, String notes) {
        final String sql = "UPDATE prescriptions SET prescription_date = ?, notes = ?, " +
                "updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, prescriptionDate);
            setNullableString(ps, 2, notes);
            ps.setInt(3, id);
            int rows = ps.executeUpdate();
            if (rows == 0) throw new DatabaseException("Prescription not found (id=" + id + ")");
        } catch (SQLException e) {
            throw new DatabaseException("Error updating prescription id=" + id, e);
        }
    }

    @Override
    public List<Prescription> search(String query) {
        List<Prescription> result = new ArrayList<>();
        StringBuilder sql = new StringBuilder(BASE_SELECT);
        String like = null;
        if (query != null && !query.isBlank()) {
            like = "%" + query.trim().toLowerCase() + "%";
            sql.append("WHERE (LOWER(COALESCE(p.full_name,'')) LIKE ? " +
                    "OR LOWER(COALESCE(p.patient_code,'')) LIKE ? " +
                    "OR LOWER(COALESCE(d.full_name,'')) LIKE ? " +
                    "OR EXISTS (SELECT 1 FROM prescription_items pi " +
                    "          WHERE pi.prescription_id = pr.id " +
                    "            AND LOWER(COALESCE(pi.medicine_name,'')) LIKE ?)) ");
        }
        sql.append("ORDER BY pr.prescription_date DESC, pr.id DESC");

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            if (like != null) {
                ps.setString(1, like);
                ps.setString(2, like);
                ps.setString(3, like);
                ps.setString(4, like);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error searching prescriptions", e);
        }
        return result;
    }

    @Override
    public boolean existsByMedicalRecord(int medicalRecordId) {
        try (Connection conn = dbConnection.getConnection()) {
            return existsByMedicalRecord(conn, medicalRecordId);
        } catch (SQLException e) {
            throw new DatabaseException("Error checking prescription existence for medical record id=" + medicalRecordId, e);
        }
    }

    public boolean existsByMedicalRecord(Connection conn, int medicalRecordId) {
        final String sql = "SELECT COUNT(*) AS c FROM prescriptions WHERE medical_record_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, medicalRecordId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt("c") > 0;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error checking prescription existence for medical record id=" + medicalRecordId, e);
        }
    }

    @Override
    public int count() {
        final String sql = "SELECT COUNT(*) AS c FROM prescriptions";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt("c");
            return 0;
        } catch (SQLException e) {
            throw new DatabaseException("Error counting prescriptions", e);
        }
    }

    // ---------- helpers ----------

    private void setNullableString(PreparedStatement ps, int idx, String value) throws SQLException {
        if (value == null) ps.setNull(idx, java.sql.Types.VARCHAR);
        else ps.setString(idx, value);
    }

    private Prescription mapRow(ResultSet rs) throws SQLException {
        Prescription p = new Prescription();
        p.setId(rs.getInt("id"));
        p.setMedicalRecordId(rs.getInt("medical_record_id"));
        p.setPatientId(rs.getInt("patient_id"));
        p.setDoctorId(rs.getInt("doctor_id"));
        String d = rs.getString("prescription_date");
        p.setPrescriptionDate(d == null || d.isBlank() ? null : LocalDate.parse(d));
        p.setNotes(rs.getString("notes"));
        p.setPatientName(rs.getString("patient_name"));
        p.setPatientCode(rs.getString("patient_code"));
        p.setDoctorName(rs.getString("doctor_name"));
        p.setItemCount(rs.getInt("item_count"));
        p.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));
        p.setUpdatedAt(toLocalDateTime(rs.getTimestamp("updated_at")));
        return p;
    }

    private LocalDateTime toLocalDateTime(Timestamp ts) {
        return ts == null ? LocalDateTime.now() : ts.toLocalDateTime();
    }
}
