package com.hospital.dao;

import com.hospital.config.DatabaseConnection;
import com.hospital.exception.DatabaseException;
import com.hospital.model.MedicalRecord;

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

public class MedicalRecordDaoImpl implements MedicalRecordDao {

    private static final String BASE_SELECT =
            "SELECT mr.id, mr.appointment_id, mr.patient_id, mr.doctor_id, " +
            "mr.diagnosis, mr.symptoms, mr.examination, mr.treatment_notes, mr.record_date, " +
            "mr.created_at, mr.updated_at, " +
            "p.full_name AS patient_name, p.patient_code AS patient_code, " +
            "d.full_name AS doctor_name, " +
            "a.appointment_date AS appointment_date, a.appointment_time AS appointment_time " +
            "FROM medical_records mr " +
            "LEFT JOIN patients p     ON p.id = mr.patient_id " +
            "LEFT JOIN doctors d      ON d.id = mr.doctor_id " +
            "LEFT JOIN appointments a ON a.id = mr.appointment_id ";

    private final DatabaseConnection dbConnection;

    public MedicalRecordDaoImpl(DatabaseConnection dbConnection) {
        this.dbConnection = dbConnection;
    }

    @Override
    public int create(int appointmentId, int patientId, int doctorId,
                      String diagnosis, String symptoms, String examination,
                      String treatmentNotes, String recordDate) {
        final String sql = "INSERT INTO medical_records " +
                "(appointment_id, patient_id, doctor_id, diagnosis, symptoms, examination, treatment_notes, record_date) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, appointmentId);
            ps.setInt(2, patientId);
            ps.setInt(3, doctorId);
            ps.setString(4, diagnosis);
            setNullableString(ps, 5, symptoms);
            setNullableString(ps, 6, examination);
            setNullableString(ps, 7, treatmentNotes);
            ps.setString(8, recordDate);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
                throw new DatabaseException("Failed to retrieve generated medical record id");
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error creating medical record", e);
        }
    }

    @Override
    public Optional<MedicalRecord> findById(int id) {
        final String sql = BASE_SELECT + "WHERE mr.id = ?";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error looking up medical record id=" + id, e);
        }
    }

    @Override
    public Optional<MedicalRecord> findByAppointment(int appointmentId) {
        final String sql = BASE_SELECT + "WHERE mr.appointment_id = ?";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, appointmentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error looking up medical record for appointment id=" + appointmentId, e);
        }
    }

    @Override
    public List<MedicalRecord> findAll() {
        return search(null);
    }

    @Override
    public void update(int id, String diagnosis, String symptoms, String examination,
                       String treatmentNotes, String recordDate) {
        final String sql = "UPDATE medical_records SET diagnosis = ?, symptoms = ?, examination = ?, " +
                "treatment_notes = ?, record_date = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, diagnosis);
            setNullableString(ps, 2, symptoms);
            setNullableString(ps, 3, examination);
            setNullableString(ps, 4, treatmentNotes);
            ps.setString(5, recordDate);
            ps.setInt(6, id);
            int rows = ps.executeUpdate();
            if (rows == 0) throw new DatabaseException("Medical record not found (id=" + id + ")");
        } catch (SQLException e) {
            throw new DatabaseException("Error updating medical record id=" + id, e);
        }
    }

    @Override
    public List<MedicalRecord> search(String query) {
        List<MedicalRecord> result = new ArrayList<>();
        StringBuilder sql = new StringBuilder(BASE_SELECT);

        String like = null;
        if (query != null && !query.isBlank()) {
            like = "%" + query.trim().toLowerCase() + "%";
            sql.append("WHERE (LOWER(COALESCE(p.full_name,'')) LIKE ? " +
                    "OR LOWER(COALESCE(p.patient_code,'')) LIKE ? " +
                    "OR LOWER(COALESCE(d.full_name,'')) LIKE ? " +
                    "OR LOWER(COALESCE(mr.diagnosis,'')) LIKE ? " +
                    "OR LOWER(COALESCE(mr.symptoms,'')) LIKE ?) ");
        }
        sql.append("ORDER BY mr.record_date DESC, mr.id DESC");

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            if (like != null) {
                ps.setString(1, like);
                ps.setString(2, like);
                ps.setString(3, like);
                ps.setString(4, like);
                ps.setString(5, like);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error searching medical records", e);
        }
        return result;
    }

    @Override
    public boolean existsByAppointment(int appointmentId) {
        final String sql = "SELECT COUNT(*) AS c FROM medical_records WHERE appointment_id = ?";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, appointmentId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt("c") > 0;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error checking medical record existence for appointment id=" + appointmentId, e);
        }
    }

    @Override
    public int count() {
        final String sql = "SELECT COUNT(*) AS c FROM medical_records";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt("c");
            return 0;
        } catch (SQLException e) {
            throw new DatabaseException("Error counting medical records", e);
        }
    }

    // ---------- helpers ----------

    private void setNullableString(PreparedStatement ps, int idx, String value) throws SQLException {
        if (value == null) ps.setNull(idx, java.sql.Types.VARCHAR);
        else ps.setString(idx, value);
    }

    private MedicalRecord mapRow(ResultSet rs) throws SQLException {
        MedicalRecord m = new MedicalRecord();
        m.setId(rs.getInt("id"));
        m.setAppointmentId(rs.getInt("appointment_id"));
        m.setPatientId(rs.getInt("patient_id"));
        m.setDoctorId(rs.getInt("doctor_id"));
        m.setDiagnosis(rs.getString("diagnosis"));
        m.setSymptoms(rs.getString("symptoms"));
        m.setExamination(rs.getString("examination"));
        m.setTreatmentNotes(rs.getString("treatment_notes"));
        String rd = rs.getString("record_date");
        m.setRecordDate(rd == null || rd.isBlank() ? null : LocalDate.parse(rd));
        m.setPatientName(rs.getString("patient_name"));
        m.setPatientCode(rs.getString("patient_code"));
        m.setDoctorName(rs.getString("doctor_name"));
        String ad = rs.getString("appointment_date");
        if (ad != null && !ad.isBlank()) m.setAppointmentDate(LocalDate.parse(ad));
        String at = rs.getString("appointment_time");
        if (at != null && !at.isBlank()) m.setAppointmentTime(LocalTime.parse(at));
        m.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));
        m.setUpdatedAt(toLocalDateTime(rs.getTimestamp("updated_at")));
        return m;
    }

    private LocalDateTime toLocalDateTime(Timestamp ts) {
        return ts == null ? LocalDateTime.now() : ts.toLocalDateTime();
    }
}
