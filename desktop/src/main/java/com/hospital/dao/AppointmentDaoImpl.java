package com.hospital.dao;

import com.hospital.config.DatabaseConnection;
import com.hospital.exception.DatabaseException;
import com.hospital.model.Appointment;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AppointmentDaoImpl implements AppointmentDao {

    private static final String BASE_SELECT =
            "SELECT a.id, a.patient_id, a.doctor_id, a.appointment_date, a.appointment_time, " +
            "a.reason, a.status, a.notes, a.created_at, a.updated_at, " +
            "p.full_name AS patient_name, p.patient_code AS patient_code, " +
            "d.full_name AS doctor_name, d.specialization AS specialization " +
            "FROM appointments a " +
            "LEFT JOIN patients p ON p.id = a.patient_id " +
            "LEFT JOIN doctors d  ON d.id = a.doctor_id ";

    private final DatabaseConnection dbConnection;

    public AppointmentDaoImpl(DatabaseConnection dbConnection) {
        this.dbConnection = dbConnection;
    }

    @Override
    public int create(int patientId, int doctorId, String appointmentDate, String appointmentTime,
                      String reason, String notes) {
        final String sql = "INSERT INTO appointments (patient_id, doctor_id, appointment_date, appointment_time, reason, notes) " +
                "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, patientId);
            ps.setInt(2, doctorId);
            ps.setString(3, appointmentDate);
            ps.setString(4, appointmentTime);
            ps.setString(5, reason);
            ps.setString(6, notes);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
                throw new DatabaseException("Failed to retrieve generated appointment id");
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error creating appointment", e);
        }
    }

    @Override
    public Optional<Appointment> findById(int id) {
        final String sql = BASE_SELECT + "WHERE a.id = ?";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error looking up appointment id=" + id, e);
        }
    }

    @Override
    public List<Appointment> findAll() {
        return search(null, null, null, 0);
    }

    @Override
    public void update(int id, int patientId, int doctorId, String appointmentDate, String appointmentTime,
                       String reason, String notes) {
        final String sql = "UPDATE appointments SET patient_id = ?, doctor_id = ?, appointment_date = ?, " +
                "appointment_time = ?, reason = ?, notes = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, patientId);
            ps.setInt(2, doctorId);
            ps.setString(3, appointmentDate);
            ps.setString(4, appointmentTime);
            ps.setString(5, reason);
            ps.setString(6, notes);
            ps.setInt(7, id);
            int rows = ps.executeUpdate();
            if (rows == 0) throw new DatabaseException("Appointment not found (id=" + id + ")");
        } catch (SQLException e) {
            throw new DatabaseException("Error updating appointment id=" + id, e);
        }
    }

    @Override
    public void updateStatus(int id, String status) {
        final String sql = "UPDATE appointments SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, id);
            int rows = ps.executeUpdate();
            if (rows == 0) throw new DatabaseException("Appointment not found (id=" + id + ")");
        } catch (SQLException e) {
            throw new DatabaseException("Error updating status for appointment id=" + id, e);
        }
    }

    @Override
    public List<Appointment> search(String query, String status, LocalDate dateFilter, int doctorId) {
        List<Appointment> result = new ArrayList<>();
        StringBuilder sql = new StringBuilder(BASE_SELECT);
        List<String> where = new ArrayList<>();

        String like = null;
        if (query != null && !query.isBlank()) {
            like = "%" + query.trim().toLowerCase() + "%";
            where.add("(LOWER(COALESCE(p.full_name,'')) LIKE ? OR LOWER(COALESCE(p.patient_code,'')) LIKE ? " +
                    "OR LOWER(COALESCE(d.full_name,'')) LIKE ? OR LOWER(COALESCE(a.reason,'')) LIKE ?)");
        }
        if (status != null && !status.isBlank()) {
            where.add("a.status = ?");
        }
        if (dateFilter != null) {
            where.add("a.appointment_date = ?");
        }
        if (doctorId > 0) {
            where.add("a.doctor_id = ?");
        }
        if (!where.isEmpty()) sql.append("WHERE ").append(String.join(" AND ", where)).append(" ");
        sql.append("ORDER BY a.appointment_date DESC, a.appointment_time ASC");

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            int idx = 1;
            if (like != null) {
                ps.setString(idx++, like);
                ps.setString(idx++, like);
                ps.setString(idx++, like);
                ps.setString(idx++, like);
            }
            if (status != null && !status.isBlank()) {
                ps.setString(idx++, status);
            }
            if (dateFilter != null) {
                ps.setString(idx++, dateFilter.toString());
            }
            if (doctorId > 0) {
                ps.setInt(idx++, doctorId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error searching appointments", e);
        }
        return result;
    }

    @Override
    public boolean hasConflict(int doctorId, String appointmentDate, String appointmentTime, int excludeId) {
        StringBuilder sql = new StringBuilder(
                "SELECT COUNT(*) AS c FROM appointments WHERE doctor_id = ? AND appointment_date = ? " +
                        "AND appointment_time = ? AND status <> 'CANCELLED'");
        if (excludeId > 0) sql.append(" AND id <> ?");
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            ps.setInt(1, doctorId);
            ps.setString(2, appointmentDate);
            ps.setString(3, appointmentTime);
            if (excludeId > 0) ps.setInt(4, excludeId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt("c") > 0;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error checking appointment conflict", e);
        }
    }

    @Override
    public int count() {
        final String sql = "SELECT COUNT(*) AS c FROM appointments";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt("c");
            return 0;
        } catch (SQLException e) {
            throw new DatabaseException("Error counting appointments", e);
        }
    }

    // ---------- helpers ----------

    private Appointment mapRow(ResultSet rs) throws SQLException {
        Appointment a = new Appointment();
        a.setId(rs.getInt("id"));
        a.setPatientId(rs.getInt("patient_id"));
        a.setDoctorId(rs.getInt("doctor_id"));
        String d = rs.getString("appointment_date");
        a.setAppointmentDate(d == null || d.isBlank() ? null : LocalDate.parse(d));
        String t = rs.getString("appointment_time");
        a.setAppointmentTime(t == null || t.isBlank() ? null : LocalTime.parse(t));
        a.setReason(rs.getString("reason"));
        a.setStatus(rs.getString("status"));
        a.setNotes(rs.getString("notes"));
        a.setPatientName(rs.getString("patient_name"));
        a.setPatientCode(rs.getString("patient_code"));
        a.setDoctorName(rs.getString("doctor_name"));
        a.setSpecialization(rs.getString("specialization"));
        a.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));
        a.setUpdatedAt(toLocalDateTime(rs.getTimestamp("updated_at")));
        return a;
    }

    private LocalDateTime toLocalDateTime(Timestamp ts) {
        return ts == null ? LocalDateTime.now() : ts.toLocalDateTime();
    }
}
