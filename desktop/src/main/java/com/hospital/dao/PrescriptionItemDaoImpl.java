package com.hospital.dao;

import com.hospital.config.DatabaseConnection;
import com.hospital.exception.DatabaseException;
import com.hospital.model.PrescriptionItem;

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

public class PrescriptionItemDaoImpl implements PrescriptionItemDao {

    private final DatabaseConnection dbConnection;

    public PrescriptionItemDaoImpl(DatabaseConnection dbConnection) {
        this.dbConnection = dbConnection;
    }

    @Override
    public int create(int prescriptionId, String medicineName, String dosage,
                      String frequency, String duration, String instructions) {
        try (Connection conn = dbConnection.getConnection()) {
            return create(conn, prescriptionId, medicineName, dosage, frequency, duration, instructions);
        } catch (SQLException e) {
            throw new DatabaseException("Error creating prescription item", e);
        }
    }

    public int create(Connection conn, int prescriptionId, String medicineName, String dosage,
                      String frequency, String duration, String instructions) {
        final String sql = "INSERT INTO prescription_items " +
                "(prescription_id, medicine_name, dosage, frequency, duration, instructions) " +
                "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, prescriptionId);
            ps.setString(2, medicineName);
            ps.setString(3, dosage);
            ps.setString(4, frequency);
            ps.setString(5, duration);
            setNullableString(ps, 6, instructions);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
                throw new DatabaseException("Failed to retrieve generated prescription item id");
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error creating prescription item", e);
        }
    }

    @Override
    public Optional<PrescriptionItem> findById(int id) {
        final String sql = "SELECT id, prescription_id, medicine_name, dosage, frequency, duration, instructions, " +
                "created_at, updated_at FROM prescription_items WHERE id = ?";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error looking up prescription item id=" + id, e);
        }
    }

    @Override
    public List<PrescriptionItem> findByPrescriptionId(int prescriptionId) {
        List<PrescriptionItem> result = new ArrayList<>();
        final String sql = "SELECT id, prescription_id, medicine_name, dosage, frequency, duration, instructions, " +
                "created_at, updated_at FROM prescription_items WHERE prescription_id = ? ORDER BY id ASC";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, prescriptionId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error listing items for prescription id=" + prescriptionId, e);
        }
        return result;
    }

    @Override
    public void update(int id, String medicineName, String dosage, String frequency,
                       String duration, String instructions) {
        final String sql = "UPDATE prescription_items SET medicine_name = ?, dosage = ?, frequency = ?, " +
                "duration = ?, instructions = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, medicineName);
            ps.setString(2, dosage);
            ps.setString(3, frequency);
            ps.setString(4, duration);
            setNullableString(ps, 5, instructions);
            ps.setInt(6, id);
            int rows = ps.executeUpdate();
            if (rows == 0) throw new DatabaseException("Prescription item not found (id=" + id + ")");
        } catch (SQLException e) {
            throw new DatabaseException("Error updating prescription item id=" + id, e);
        }
    }

    @Override
    public void delete(int id) {
        final String sql = "DELETE FROM prescription_items WHERE id = ?";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Error deleting prescription item id=" + id, e);
        }
    }

    @Override
    public void deleteByPrescriptionId(int prescriptionId) {
        try (Connection conn = dbConnection.getConnection()) {
            deleteByPrescriptionId(conn, prescriptionId);
        } catch (SQLException e) {
            throw new DatabaseException("Error deleting items for prescription id=" + prescriptionId, e);
        }
    }

    public void deleteByPrescriptionId(Connection conn, int prescriptionId) {
        final String sql = "DELETE FROM prescription_items WHERE prescription_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, prescriptionId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Error deleting items for prescription id=" + prescriptionId, e);
        }
    }

    @Override
    public int count() {
        final String sql = "SELECT COUNT(*) AS c FROM prescription_items";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt("c");
            return 0;
        } catch (SQLException e) {
            throw new DatabaseException("Error counting prescription items", e);
        }
    }

    @Override
    public int countByPrescriptionId(int prescriptionId) {
        final String sql = "SELECT COUNT(*) AS c FROM prescription_items WHERE prescription_id = ?";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, prescriptionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("c");
                return 0;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error counting items for prescription id=" + prescriptionId, e);
        }
    }

    // ---------- helpers ----------

    private void setNullableString(PreparedStatement ps, int idx, String value) throws SQLException {
        if (value == null) ps.setNull(idx, java.sql.Types.VARCHAR);
        else ps.setString(idx, value);
    }

    private PrescriptionItem mapRow(ResultSet rs) throws SQLException {
        PrescriptionItem it = new PrescriptionItem();
        it.setId(rs.getInt("id"));
        it.setPrescriptionId(rs.getInt("prescription_id"));
        it.setMedicineName(rs.getString("medicine_name"));
        it.setDosage(rs.getString("dosage"));
        it.setFrequency(rs.getString("frequency"));
        it.setDuration(rs.getString("duration"));
        it.setInstructions(rs.getString("instructions"));
        it.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));
        it.setUpdatedAt(toLocalDateTime(rs.getTimestamp("updated_at")));
        return it;
    }

    private LocalDateTime toLocalDateTime(Timestamp ts) {
        return ts == null ? LocalDateTime.now() : ts.toLocalDateTime();
    }
}
