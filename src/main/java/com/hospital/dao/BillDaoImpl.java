package com.hospital.dao;

import com.hospital.config.DatabaseConnection;
import com.hospital.exception.DatabaseException;
import com.hospital.model.Bill;

import java.math.BigDecimal;
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

public class BillDaoImpl implements BillDao {

    private static final String BASE_SELECT =
            "SELECT b.id, b.bill_number, b.patient_id, b.appointment_id, b.bill_date, b.status, " +
            "b.notes, b.subtotal, b.discount, b.total_amount, b.created_at, b.updated_at, " +
            "p.full_name AS patient_name, p.patient_code AS patient_code, " +
            "a.appointment_date AS appointment_date, a.appointment_time AS appointment_time, " +
            "d.full_name AS doctor_name, " +
            "(SELECT COUNT(*) FROM bill_items bi WHERE bi.bill_id = b.id) AS item_count " +
            "FROM bills b " +
            "LEFT JOIN patients p    ON p.id = b.patient_id " +
            "LEFT JOIN appointments a ON a.id = b.appointment_id " +
            "LEFT JOIN doctors d     ON d.id = a.doctor_id ";

    private final DatabaseConnection dbConnection;

    public BillDaoImpl(DatabaseConnection dbConnection) {
        this.dbConnection = dbConnection;
    }

    @Override
    public int create(String billNumber, int patientId, Integer appointmentId,
                      String billDate, String status, String notes,
                      BigDecimal subtotal, BigDecimal discount, BigDecimal totalAmount) {
        try (Connection conn = dbConnection.getConnection()) {
            return create(conn, billNumber, patientId, appointmentId, billDate, status, notes,
                    subtotal, discount, totalAmount);
        } catch (SQLException e) {
            throw new DatabaseException("Error creating bill", e);
        }
    }

