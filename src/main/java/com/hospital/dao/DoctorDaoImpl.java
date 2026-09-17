package com.hospital.dao;

import com.hospital.config.DatabaseConnection;
import com.hospital.exception.DatabaseException;
import com.hospital.model.Doctor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DoctorDaoImpl implements DoctorDao {

    private static final String BASE_SELECT =
            "SELECT d.id, d.user_id, d.department_id, d.full_name, d.specialization, d.phone, d.email, " +
            "d.consultation_fee, d.is_active, d.created_at, d.updated_at, " +
            "dp.name AS department_name, u.username AS username " +
            "FROM doctors d " +
            "LEFT JOIN departments dp ON dp.id = d.department_id " +
            "LEFT JOIN users u ON u.id = d.user_id ";

    private final DatabaseConnection dbConnection;

    public DoctorDaoImpl(DatabaseConnection dbConnection) {
        this.dbConnection = dbConnection;
    }

    @Override
    public int create(Integer userId, int departmentId, String fullName, String specialization,
                      String phone, String email, double consultationFee) {
        final String sql = "INSERT INTO doctors (user_id, department_id, full_name, specialization, phone, email, consultation_fee) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            if (userId == null) ps.setNull(1, java.sql.Types.INTEGER);
            else ps.setInt(1, userId);
            ps.setInt(2, departmentId);
            ps.setString(3, fullName);
            ps.setString(4, specialization);
            ps.setString(5, phone);
            ps.setString(6, email);
            ps.setDouble(7, consultationFee);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
                throw new DatabaseException("Failed to retrieve generated doctor id");
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error creating doctor: " + fullName, e);
        }
    }

    @Override
    public Optional<Doctor> findById(int id) {
        final String sql = BASE_SELECT + "WHERE d.id = ?";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error looking up doctor id=" + id, e);
        }
    }

    @Override
    public List<Doctor> findAll() {
        return search(null, 0);
    }

    @Override
    public void update(int id, Integer userId, int departmentId, String fullName, String specialization,
                       String phone, String email, double consultationFee) {
        final String sql = "UPDATE doctors SET user_id = ?, department_id = ?, full_name = ?, specialization = ?, " +
                "phone = ?, email = ?, consultation_fee = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (userId == null) ps.setNull(1, java.sql.Types.INTEGER);
            else ps.setInt(1, userId);
            ps.setInt(2, departmentId);
            ps.setString(3, fullName);
            ps.setString(4, specialization);
            ps.setString(5, phone);
            ps.setString(6, email);
            ps.setDouble(7, consultationFee);
            ps.setInt(8, id);
            int rows = ps.executeUpdate();
            if (rows == 0) throw new DatabaseException("Doctor not found (id=" + id + ")");
        } catch (SQLException e) {
            throw new DatabaseException("Error updating doctor id=" + id, e);
        }
    }

    @Override
    public void updateActiveStatus(int id, boolean active) {
        final String sql = "UPDATE doctors SET is_active = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, active ? 1 : 0);
            ps.setInt(2, id);
            int rows = ps.executeUpdate();
            if (rows == 0) throw new DatabaseException("Doctor not found (id=" + id + ")");
        } catch (SQLException e) {
            throw new DatabaseException("Error updating active status for doctor id=" + id, e);
        }
    }

    @Override
    public List<Doctor> search(String query, int departmentId) {
        List<Doctor> result = new ArrayList<>();
        StringBuilder sql = new StringBuilder(BASE_SELECT);
        List<String> where = new ArrayList<>();

        String like = null;
        if (query != null && !query.isBlank()) {
            like = "%" + query.trim().toLowerCase() + "%";
            where.add("(LOWER(d.full_name) LIKE ? OR LOWER(d.specialization) LIKE ? " +
                    "OR LOWER(COALESCE(d.phone,'')) LIKE ? OR LOWER(COALESCE(d.email,'')) LIKE ?)");
        }
        if (departmentId > 0) {
            where.add("d.department_id = ?");
        }
        if (!where.isEmpty()) sql.append("WHERE ").append(String.join(" AND ", where)).append(" ");
        sql.append("ORDER BY d.full_name COLLATE NOCASE ASC");

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            int idx = 1;
            if (like != null) {
                ps.setString(idx++, like);
                ps.setString(idx++, like);
                ps.setString(idx++, like);
                ps.setString(idx++, like);
            }
            if (departmentId > 0) {
                ps.setInt(idx++, departmentId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error searching doctors", e);
        }
        return result;
    }

    @Override
    public boolean existsById(int id) {
        final String sql = "SELECT COUNT(*) AS c FROM doctors WHERE id = ?";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt("c") > 0;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error checking doctor existence", e);
        }
    }

    @Override
    public int count() {
        final String sql = "SELECT COUNT(*) AS c FROM doctors";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt("c");
            return 0;
        } catch (SQLException e) {
            throw new DatabaseException("Error counting doctors", e);
        }
    }

    // ---------- helpers ----------

    private Doctor mapRow(ResultSet rs) throws SQLException {
        Doctor d = new Doctor();
        d.setId(rs.getInt("id"));
        int uid = rs.getInt("user_id");
        d.setUserId(rs.wasNull() ? null : uid);
        d.setDepartmentId(rs.getInt("department_id"));
        d.setFullName(rs.getString("full_name"));
        d.setSpecialization(rs.getString("specialization"));
        d.setPhone(rs.getString("phone"));
        d.setEmail(rs.getString("email"));
        d.setConsultationFee(rs.getDouble("consultation_fee"));
        d.setActive(rs.getInt("is_active") == 1);
        d.setDepartmentName(rs.getString("department_name"));
        d.setUsername(rs.getString("username"));
        d.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));
        d.setUpdatedAt(toLocalDateTime(rs.getTimestamp("updated_at")));
        return d;
    }

    private LocalDateTime toLocalDateTime(Timestamp ts) {
        return ts == null ? LocalDateTime.now() : ts.toLocalDateTime();
    }
}
