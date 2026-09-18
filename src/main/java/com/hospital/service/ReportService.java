package com.hospital.service;

import com.hospital.dao.ReportDao;
import com.hospital.exception.AuthorizationException;
import com.hospital.exception.DatabaseException;
import com.hospital.exception.ValidationException;
import com.hospital.model.DashboardStats;
import com.hospital.model.Role;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Service layer for dashboard statistics and read-only reports (Phase 7).
 *
 * <p>Every authenticated user can view reports (reports are read-only).
 * Receptionists and doctors see the operational reports; admins see everything,
 * but since this phase's reports are all operational read-only views,
 * any logged-in user can run any report. Service still rejects unauthenticated
 * access, and nothing in reports allows mutation.
 */
public class ReportService {

    private final ReportDao reportDao;

    public ReportService(ReportDao reportDao) {
        this.reportDao = reportDao;
    }

    // ---------- dashboard ----------

    public DashboardStats getDashboardStats() {
        requireLoggedIn();
        return reportDao.getDashboardStats();
    }

    // ---------- reports ----------

    public ReportResult appointmentReport(String from, String to, String status, String search) {
        requireLoggedIn();
        validateDateRange(from, to);
        StringBuilder sql = new StringBuilder(
                "SELECT a.id, a.appointment_date, a.appointment_time, " +
                        "p.full_name AS patient_name, p.patient_code AS patient_code, " +
                        "d.full_name AS doctor_name, a.status, a.reason " +
                        "FROM appointments a " +
                        "LEFT JOIN patients p ON p.id = a.patient_id " +
                        "LEFT JOIN doctors d  ON d.id = a.doctor_id ");
        List<Object> params = new ArrayList<>();
        List<String> where = new ArrayList<>();
        applyDateRange(where, params, "a.appointment_date", from, to);
        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status.trim())) {
            where.add("a.status = ?");
            params.add(status.trim().toUpperCase());
        }
        if (search != null && !search.isBlank()) {
            String like = "%" + search.trim().toLowerCase() + "%";
            where.add("(LOWER(COALESCE(p.full_name,'')) LIKE ? OR LOWER(COALESCE(p.patient_code,'')) LIKE ? " +
                    "OR LOWER(COALESCE(d.full_name,'')) LIKE ? OR LOWER(COALESCE(a.reason,'')) LIKE ?)");
            for (int i = 0; i < 4; i++) params.add(like);
        }
        if (!where.isEmpty()) sql.append("WHERE ").append(String.join(" AND ", where)).append(" ");
        sql.append("ORDER BY a.appointment_date DESC, a.appointment_time DESC");
        List<String> columns = List.of("ID", "Date", "Time", "Patient", "Code", "Doctor", "Status", "Reason");
        return new ReportResult(columns, reportDao.query(sql.toString(), params.toArray()));
    }

    public ReportResult patientReport(String activeFilter, String search) {
        requireLoggedIn();
        StringBuilder sql = new StringBuilder(
                "SELECT p.patient_code, p.full_name, p.date_of_birth, p.gender, p.phone, " +
                        "p.blood_group, CASE WHEN p.is_active = 1 THEN 'Active' ELSE 'Inactive' END AS status " +
                        "FROM patients p ");
        List<Object> params = new ArrayList<>();
        List<String> where = new ArrayList<>();
        if (activeFilter != null && !activeFilter.isBlank() && !"ALL".equalsIgnoreCase(activeFilter.trim())) {
            boolean active = "ACTIVE".equalsIgnoreCase(activeFilter.trim());
            where.add("p.is_active = ?");
            params.add(active ? 1 : 0);
        }
        if (search != null && !search.isBlank()) {
            String like = "%" + search.trim().toLowerCase() + "%";
            where.add("(LOWER(p.full_name) LIKE ? OR LOWER(p.patient_code) LIKE ? " +
                    "OR LOWER(COALESCE(p.phone,'')) LIKE ?)");
            for (int i = 0; i < 3; i++) params.add(like);
        }
        if (!where.isEmpty()) sql.append("WHERE ").append(String.join(" AND ", where)).append(" ");
        sql.append("ORDER BY p.full_name COLLATE NOCASE ASC");
        List<String> columns = List.of("Code", "Name", "DOB", "Gender", "Phone", "Blood Group", "Status");
        return new ReportResult(columns, reportDao.query(sql.toString(), params.toArray()));
    }

    public ReportResult doctorReport(Integer departmentId, String activeFilter, String search) {
        requireLoggedIn();
        StringBuilder sql = new StringBuilder(
                "SELECT d.full_name, d.specialization, dep.name AS department, d.phone, d.email, " +
                        "d.consultation_fee, CASE WHEN d.is_active = 1 THEN 'Active' ELSE 'Inactive' END AS status " +
                        "FROM doctors d LEFT JOIN departments dep ON dep.id = d.department_id ");
        List<Object> params = new ArrayList<>();
        List<String> where = new ArrayList<>();
        if (departmentId != null && departmentId > 0) {
            where.add("d.department_id = ?");
            params.add(departmentId);
        }
        if (activeFilter != null && !activeFilter.isBlank() && !"ALL".equalsIgnoreCase(activeFilter.trim())) {
            boolean active = "ACTIVE".equalsIgnoreCase(activeFilter.trim());
            where.add("d.is_active = ?");
            params.add(active ? 1 : 0);
        }
        if (search != null && !search.isBlank()) {
            String like = "%" + search.trim().toLowerCase() + "%";
            where.add("(LOWER(d.full_name) LIKE ? OR LOWER(COALESCE(d.specialization,'')) LIKE ? " +
                    "OR LOWER(COALESCE(dep.name,'')) LIKE ?)");
            for (int i = 0; i < 3; i++) params.add(like);
        }
        if (!where.isEmpty()) sql.append("WHERE ").append(String.join(" AND ", where)).append(" ");
        sql.append("ORDER BY d.full_name COLLATE NOCASE ASC");
        List<String> columns = List.of("Doctor", "Specialization", "Department", "Phone", "Email", "Fee", "Status");
        return new ReportResult(columns, reportDao.query(sql.toString(), params.toArray()));
    }

    public ReportResult medicalRecordReport(String from, String to, String search) {
        requireLoggedIn();
        validateDateRange(from, to);
        StringBuilder sql = new StringBuilder(
                "SELECT mr.id, a.id, p.full_name, p.patient_code, d.full_name, mr.record_date, " +
                        "mr.diagnosis, mr.treatment_notes " +
                        "FROM medical_records mr " +
                        "LEFT JOIN patients p ON p.id = mr.patient_id " +
                        "LEFT JOIN doctors d  ON d.id = mr.doctor_id " +
                        "LEFT JOIN appointments a ON a.id = mr.appointment_id ");
        List<Object> params = new ArrayList<>();
        List<String> where = new ArrayList<>();
        applyDateRange(where, params, "mr.record_date", from, to);
        if (search != null && !search.isBlank()) {
            String like = "%" + search.trim().toLowerCase() + "%";
            where.add("(LOWER(COALESCE(p.full_name,'')) LIKE ? OR LOWER(COALESCE(p.patient_code,'')) LIKE ? " +
                    "OR LOWER(COALESCE(d.full_name,'')) LIKE ? OR LOWER(COALESCE(mr.diagnosis,'')) LIKE ?)");
            for (int i = 0; i < 4; i++) params.add(like);
        }
        if (!where.isEmpty()) sql.append("WHERE ").append(String.join(" AND ", where)).append(" ");
        sql.append("ORDER BY mr.record_date DESC, mr.id DESC");
        List<String> columns = List.of("Record", "Appointment", "Patient", "Code", "Doctor", "Date", "Diagnosis", "Treatment");
        return new ReportResult(columns, reportDao.query(sql.toString(), params.toArray()));
    }

    public ReportResult prescriptionReport(String from, String to, String search) {
        requireLoggedIn();
        validateDateRange(from, to);
        StringBuilder sql = new StringBuilder(
                "SELECT pr.id, pr.prescription_date, p.full_name, p.patient_code, d.full_name, " +
                        "GROUP_CONCAT(pi.medicine_name, ', ') AS medicines " +
                        "FROM prescriptions pr " +
                        "LEFT JOIN patients p ON p.id = pr.patient_id " +
                        "LEFT JOIN doctors d  ON d.id = pr.doctor_id " +
                        "LEFT JOIN prescription_items pi ON pi.prescription_id = pr.id ");
        List<Object> params = new ArrayList<>();
        List<String> where = new ArrayList<>();
        applyDateRange(where, params, "pr.prescription_date", from, to);
        if (search != null && !search.isBlank()) {
            String like = "%" + search.trim().toLowerCase() + "%";
            where.add("(LOWER(COALESCE(p.full_name,'')) LIKE ? OR LOWER(COALESCE(p.patient_code,'')) LIKE ? " +
                    "OR LOWER(COALESCE(d.full_name,'')) LIKE ? " +
                    "OR EXISTS (SELECT 1 FROM prescription_items pi2 WHERE pi2.prescription_id = pr.id " +
                    "          AND LOWER(COALESCE(pi2.medicine_name,'')) LIKE ?))");
            for (int i = 0; i < 4; i++) params.add(like);
        }
        if (!where.isEmpty()) sql.append("WHERE ").append(String.join(" AND ", where)).append(" ");
        sql.append("GROUP BY pr.id ORDER BY pr.prescription_date DESC, pr.id DESC");
        List<String> columns = List.of("ID", "Date", "Patient", "Code", "Doctor", "Medicines");
        return new ReportResult(columns, reportDao.query(sql.toString(), params.toArray()));
    }

    public BillingReportResult billingReport(String from, String to, String status, String search) {
        requireLoggedIn();
        validateDateRange(from, to);
        StringBuilder sql = new StringBuilder(
                "SELECT b.bill_number, b.bill_date, p.full_name, p.patient_code, b.appointment_id, " +
                        "b.subtotal, b.discount, b.total_amount, b.status " +
                        "FROM bills b " +
                        "LEFT JOIN patients p ON p.id = b.patient_id ");
        List<Object> params = new ArrayList<>();
        List<String> where = new ArrayList<>();
        applyDateRange(where, params, "b.bill_date", from, to);
        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status.trim())) {
            where.add("b.status = ?");
            params.add(status.trim().toUpperCase());
        }
        if (search != null && !search.isBlank()) {
            String like = "%" + search.trim().toLowerCase() + "%";
            where.add("(LOWER(b.bill_number) LIKE ? OR LOWER(COALESCE(p.full_name,'')) LIKE ? " +
                    "OR LOWER(COALESCE(p.patient_code,'')) LIKE ? " +
                    "OR EXISTS (SELECT 1 FROM bill_items bi WHERE bi.bill_id = b.id " +
                    "          AND LOWER(COALESCE(bi.description,'')) LIKE ?))");
            for (int i = 0; i < 4; i++) params.add(like);
        }
        if (!where.isEmpty()) sql.append("WHERE ").append(String.join(" AND ", where)).append(" ");
        sql.append("ORDER BY b.bill_date DESC, b.id DESC");

        // Aggregation query shares the same filters but we build simpler WHERE clauses
        // without patient/item joins to avoid double-counting rows.
        StringBuilder sumSql = new StringBuilder(
                "SELECT COUNT(*) AS c, COALESCE(SUM(b.subtotal),0) AS sub, " +
                        "COALESCE(SUM(b.discount),0) AS disc, COALESCE(SUM(b.total_amount),0) AS tot " +
                        "FROM bills b ");
        List<Object> sumParams = new ArrayList<>();
        List<String> sumWhere = new ArrayList<>();
        applyDateRange(sumWhere, sumParams, "b.bill_date", from, to);
        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status.trim())) {
            sumWhere.add("b.status = ?");
            sumParams.add(status.trim().toUpperCase());
        }
        if (search != null && !search.isBlank()) {
            String like = "%" + search.trim().toLowerCase() + "%";
            sumWhere.add("(LOWER(b.bill_number) LIKE ? OR EXISTS (SELECT 1 FROM patients p " +
                    "WHERE p.id = b.patient_id AND (LOWER(p.full_name) LIKE ? OR LOWER(p.patient_code) LIKE ?)) " +
                    "OR EXISTS (SELECT 1 FROM bill_items bi WHERE bi.bill_id = b.id " +
                    "          AND LOWER(COALESCE(bi.description,'')) LIKE ?))");
            sumParams.add(like);
            sumParams.add(like);
            sumParams.add(like);
            sumParams.add(like);
        }
        if (!sumWhere.isEmpty()) sumSql.append("WHERE ").append(String.join(" AND ", sumWhere)).append(" ");

        List<List<Object>> rows = reportDao.query(sql.toString(), params.toArray());
        List<List<Object>> sumRows = reportDao.query(sumSql.toString(), sumParams.toArray());
        int count = 0;
        BigDecimal sub = BigDecimal.ZERO, disc = BigDecimal.ZERO, tot = BigDecimal.ZERO;
        if (!sumRows.isEmpty()) {
            List<Object> r = sumRows.get(0);
            count = ((Number) r.get(0)).intValue();
            sub = toBigDecimal(r.get(1));
            disc = toBigDecimal(r.get(2));
            tot = toBigDecimal(r.get(3));
        }

        List<String> columns = List.of("Bill #", "Date", "Patient", "Code", "Apt", "Subtotal", "Discount", "Total", "Status");
        return new BillingReportResult(columns, rows, count, sub, disc, tot);
    }

    // ---------- helpers ----------

    private void requireLoggedIn() {
        Session session = Session.getInstance();
        if (!session.isLoggedIn() || session.getRole() == null) {
            throw new AuthorizationException("You must be logged in to access reports.");
        }
    }

    private void applyDateRange(List<String> where, List<Object> params, String col, String from, String to) {
        if (from != null && !from.isBlank()) {
            String f = from.trim();
            ensureDate(f, "From date");
            where.add(col + " >= ?");
            params.add(f);
        }
        if (to != null && !to.isBlank()) {
            String t = to.trim();
            ensureDate(t, "To date");
            where.add(col + " <= ?");
            params.add(t);
        }
    }

    private void validateDateRange(String from, String to) {
        LocalDate f = null, t = null;
        if (from != null && !from.isBlank()) {
            f = ensureDate(from.trim(), "From date");
        }
        if (to != null && !to.isBlank()) {
            t = ensureDate(to.trim(), "To date");
        }
        if (f != null && t != null && f.isAfter(t)) {
            throw new ValidationException("From date cannot be after To date.");
        }
    }

    private LocalDate ensureDate(String s, String field) {
        try {
            return LocalDate.parse(s);
        } catch (DateTimeParseException e) {
            throw new ValidationException(field + " must be a valid date in YYYY-MM-DD format.");
        }
    }

    private BigDecimal toBigDecimal(Object o) {
        if (o == null) return BigDecimal.ZERO;
        if (o instanceof BigDecimal bd) return bd;
        if (o instanceof Number n) return new BigDecimal(n.toString());
        try {
            return new BigDecimal(o.toString());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    // ---------- result DTOs ----------

    public static class ReportResult {
        private final List<String> columns;
        private final List<List<Object>> rows;

        public ReportResult(List<String> columns, List<List<Object>> rows) {
            this.columns = columns;
            this.rows = rows;
        }

        public List<String> getColumns() { return columns; }
        public List<List<Object>> getRows() { return rows; }
    }

    public static class BillingReportResult extends ReportResult {
        private final int count;
        private final BigDecimal subtotal;
        private final BigDecimal discount;
        private final BigDecimal total;

        public BillingReportResult(List<String> columns, List<List<Object>> rows,
                                   int count, BigDecimal subtotal, BigDecimal discount, BigDecimal total) {
            super(columns, rows);
            this.count = count;
            this.subtotal = subtotal;
            this.discount = discount;
            this.total = total;
        }

        public int getCount() { return count; }
        public BigDecimal getSubtotal() { return subtotal; }
        public BigDecimal getDiscount() { return discount; }
        public BigDecimal getTotal() { return total; }
    }
}