    public int create(Connection conn, String billNumber, int patientId, Integer appointmentId,
                      String billDate, String status, String notes,
                      BigDecimal subtotal, BigDecimal discount, BigDecimal totalAmount) {
        final String sql = "INSERT INTO bills " +
                "(bill_number, patient_id, appointment_id, bill_date, status, notes, " +
                "subtotal, discount, total_amount) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, billNumber);
            ps.setInt(2, patientId);
            setNullableInt(ps, 3, appointmentId);
            ps.setString(4, billDate);
            ps.setString(5, status);
            setNullableString(ps, 6, notes);
            ps.setBigDecimal(7, subtotal);
            ps.setBigDecimal(8, discount);
            ps.setBigDecimal(9, totalAmount);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
                throw new DatabaseException("Failed to retrieve generated bill id");
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error creating bill", e);
        }
    }

    @Override
    public Optional<Bill> findById(int id) {
        final String sql = BASE_SELECT + "WHERE b.id = ?";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error looking up bill id=" + id, e);
        }
    }

    @Override
    public Optional<Bill> findByBillNumber(String billNumber) {
        final String sql = BASE_SELECT + "WHERE b.bill_number = ?";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, billNumber);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error looking up bill number=" + billNumber, e);
        }
    }

    @Override
    public List<Bill> findAll() {
        return search(null, null);
    }

    @Override
    public void update(int id, Integer appointmentId, String billDate, String notes,
                       BigDecimal subtotal, BigDecimal discount, BigDecimal totalAmount) {
        try (Connection conn = dbConnection.getConnection()) {
            update(conn, id, appointmentId, billDate, notes, subtotal, discount, totalAmount);
        } catch (SQLException e) {
            throw new DatabaseException("Error updating bill id=" + id, e);
        }
    }

    public void update(Connection conn, int id, Integer appointmentId, String billDate, String notes,
                       BigDecimal subtotal, BigDecimal discount, BigDecimal totalAmount) {
        final String sql = "UPDATE bills SET appointment_id = ?, bill_date = ?, notes = ?, " +
                "subtotal = ?, discount = ?, total_amount = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            setNullableInt(ps, 1, appointmentId);
            ps.setString(2, billDate);
            setNullableString(ps, 3, notes);
            ps.setBigDecimal(4, subtotal);
            ps.setBigDecimal(5, discount);
            ps.setBigDecimal(6, totalAmount);
            ps.setInt(7, id);
            int rows = ps.executeUpdate();
            if (rows == 0) throw new DatabaseException("Bill not found (id=" + id + ")");
        } catch (SQLException e) {
            throw new DatabaseException("Error updating bill id=" + id, e);
        }
    }

    @Override
    public void updateStatus(int id, String status) {
        try (Connection conn = dbConnection.getConnection()) {
            updateStatus(conn, id, status);
        } catch (SQLException e) {
            throw new DatabaseException("Error updating bill status id=" + id, e);
        }
    }

    public void updateStatus(Connection conn, int id, String status) {
        final String sql = "UPDATE bills SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, id);
            int rows = ps.executeUpdate();
            if (rows == 0) throw new DatabaseException("Bill not found (id=" + id + ")");
        } catch (SQLException e) {
            throw new DatabaseException("Error updating bill status id=" + id, e);
        }
    }

    @Override
    public void updateBillNumber(int id, String billNumber) {
        try (Connection conn = dbConnection.getConnection()) {
            updateBillNumber(conn, id, billNumber);
        } catch (SQLException e) {
            throw new DatabaseException("Error updating bill number id=" + id, e);
        }
    }

    public void updateBillNumber(Connection conn, int id, String billNumber) {
        final String sql = "UPDATE bills SET bill_number = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, billNumber);
            ps.setInt(2, id);
            int rows = ps.executeUpdate();
            if (rows == 0) throw new DatabaseException("Bill not found (id=" + id + ")");
        } catch (SQLException e) {
            throw new DatabaseException("Error updating bill number id=" + id, e);
        }
    }

    @Override
    public List<Bill> search(String query, String status) {
        List<Bill> result = new ArrayList<>();
        StringBuilder sql = new StringBuilder(BASE_SELECT);
        List<String> where = new ArrayList<>();
        List<Object> params = new ArrayList<>();

        if (query != null && !query.isBlank()) {
            String like = "%" + query.trim().toLowerCase() + "%";
            where.add("(LOWER(b.bill_number) LIKE ? " +
                    "OR LOWER(COALESCE(p.full_name,'')) LIKE ? " +
                    "OR LOWER(COALESCE(p.patient_code,'')) LIKE ? " +
                    "OR LOWER(COALESCE(d.full_name,'')) LIKE ? " +
                    "OR EXISTS (SELECT 1 FROM bill_items bi " +
                    "          WHERE bi.bill_id = b.id " +
                    "            AND LOWER(COALESCE(bi.description,'')) LIKE ?))");
            for (int i = 0; i < 5; i++) params.add(like);
        }
        if (status != null && !status.isBlank()) {
            where.add("b.status = ?");
            params.add(status.trim().toUpperCase());
        }
        if (!where.isEmpty()) {
            sql.append("WHERE ").append(String.join(" AND ", where)).append(" ");
        }
        sql.append("ORDER BY b.bill_date DESC, b.id DESC");

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                Object o = params.get(i);
                if (o instanceof String s) ps.setString(i + 1, s);
                else if (o instanceof Integer ii) ps.setInt(i + 1, ii);
                else ps.setObject(i + 1, o);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error searching bills", e);
        }
        return result;
    }

    @Override
    public int count() {
        final String sql = "SELECT COUNT(*) AS c FROM bills";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt("c");
            return 0;
        } catch (SQLException e) {
            throw new DatabaseException("Error counting bills", e);
        }
    }

    // ---------- helpers ----------

    private void setNullableString(PreparedStatement ps, int idx, String value) throws SQLException {
        if (value == null) ps.setNull(idx, java.sql.Types.VARCHAR);
        else ps.setString(idx, value);
    }

    private void setNullableInt(PreparedStatement ps, int idx, Integer value) throws SQLException {
        if (value == null) ps.setNull(idx, java.sql.Types.INTEGER);
        else ps.setInt(idx, value);
    }

    private Bill mapRow(ResultSet rs) throws SQLException {
        Bill b = new Bill();
        b.setId(rs.getInt("id"));
        b.setBillNumber(rs.getString("bill_number"));
        b.setPatientId(rs.getInt("patient_id"));
        int apptId = rs.getInt("appointment_id");
        b.setAppointmentId(rs.wasNull() ? null : apptId);
        String d = rs.getString("bill_date");
        b.setBillDate(d == null || d.isBlank() ? null : LocalDate.parse(d));
        b.setStatus(rs.getString("status"));
        b.setNotes(rs.getString("notes"));
        b.setSubtotal(rs.getBigDecimal("subtotal"));
        b.setDiscount(rs.getBigDecimal("discount"));
        b.setTotalAmount(rs.getBigDecimal("total_amount"));
        b.setPatientName(rs.getString("patient_name"));
        b.setPatientCode(rs.getString("patient_code"));
        b.setAppointmentDate(rs.getString("appointment_date"));
        b.setAppointmentTime(rs.getString("appointment_time"));
        b.setDoctorName(rs.getString("doctor_name"));
        b.setItemCount(rs.getInt("item_count"));
        b.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));
        b.setUpdatedAt(toLocalDateTime(rs.getTimestamp("updated_at")));
        return b;
    }

    private LocalDateTime toLocalDateTime(Timestamp ts) {
        return ts == null ? LocalDateTime.now() : ts.toLocalDateTime();
    }
}
