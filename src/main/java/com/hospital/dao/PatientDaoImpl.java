package com.hospital.dao;

import com.hospital.config.DatabaseConnection;
import com.hospital.exception.DatabaseException;
import com.hospital.model.Patient;

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

public class PatientDaoImpl implements PatientDao {

    private static final String BASE_SELECT =
            "SELECT id, patient_code, full_name, date_of_birth, gender, phone, email, address, " +
            "emergency_contact_name, emergency_contact_phone, blood_group, is_active, " +
            "created_at, updated_at FROM patients ";

    private final DatabaseConnection dbConnection;

    public PatientDaoImpl(DatabaseConnection dbConnection) {
        this.dbConnection = dbConnection;
    }

    @Override
    public int create(String patientCode, String fullName, String dateOfBirth, String gender,
                      String phone, String email, String address,
                      String emergencyContactName, String emergencyContactPhone, String bloodGroup) {
        final String sql = "INSERT INTO patients (patient_code, full_name, date_of_birth, gender, phone, email, address, " +
                "emergency_contact_name, emergency_contact_phone, blood_group) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, patientCode);
            ps.setString(2, fullName);
            ps.setString(3, dateOfBirth);
            ps.setString(4, gender);
            ps.setString(5, phone);
            ps.setString(6, email);
            ps.setString(7, address);
            ps.setString(8, emergencyContactName);
            ps.setString(9, emergencyContactPhone);
            ps.setString(10, bloodGroup);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
                throw new DatabaseException("Failed to retrieve generated patient id");
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error creating patient: " + fullName, e);
        }
    }

    @Override
    public Optional<Patient> findById(int id) {
        final String sql = BASE_SELECT + "WHERE id = ?";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error looking up patient id=" + id, e);
        }
    }

    @Override
    public Optional<Patient> findByPatientCode(String patientCode) {
        final String sql = BASE_SELECT + "WHERE patient_code = ?";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, patientCode);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error looking up patient code=" + patientCode, e);
        }
    }

    @Override
    public List<Patient> findAll() {
        return search(null, null);
    }

    @Override
    public void update(int id, String fullName, String dateOfBirth, String gender,
                       String phone, String email, String address,
                       String emergencyContactName, String emergencyContactPhone, String bloodGroup) {
        final String sql = "UPDATE patients SET full_name = ?, date_of_birth = ?, gender = ?, phone = ?, email = ?, " +
                "address = ?, emergency_contact_name = ?, emergency_contact_phone = ?, blood_group = ?, " +
                "updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, fullName);
            ps.setString(2, dateOfBirth);
            ps.setString(3, gender);
            ps.setString(4, phone);
            ps.setString(5, email);
            ps.setString(6, address);
            ps.setString(7, emergencyContactName);
            ps.setString(8, emergencyContactPhone);
            ps.setString(9, bloodGroup);
            ps.setInt(10, id);
            int rows = ps.executeUpdate();
            if (rows == 0) throw new DatabaseException("Patient not found (id=" + id + ")");
        } catch (SQLException e) {
            throw new DatabaseException("Error updating patient id=" + id, e);
        }
    }

    @Override
    public void updateActiveStatus(int id, boolean active) {
        final String sql = "UPDATE patients SET is_active = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, active ? 1 : 0);
            ps.setInt(2, id);
            int rows = ps.executeUpdate();
            if (rows == 0) throw new DatabaseException("Patient not found (id=" + id + ")");
        } catch (SQLException e) {
            throw new DatabaseException("Error updating active status for patient id=" + id, e);
        }
    }

    @Override
    public List<Patient> search(String query, Boolean activeFilter) {
        List<Patient> result = new ArrayList<>();
        StringBuilder sql = new StringBuilder(BASE_SELECT);
        List<String> where = new ArrayList<>();

        String like = null;
        if (query != null && !query.isBlank()) {
            like = "%" + query.trim().toLowerCase() + "%";
            where.add("(LOWER(patient_code) LIKE ? OR LOWER(full_name) LIKE ? " +
                    "OR LOWER(COALESCE(phone,'')) LIKE ? OR LOWER(COALESCE(email,'')) LIKE ?)");
        }
        Integer activeInt = null;
        if (activeFilter != null) {
            where.add("is_active = ?");
            activeInt = activeFilter ? 1 : 0;
        }
        if (!where.isEmpty()) sql.append("WHERE ").append(String.join(" AND ", where)).append(" ");
        sql.append("ORDER BY full_name COLLATE NOCASE ASC");

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            int idx = 1;
            if (like != null) {
                ps.setString(idx++, like);
                ps.setString(idx++, like);
                ps.setString(idx++, like);
                ps.setString(idx++, like);
            }
            if (activeInt != null) {
                ps.setInt(idx++, activeInt);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error searching patients", e);
        }
        return result;
    }

    @Override
    public boolean existsById(int id) {
        final String sql = "SELECT COUNT(*) AS c FROM patients WHERE id = ?";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt("c") > 0;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error checking patient existence", e);
        }
    }

    @Override
    public boolean existsByPatientCode(String patientCode) {
        final String sql = "SELECT COUNT(*) AS c FROM patients WHERE patient_code = ?";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, patientCode);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt("c") > 0;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error checking patient code uniqueness", e);
        }
    }

    @Override
    public int count() {
        final String sql = "SELECT COUNT(*) AS c FROM patients";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt("c");
            return 0;
        } catch (SQLException e) {
            throw new DatabaseException("Error counting patients", e);
        }
    }

    @Override
    public int maxPatientCodeNumber() {
        int max = 0;
        final String sql = "SELECT patient_code FROM patients WHERE patient_code LIKE 'PAT-%'";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String code = rs.getString("patient_code");
                if (code == null) continue;
                String suffix = code.substring("PAT-".length());
                try {
                    int n = Integer.parseInt(suffix);
                    if (n > max) max = n;
                } catch (NumberFormatException ignored) {
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error reading max patient code", e);
        }
        return max;
    }

    // ---------- helpers ----------

    private Patient mapRow(ResultSet rs) throws SQLException {
        Patient p = new Patient();
        p.setId(rs.getInt("id"));
        p.setPatientCode(rs.getString("patient_code"));
        p.setFullName(rs.getString("full_name"));
        String dob = rs.getString("date_of_birth");
        p.setDateOfBirth(dob == null || dob.isBlank() ? null : LocalDate.parse(dob));
        p.setGender(rs.getString("gender"));
        p.setPhone(rs.getString("phone"));
        p.setEmail(rs.getString("email"));
        p.setAddress(rs.getString("address"));
        p.setEmergencyContactName(rs.getString("emergency_contact_name"));
        p.setEmergencyContactPhone(rs.getString("emergency_contact_phone"));
        p.setBloodGroup(rs.getString("blood_group"));
        p.setActive(rs.getInt("is_active") == 1);
        p.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));
        p.setUpdatedAt(toLocalDateTime(rs.getTimestamp("updated_at")));
        return p;
    }

    private LocalDateTime toLocalDateTime(Timestamp ts) {
        return ts == null ? LocalDateTime.now() : ts.toLocalDateTime();
    }
}
