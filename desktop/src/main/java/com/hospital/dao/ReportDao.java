package com.hospital.dao;

import com.hospital.config.DatabaseConnection;
import com.hospital.exception.DatabaseException;
import com.hospital.model.DashboardStats;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Read-only DAO for dashboard aggregates and report queries (Phase 7).
 *
 * <p>Returns generic column data as {@code List<List<Object>>} so the reports
 * screen can render multiple report types without creating a model per report.
 */
public class ReportDao {

    private final DatabaseConnection dbConnection;

    public ReportDao(DatabaseConnection dbConnection) {
        this.dbConnection = dbConnection;
    }

    public DatabaseConnection getDbConnection() {
        return dbConnection;
    }

    public DashboardStats getDashboardStats() {
        DashboardStats s = new DashboardStats();
        String today = LocalDate.now().toString();
        String totalPatientsSql = "SELECT COUNT(*) AS c FROM patients";
        String activeDoctorsSql = "SELECT COUNT(*) AS c FROM doctors WHERE is_active = 1";
        String todayApptsSql = "SELECT COUNT(*) AS c FROM appointments WHERE appointment_date = ?";
        String pendingApptsSql = "SELECT COUNT(*) AS c FROM appointments WHERE status = 'SCHEDULED'";
        String completedApptsSql = "SELECT COUNT(*) AS c FROM appointments WHERE status = 'COMPLETED'";
        String unpaidBillsSql = "SELECT COUNT(*) AS c FROM bills WHERE status = 'UNPAID'";
        String partialBillsSql = "SELECT COUNT(*) AS c FROM bills WHERE status = 'PARTIALLY_PAID'";
        // Today's revenue: sum of total_amount for bills marked PAID on bill_date = today
        // (we treat PAID bills dated today as today's revenue; record-keeping only).
        String todayRevenueSql = "SELECT COALESCE(SUM(total_amount), 0) AS s FROM bills " +
                "WHERE bill_date = ? AND status = 'PAID'";

        try (Connection conn = dbConnection.getConnection()) {
            s.setTotalPatients(execInt(conn, totalPatientsSql));
            s.setActiveDoctors(execInt(conn, activeDoctorsSql));
            s.setTodaysAppointments(execIntParam(conn, todayApptsSql, today));
            s.setPendingAppointments(execInt(conn, pendingApptsSql));
            s.setCompletedAppointments(execInt(conn, completedApptsSql));
            s.setUnpaidBills(execInt(conn, unpaidBillsSql));
            s.setPartiallyPaidBills(execInt(conn, partialBillsSql));
            s.setTodaysRevenue(execBigDecimalParam(conn, todayRevenueSql, today));
            return s;
        } catch (SQLException e) {
            throw new DatabaseException("Error loading dashboard statistics", e);
        }
    }

    /**
     * Generic parameterized report query. Returns a list of rows, where each
     * row is a list of Objects matching the columns produced by {@code sql}.
     *
     * @param sql    parameterized SELECT statement
     * @param params positional parameters in order
     */
    public List<List<Object>> query(String sql, Object... params) {
        List<List<Object>> rows = new ArrayList<>();
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                Object o = params[i];
                if (o == null) ps.setObject(i + 1, null);
                else if (o instanceof String st) ps.setString(i + 1, st);
                else if (o instanceof Integer ii) ps.setInt(i + 1, ii);
                else if (o instanceof Long ll) ps.setLong(i + 1, ll);
                else if (o instanceof BigDecimal bd) ps.setBigDecimal(i + 1, bd);
                else ps.setObject(i + 1, o);
            }
            try (ResultSet rs = ps.executeQuery()) {
                int cols = rs.getMetaData().getColumnCount();
                while (rs.next()) {
                    List<Object> row = new ArrayList<>(cols);
                    for (int c = 1; c <= cols; c++) {
                        row.add(rs.getObject(c));
                    }
                    rows.add(row);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error executing report query", e);
        }
        return rows;
    }

    public BigDecimal sum(String sql, Object... params) {
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                Object o = params[i];
                if (o instanceof String s) ps.setString(i + 1, s);
                else if (o instanceof Integer ii) ps.setInt(i + 1, ii);
                else ps.setObject(i + 1, o);
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    BigDecimal v = rs.getBigDecimal(1);
                    return v == null ? BigDecimal.ZERO : v;
                }
                return BigDecimal.ZERO;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error executing report sum", e);
        }
    }

    private int execInt(Connection conn, String sql) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
            return 0;
        }
    }

    private int execIntParam(Connection conn, String sql, String p) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, p);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
                return 0;
            }
        }
    }

    private BigDecimal execBigDecimalParam(Connection conn, String sql, String p) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, p);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    BigDecimal v = rs.getBigDecimal(1);
                    return v == null ? BigDecimal.ZERO : v;
                }
                return BigDecimal.ZERO;
            }
        }
    }
}
