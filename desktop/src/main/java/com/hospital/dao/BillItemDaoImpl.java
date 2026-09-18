package com.hospital.dao;

import com.hospital.config.DatabaseConnection;
import com.hospital.exception.DatabaseException;
import com.hospital.model.BillItem;

import java.math.BigDecimal;
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

public class BillItemDaoImpl implements BillItemDao {

    private final DatabaseConnection dbConnection;

    public BillItemDaoImpl(DatabaseConnection dbConnection) {
        this.dbConnection = dbConnection;
    }

    @Override
    public int create(int billId, String description, int quantity,
                      BigDecimal unitPrice, BigDecimal amount) {
        try (Connection conn = dbConnection.getConnection()) {
            return create(conn, billId, description, quantity, unitPrice, amount);
        } catch (SQLException e) {
            throw new DatabaseException("Error creating bill item", e);
        }
    }

    public int create(Connection conn, int billId, String description, int quantity,
                      BigDecimal unitPrice, BigDecimal amount) {
        final String sql = "INSERT INTO bill_items " +
                "(bill_id, description, quantity, unit_price, amount) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, billId);
            ps.setString(2, description);
            ps.setInt(3, quantity);
            ps.setBigDecimal(4, unitPrice);
            ps.setBigDecimal(5, amount);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
                throw new DatabaseException("Failed to retrieve generated bill item id");
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error creating bill item", e);
        }
    }

    @Override
    public Optional<BillItem> findById(int id) {
        final String sql = "SELECT id, bill_id, description, quantity, unit_price, amount, " +
                "created_at, updated_at FROM bill_items WHERE id = ?";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error looking up bill item id=" + id, e);
        }
    }

    @Override
    public List<BillItem> findByBillId(int billId) {
        List<BillItem> result = new ArrayList<>();
        final String sql = "SELECT id, bill_id, description, quantity, unit_price, amount, " +
                "created_at, updated_at FROM bill_items WHERE bill_id = ? ORDER BY id ASC";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, billId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error listing items for bill id=" + billId, e);
        }
        return result;
    }

    @Override
    public void update(int id, String description, int quantity,
                       BigDecimal unitPrice, BigDecimal amount) {
        final String sql = "UPDATE bill_items SET description = ?, quantity = ?, unit_price = ?, " +
                "amount = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, description);
            ps.setInt(2, quantity);
            ps.setBigDecimal(3, unitPrice);
            ps.setBigDecimal(4, amount);
            ps.setInt(5, id);
            int rows = ps.executeUpdate();
            if (rows == 0) throw new DatabaseException("Bill item not found (id=" + id + ")");
        } catch (SQLException e) {
            throw new DatabaseException("Error updating bill item id=" + id, e);
        }
    }

    @Override
    public void delete(int id) {
        final String sql = "DELETE FROM bill_items WHERE id = ?";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Error deleting bill item id=" + id, e);
        }
    }

    @Override
    public void deleteAllByBillId(int billId) {
        try (Connection conn = dbConnection.getConnection()) {
            deleteAllByBillId(conn, billId);
        } catch (SQLException e) {
            throw new DatabaseException("Error deleting items for bill id=" + billId, e);
        }
    }

    public void deleteAllByBillId(Connection conn, int billId) {
        final String sql = "DELETE FROM bill_items WHERE bill_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, billId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Error deleting items for bill id=" + billId, e);
        }
    }

    @Override
    public int count() {
        final String sql = "SELECT COUNT(*) AS c FROM bill_items";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt("c");
            return 0;
        } catch (SQLException e) {
            throw new DatabaseException("Error counting bill items", e);
        }
    }

    private BillItem mapRow(ResultSet rs) throws SQLException {
        BillItem it = new BillItem();
        it.setId(rs.getInt("id"));
        it.setBillId(rs.getInt("bill_id"));
        it.setDescription(rs.getString("description"));
        it.setQuantity(rs.getInt("quantity"));
        it.setUnitPrice(rs.getBigDecimal("unit_price"));
        it.setAmount(rs.getBigDecimal("amount"));
        it.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));
        it.setUpdatedAt(toLocalDateTime(rs.getTimestamp("updated_at")));
        return it;
    }

    private LocalDateTime toLocalDateTime(Timestamp ts) {
        return ts == null ? LocalDateTime.now() : ts.toLocalDateTime();
    }
}
