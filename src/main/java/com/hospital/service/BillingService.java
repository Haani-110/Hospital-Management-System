package com.hospital.service;

import com.hospital.config.DatabaseConnection;
import com.hospital.dao.AppointmentDao;
import com.hospital.dao.BillDao;
import com.hospital.dao.BillDaoImpl;
import com.hospital.dao.BillItemDao;
import com.hospital.dao.BillItemDaoImpl;
import com.hospital.dao.PatientDao;
import com.hospital.exception.AuthorizationException;
import com.hospital.exception.DatabaseException;
import com.hospital.exception.ValidationException;
import com.hospital.model.Appointment;
import com.hospital.model.Bill;
import com.hospital.model.BillItem;
import com.hospital.model.Patient;
import com.hospital.model.Role;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Business logic for managing bills (Phase 6).
 *
 * <h3>Money handling</h3>
 * All monetary values are {@link BigDecimal} normalized to 2 decimal places using
 * {@link RoundingMode#HALF_UP}. Item amounts and totals are always calculated
 * by the service — client-supplied totals/amounts are never trusted.
 *
 * <h3>Authorization</h3>
 * <ul>
 *   <li>ADMIN: full view/search/create/edit/status/cancel.</li>
 *   <li>RECEPTIONIST: view/search/create/edit contents, mark PARTIALLY_PAID;
 *       cannot mark PAID or CANCEL.</li>
 *   <li>DOCTOR: view/search/details only.</li>
 *   <li>Unauthenticated: all operations rejected.</li>
 * </ul>
 * CANCELLED bills are immutable.
 */
public class BillingService {

    private static final int MAX_DESCRIPTION = 250;
    private static final int MAX_NOTES = 2000;
    private static final int MAX_QUANTITY = 9999;
    private static final BigDecimal MAX_UNIT_PRICE = new BigDecimal("99999999.99");
    private static final int MONEY_SCALE = 2;

    private final BillDao billDao;
    private final BillItemDao itemDao;
    private final BillDaoImpl billDaoTx;
    private final BillItemDaoImpl itemDaoTx;
    private final PatientDao patientDao;
    private final AppointmentDao appointmentDao;
    private final DatabaseConnection dbConnection;

    public BillingService(BillDao billDao, BillItemDao itemDao,
                          PatientDao patientDao, AppointmentDao appointmentDao,
                          DatabaseConnection dbConnection) {
        this.billDao = billDao;
        this.itemDao = itemDao;
        this.billDaoTx = (BillDaoImpl) billDao;
        this.itemDaoTx = (BillItemDaoImpl) itemDao;
        this.patientDao = patientDao;
        this.appointmentDao = appointmentDao;
        this.dbConnection = dbConnection;
    }

    // ---------- view operations ----------

    public List<Bill> getAllBills() {
        requireLoggedIn();
        return billDao.findAll();
    }

    public List<Bill> searchBills(String query, String status) {
        requireLoggedIn();
        return billDao.search(query, normalizeStatus(status));
    }

    public Bill getBill(int id) {
        requireLoggedIn();
        return billDao.findById(id)
                .orElseThrow(() -> new ValidationException("Bill not found (id=" + id + ")"));
    }

    public Bill getBillByNumber(String billNumber) {
        requireLoggedIn();
        if (billNumber == null || billNumber.isBlank()) {
            throw new ValidationException("Bill number is required.");
        }
        return billDao.findByBillNumber(billNumber.trim())
                .orElseThrow(() -> new ValidationException("Bill not found (" + billNumber.trim() + ")"));
    }

    public List<BillItem> getItems(int billId) {
        requireLoggedIn();
        getBill(billId); // existence + auth
        return itemDao.findByBillId(billId);
    }

    // ---------- mutations ----------

    public Bill createBill(Integer patientId, Integer appointmentId, String billDate,
                           String discountStr, String notes, List<BillItemInput> items) {
        requireCanCreate();
        Patient patient = validatePatient(patientId);
        Appointment appt = validateAppointmentForBill(appointmentId, patient.getId());
        LocalDate date = validateDate(billDate);
        List<ValidatedItem> validated = validateItems(items);
        BigDecimal discount = parseMoney(discountStr, "Discount");
        BigDecimal subtotal = computeSubtotal(validated);
        if (discount.signum() < 0) {
            throw new ValidationException("Discount cannot be negative.");
        }
        if (discount.compareTo(subtotal) > 0) {
            throw new ValidationException("Discount cannot exceed the subtotal.");
        }
        BigDecimal total = normalize(subtotal.subtract(discount));
        if (total.signum() < 0) {
            throw new ValidationException("Total cannot be negative.");
        }
        String cleanedNotes = validateNotes(notes);

        try (Connection conn = dbConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Insert with a placeholder bill number, then update with deterministic BILL-%06d.
                int id = billDaoTx.create(conn, "PENDING", patient.getId(),
                        appt == null ? null : appt.getId(),
                        date.toString(), Bill.STATUS_UNPAID, cleanedNotes,
                        subtotal, discount, total);
                String billNumber = String.format("BILL-%06d", id);
                billDaoTx.updateBillNumber(conn, id, billNumber);
                for (ValidatedItem vi : validated) {
                    itemDaoTx.create(conn, id, vi.description, vi.quantity, vi.unitPrice, vi.amount);
                }
                conn.commit();
                return billDao.findById(id).orElseThrow(
                        () -> new DatabaseException("Bill was created but could not be loaded back."));
            } catch (SQLException | DatabaseException e) {
                safeRollback(conn);
                if (e instanceof DatabaseException de) {
                    String msg = de.getMessage() == null ? "" : de.getMessage();
                    if (msg.contains("UNIQUE") || msg.contains("bill_number")) {
                        throw new ValidationException("A bill with this number already exists.");
                    }
                    throw de;
                }
                throw new DatabaseException("Error creating bill", e);
            } finally {
                safeSetAutoCommitTrue(conn);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error opening connection for bill creation", e);
        }
    }

    public Bill updateBill(int id, Integer appointmentId, String billDate,
                           String discountStr, String notes, List<BillItemInput> items) {
        requireCanEdit();
        Bill existing = billDao.findById(id)
                .orElseThrow(() -> new ValidationException("Bill not found (id=" + id + ")"));
        if (Bill.STATUS_CANCELLED.equals(existing.getStatus())) {
            throw new ValidationException("Cancelled bills cannot be edited.");
        }
        // Patient cannot change; derive from existing bill.
        Patient patient = patientDao.findById(existing.getPatientId())
                .orElseThrow(() -> new ValidationException("The patient attached to this bill no longer exists."));
        Appointment appt = validateAppointmentForBill(appointmentId, patient.getId());
        LocalDate date = validateDate(billDate);
        List<ValidatedItem> validated = validateItems(items);
        BigDecimal discount = parseMoney(discountStr, "Discount");
        BigDecimal subtotal = computeSubtotal(validated);
        if (discount.signum() < 0) {
            throw new ValidationException("Discount cannot be negative.");
        }
        if (discount.compareTo(subtotal) > 0) {
            throw new ValidationException("Discount cannot exceed the subtotal.");
        }
        BigDecimal total = normalize(subtotal.subtract(discount));
        if (total.signum() < 0) {
            throw new ValidationException("Total cannot be negative.");
        }
        String cleanedNotes = validateNotes(notes);

        try (Connection conn = dbConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                billDaoTx.update(conn, id, appt == null ? null : appt.getId(),
                        date.toString(), cleanedNotes, subtotal, discount, total);
                itemDaoTx.deleteAllByBillId(conn, id);
                for (ValidatedItem vi : validated) {
                    itemDaoTx.create(conn, id, vi.description, vi.quantity, vi.unitPrice, vi.amount);
                }
                conn.commit();
                return billDao.findById(id).orElseThrow(
                        () -> new DatabaseException("Bill was updated but could not be loaded back."));
            } catch (SQLException | DatabaseException e) {
                safeRollback(conn);
                if (e instanceof DatabaseException de) throw de;
                throw new DatabaseException("Error updating bill", e);
            } finally {
                safeSetAutoCommitTrue(conn);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error opening connection for bill update", e);
        }
    }

    public Bill markStatus(int id, String targetStatus) {
        String target = normalizeStatus(targetStatus);
        Bill existing = billDao.findById(id)
                .orElseThrow(() -> new ValidationException("Bill not found (id=" + id + ")"));
        requireCanChangeStatus(existing.getStatus(), target);
        try (Connection conn = dbConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                billDaoTx.updateStatus(conn, id, target);
                conn.commit();
            } catch (SQLException | DatabaseException e) {
                safeRollback(conn);
                if (e instanceof DatabaseException de) throw de;
                throw new DatabaseException("Error updating bill status", e);
            } finally {
                safeSetAutoCommitTrue(conn);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error opening connection for status change", e);
        }
        return billDao.findById(id).orElseThrow(
                () -> new DatabaseException("Bill status was updated but could not be loaded back."));
    }

    // ---------- authorization ----------

    private void requireLoggedIn() {
        Session session = Session.getInstance();
        if (!session.isLoggedIn() || session.getRole() == null) {
            throw new AuthorizationException("You must be logged in to access billing.");
        }
    }

    private void requireCanCreate() {
        requireLoggedIn();
        Role role = Session.getInstance().getRole();
        if (role == Role.ADMIN || role == Role.RECEPTIONIST) return;
        throw new AuthorizationException("You are not allowed to create bills.");
    }

    private void requireCanEdit() {
        requireLoggedIn();
        Role role = Session.getInstance().getRole();
        if (role == Role.ADMIN || role == Role.RECEPTIONIST) return;
        throw new AuthorizationException("You are not allowed to edit bills.");
    }

    private void requireCanChangeStatus(String current, String target) {
        requireLoggedIn();
        Role role = Session.getInstance().getRole();
        if (Bill.STATUS_CANCELLED.equals(current)) {
            throw new ValidationException("Cancelled bills cannot have their status changed.");
        }
        boolean allowed;
        switch (target) {
            case Bill.STATUS_PARTIALLY_PAID ->
                // Admin and receptionist can record partial payment.
                    allowed = role == Role.ADMIN || role == Role.RECEPTIONIST;
            case Bill.STATUS_PAID, Bill.STATUS_CANCELLED ->
                // Only admin may finalize payment or cancel a bill.
                    allowed = role == Role.ADMIN;
            case Bill.STATUS_UNPAID ->
                allowed = false; // do not allow going back to UNPAID
            default -> allowed = false;
        }
        if (!allowed) {
            throw new AuthorizationException("You are not allowed to change bill status to " + target + ".");
        }
        // Lifecycle check.
        boolean transitionOk = switch (current) {
            case Bill.STATUS_UNPAID ->
                    target.equals(Bill.STATUS_PARTIALLY_PAID)
                            || target.equals(Bill.STATUS_PAID)
                            || target.equals(Bill.STATUS_CANCELLED);
            case Bill.STATUS_PARTIALLY_PAID ->
                    target.equals(Bill.STATUS_PAID) || target.equals(Bill.STATUS_CANCELLED);
            case Bill.STATUS_PAID ->
                    target.equals(Bill.STATUS_CANCELLED);
            default -> false;
        };
        if (!transitionOk) {
            throw new ValidationException(
                    "Cannot transition bill status from " + current + " to " + target + ".");
        }
    }

    // ---------- validation ----------

    private Patient validatePatient(Integer patientId) {
        if (patientId == null || patientId <= 0) {
            throw new ValidationException("Patient is required.");
        }
        Patient p = patientDao.findById(patientId)
                .orElseThrow(() -> new ValidationException("Selected patient does not exist."));
        if (!p.isActive()) {
            throw new ValidationException("Selected patient is inactive and cannot be billed.");
        }
        return p;
    }

    private Appointment validateAppointmentForBill(Integer appointmentId, int patientId) {
        if (appointmentId == null || appointmentId <= 0) return null;
        Appointment a = appointmentDao.findById(appointmentId)
                .orElseThrow(() -> new ValidationException("Selected appointment does not exist."));
        if (a.getPatientId() != patientId) {
            throw new ValidationException(
                    "The selected appointment belongs to a different patient.");
        }
        return a;
    }

    private LocalDate validateDate(String date) {
        if (date == null) throw new ValidationException("Bill date is required.");
        String t = date.trim();
        if (t.isEmpty()) throw new ValidationException("Bill date is required.");
        try {
            return LocalDate.parse(t);
        } catch (DateTimeParseException e) {
            throw new ValidationException("Bill date must be a valid date in YYYY-MM-DD format.");
        }
    }

    private String validateNotes(String notes) {
        if (notes == null) return null;
        String t = notes.trim();
        if (t.isEmpty()) return null;
        if (t.length() > MAX_NOTES) {
            throw new ValidationException("Notes are too long (maximum " + MAX_NOTES + " characters).");
        }
        return t;
    }

    private List<ValidatedItem> validateItems(List<BillItemInput> items) {
        if (items == null || items.isEmpty()) {
            throw new ValidationException("At least one bill item is required.");
        }
        List<ValidatedItem> out = new ArrayList<>();
        int idx = 0;
        for (BillItemInput it : items) {
            idx++;
            if (it == null) throw new ValidationException("Bill item #" + idx + " is invalid.");
            String desc = required(it.description, "Description", idx, MAX_DESCRIPTION);
            int qty = parseQuantity(it.quantity, idx);
            BigDecimal price = parseMoney(it.unitPrice, "Unit price for item #" + idx);
            if (price.signum() < 0) {
                throw new ValidationException("Unit price for item #" + idx + " cannot be negative.");
            }
            if (price.compareTo(MAX_UNIT_PRICE) > 0) {
                throw new ValidationException("Unit price for item #" + idx + " exceeds the maximum allowed.");
            }
            BigDecimal amount = normalize(price.multiply(BigDecimal.valueOf(qty)));
            out.add(new ValidatedItem(desc, qty, normalize(price), amount));
        }
        return out;
    }

    private int parseQuantity(String raw, int idx) {
        if (raw == null || raw.isBlank()) {
            throw new ValidationException("Quantity is required for bill item #" + idx + ".");
        }
        int qty;
        try {
            qty = Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            throw new ValidationException("Quantity for item #" + idx + " must be a whole number.");
        }
        if (qty < 1) {
            throw new ValidationException("Quantity for item #" + idx + " must be at least 1.");
        }
        if (qty > MAX_QUANTITY) {
            throw new ValidationException("Quantity for item #" + idx + " is too large (maximum " + MAX_QUANTITY + ").");
        }
        return qty;
    }

    private BigDecimal parseMoney(String raw, String field) {
        if (raw == null || raw.isBlank()) return BigDecimal.ZERO;
        BigDecimal v;
        try {
            v = new BigDecimal(raw.trim());
        } catch (NumberFormatException e) {
            throw new ValidationException(field + " must be a valid monetary amount.");
        }
        return normalize(v);
    }

    private BigDecimal computeSubtotal(List<ValidatedItem> items) {
        BigDecimal sum = BigDecimal.ZERO;
        for (ValidatedItem vi : items) sum = sum.add(vi.amount);
        return normalize(sum);
    }

    private static BigDecimal normalize(BigDecimal v) {
        if (v == null) return BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        return v.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private String required(String raw, String field, int idx, int max) {
        if (raw == null) throw new ValidationException(field + " is required for bill item #" + idx + ".");
        String t = raw.trim();
        if (t.isEmpty()) throw new ValidationException(field + " is required for bill item #" + idx + ".");
        if (t.length() > max) {
            throw new ValidationException(field + " for item #" + idx + " is too long (maximum " + max + " characters).");
        }
        return t;
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) return null;
        String s = status.trim().toUpperCase();
        return switch (s) {
            case Bill.STATUS_UNPAID, Bill.STATUS_PARTIALLY_PAID, Bill.STATUS_PAID, Bill.STATUS_CANCELLED -> s;
            default -> s; // let DB CHECK constraint / service lifecycle reject invalid
        };
    }

    private void safeRollback(Connection conn) {
        try { conn.rollback(); } catch (SQLException ignore) {}
    }

    private void safeSetAutoCommitTrue(Connection conn) {
        try { conn.setAutoCommit(true); } catch (SQLException ignore) {}
    }

    // ---------- DTOs ----------

    /** DTO for incoming bill item data (from UI or tests). */
    public static class BillItemInput {
        public String description;
        public String quantity;
        public String unitPrice;

        public BillItemInput() {}
        public BillItemInput(String description, String quantity, String unitPrice) {
            this.description = description;
            this.quantity = quantity;
            this.unitPrice = unitPrice;
        }
    }

    private record ValidatedItem(String description, int quantity, BigDecimal unitPrice, BigDecimal amount) {}
}
