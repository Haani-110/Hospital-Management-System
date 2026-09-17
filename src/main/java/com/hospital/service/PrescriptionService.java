package com.hospital.service;

import com.hospital.dao.DoctorDao;
import com.hospital.dao.MedicalRecordDao;
import com.hospital.dao.PrescriptionDao;
import com.hospital.dao.PrescriptionDaoImpl;
import com.hospital.dao.PrescriptionItemDao;
import com.hospital.dao.PrescriptionItemDaoImpl;
import com.hospital.exception.AuthorizationException;
import com.hospital.exception.DatabaseException;
import com.hospital.exception.ValidationException;
import com.hospital.model.Doctor;
import com.hospital.model.MedicalRecord;
import com.hospital.model.Prescription;
import com.hospital.model.PrescriptionItem;
import com.hospital.model.Role;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.hospital.config.DatabaseConnection;

/**
 * Business logic for managing prescriptions (Phase 5).
 *
 * <h3>Relationship</h3>
 * <ul>
 *   <li>A prescription belongs to exactly one medical record (UNIQUE on
 *       {@code medical_record_id}).</li>
 *   <li>Patient and doctor are derived from the medical record and cannot be
 *       changed after creation.</li>
 *   <li>A prescription must contain at least one medicine item.</li>
 * </ul>
 *
 * <h3>Authorization</h3>
 * <ul>
 *   <li>ADMIN: full view/search/create/edit.</li>
 *   <li>DOCTOR: view/search/create/edit, but only for prescriptions whose
 *       medical record's doctor matches the logged-in doctor profile linked via
 *       {@code doctors.user_id}.</li>
 *   <li>RECEPTIONIST: view/search only.</li>
 *   <li>Unauthenticated: all operations rejected.</li>
 * </ul>
 */
public class PrescriptionService {

    private static final int MAX_MEDICINE_NAME = 200;
    private static final int MAX_DOSAGE = 100;
    private static final int MAX_FREQUENCY = 100;
    private static final int MAX_DURATION = 100;
    private static final int MAX_INSTRUCTIONS = 500;
    private static final int MAX_NOTES = 2000;

    private final PrescriptionDao prescriptionDao;
    private final PrescriptionItemDao itemDao;
    private final PrescriptionDaoImpl prescriptionDaoTx;
    private final PrescriptionItemDaoImpl itemDaoTx;
    private final MedicalRecordDao medicalRecordDao;
    private final DoctorDao doctorDao;
    private final DatabaseConnection dbConnection;

    public PrescriptionService(PrescriptionDao prescriptionDao,
                               PrescriptionItemDao itemDao,
                               MedicalRecordDao medicalRecordDao,
                               DoctorDao doctorDao,
                               DatabaseConnection dbConnection) {
        this.prescriptionDao = prescriptionDao;
        this.itemDao = itemDao;
        this.prescriptionDaoTx = (PrescriptionDaoImpl) prescriptionDao;
        this.itemDaoTx = (PrescriptionItemDaoImpl) itemDao;
        this.medicalRecordDao = medicalRecordDao;
        this.doctorDao = doctorDao;
        this.dbConnection = dbConnection;
    }

    // ---------- view operations ----------

    public List<Prescription> getAllPrescriptions() {
        requireLoggedIn();
        return prescriptionDao.findAll();
    }

    public List<Prescription> searchPrescriptions(String query) {
        requireLoggedIn();
        return prescriptionDao.search(query);
    }

    public Prescription getPrescription(int id) {
        requireLoggedIn();
        return prescriptionDao.findById(id)
                .orElseThrow(() -> new ValidationException("Prescription not found (id=" + id + ")"));
    }

    public List<PrescriptionItem> getItems(int prescriptionId) {
        requireLoggedIn();
        // Also verify prescription exists (throws if not)
        getPrescription(prescriptionId);
        return itemDao.findByPrescriptionId(prescriptionId);
    }

    /**
     * Return medical records that are eligible for a new prescription (i.e. do
     * not already have one) for the create-prescription chooser.
     */
    public List<MedicalRecord> getEligibleMedicalRecordsForCreate() {
        requireLoggedIn();
        return medicalRecordDao.findAll().stream()
                .filter(r -> !prescriptionDao.existsByMedicalRecord(r.getId()))
                .toList();
    }

    // ---------- mutations ----------

    public Prescription createPrescription(Integer medicalRecordId, String prescriptionDate,
                                           String notes, List<PrescriptionItemInput> items) {
        requireCanCreateOrEdit(false);
        MedicalRecord record = validateMedicalRecordForCreate(medicalRecordId);
        LocalDate date = validateDate(prescriptionDate);
        String cleanedNotes = validateNotes(notes);
        List<ValidatedItem> validated = validateItems(items, false);
        verifyDoctorOwnsRecordIfDoctorRole(record);

        try (Connection conn = dbConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Double-check uniqueness inside the transaction to avoid race conditions.
                if (prescriptionDaoTx.existsByMedicalRecord(conn, record.getId())) {
                    throw new ValidationException("A prescription already exists for this medical record.");
                }
                int pid = prescriptionDaoTx.create(conn, record.getId(), record.getPatientId(),
                        record.getDoctorId(), date.toString(), cleanedNotes);
                for (ValidatedItem vi : validated) {
                    itemDaoTx.create(conn, pid, vi.medicineName, vi.dosage,
                            vi.frequency, vi.duration, vi.instructions);
                }
                conn.commit();
                return prescriptionDaoTx.findById(conn, pid).orElseThrow(
                        () -> new DatabaseException("Prescription was created but could not be loaded back."));
            } catch (SQLException | DatabaseException e) {
                safeRollback(conn);
                if (e instanceof DatabaseException de) {
                    String msg = de.getMessage() == null ? "" : de.getMessage();
                    if (msg.contains("UNIQUE") || msg.contains("medical_record_id")) {
                        throw new ValidationException("A prescription already exists for this medical record.");
                    }
                    throw de;
                }
                throw new DatabaseException("Error creating prescription", e);
            } finally {
                safeSetAutoCommitTrue(conn);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error opening connection for prescription creation", e);
        }
    }

    public Prescription updatePrescription(int id, String prescriptionDate, String notes,
                                           List<PrescriptionItemInput> items) {
        requireCanCreateOrEdit(true);
        Prescription existing = prescriptionDao.findById(id)
                .orElseThrow(() -> new ValidationException("Prescription not found (id=" + id + ")"));
        MedicalRecord record = medicalRecordDao.findById(existing.getMedicalRecordId())
                .orElseThrow(() -> new ValidationException(
                        "The medical record attached to this prescription no longer exists."));
        verifyDoctorOwnsRecordIfDoctorRole(record);

        LocalDate date = validateDate(prescriptionDate);
        String cleanedNotes = validateNotes(notes);
        List<ValidatedItem> validated = validateItems(items, true);

        try (Connection conn = dbConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                prescriptionDaoTx.update(conn, id, date.toString(), cleanedNotes);
                // Replace items: delete all existing, then insert the new set.
                // Because we validated items already has ≥1, this always leaves ≥1.
                itemDaoTx.deleteByPrescriptionId(conn, id);
                for (ValidatedItem vi : validated) {
                    itemDaoTx.create(conn, id, vi.medicineName, vi.dosage,
                            vi.frequency, vi.duration, vi.instructions);
                }
                conn.commit();
                return prescriptionDaoTx.findById(conn, id).orElseThrow(
                        () -> new DatabaseException("Prescription was updated but could not be loaded back."));
            } catch (SQLException | DatabaseException e) {
                safeRollback(conn);
                if (e instanceof DatabaseException de) throw de;
                throw new DatabaseException("Error updating prescription", e);
            } finally {
                safeSetAutoCommitTrue(conn);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error opening connection for prescription update", e);
        }
    }

    // ---------- authorization ----------

    private void requireLoggedIn() {
        Session session = Session.getInstance();
        if (!session.isLoggedIn() || session.getRole() == null) {
            throw new AuthorizationException("You must be logged in to access prescriptions.");
        }
    }

    private void requireCanCreateOrEdit(boolean isEdit) {
        requireLoggedIn();
        Role role = Session.getInstance().getRole();
        if (role == Role.ADMIN) return;
        if (role == Role.DOCTOR) return; // ownership checked per-record
        if (role == Role.RECEPTIONIST) {
            throw new AuthorizationException(
                    isEdit ? "Receptionists cannot edit prescriptions."
                           : "Receptionists cannot create prescriptions.");
        }
        throw new AuthorizationException("You are not allowed to modify prescriptions.");
    }

    private void verifyDoctorOwnsRecordIfDoctorRole(MedicalRecord record) {
        Session session = Session.getInstance();
        if (session.getRole() != Role.DOCTOR) return;
        var user = session.getCurrentUser();
        if (user == null) throw new AuthorizationException("No logged-in user.");
        Optional<Doctor> linked = doctorDao.findByUserId(user.getId());
        if (linked.isEmpty()) {
            throw new AuthorizationException(
                    "Your user account is not linked to a doctor profile, so you cannot create or edit prescriptions. "
                            + "Please ask an administrator to link your account to your doctor profile.");
        }
        if (linked.get().getId() != record.getDoctorId()) {
            throw new AuthorizationException(
                    "You can only create or edit prescriptions for your own patients.");
        }
    }

    // ---------- validation ----------

    private MedicalRecord validateMedicalRecordForCreate(Integer medicalRecordId) {
        if (medicalRecordId == null || medicalRecordId <= 0) {
            throw new ValidationException("Medical record is required.");
        }
        MedicalRecord record = medicalRecordDao.findById(medicalRecordId)
                .orElseThrow(() -> new ValidationException("Selected medical record does not exist."));
        if (prescriptionDao.existsByMedicalRecord(record.getId())) {
            throw new ValidationException("A prescription already exists for this medical record.");
        }
        return record;
    }

    private LocalDate validateDate(String date) {
        if (date == null) throw new ValidationException("Prescription date is required.");
        String t = date.trim();
        if (t.isEmpty()) throw new ValidationException("Prescription date is required.");
        try {
            return LocalDate.parse(t);
        } catch (DateTimeParseException e) {
            throw new ValidationException("Prescription date must be a valid date in YYYY-MM-DD format.");
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

    private List<ValidatedItem> validateItems(List<PrescriptionItemInput> items, boolean isEdit) {
        if (items == null || items.isEmpty()) {
            throw new ValidationException("At least one medicine item is required.");
        }
        List<ValidatedItem> out = new ArrayList<>();
        int idx = 0;
        for (PrescriptionItemInput it : items) {
            idx++;
            if (it == null) {
                throw new ValidationException("Medicine item #" + idx + " is invalid.");
            }
            String name = required(it.medicineName, "Medicine name", idx, MAX_MEDICINE_NAME);
            String dosage = required(it.dosage, "Dosage", idx, MAX_DOSAGE);
            String freq = required(it.frequency, "Frequency", idx, MAX_FREQUENCY);
            String dur = required(it.duration, "Duration", idx, MAX_DURATION);
            String instr = trimToNull(it.instructions);
            if (instr != null && instr.length() > MAX_INSTRUCTIONS) {
                throw new ValidationException("Instructions for item #" + idx + " are too long (maximum "
                        + MAX_INSTRUCTIONS + " characters).");
            }
            out.add(new ValidatedItem(name, dosage, freq, dur, instr));
        }
        if (out.isEmpty()) {
            throw new ValidationException("At least one medicine item is required.");
        }
        return out;
    }

    private String required(String raw, String field, int idx, int max) {
        if (raw == null) {
            throw new ValidationException(field + " is required for medicine item #" + idx + ".");
        }
        String t = raw.trim();
        if (t.isEmpty()) {
            throw new ValidationException(field + " is required for medicine item #" + idx + ".");
        }
        if (t.length() > max) {
            throw new ValidationException(field + " for item #" + idx + " is too long (maximum " + max + " characters).");
        }
        return t;
    }

    private String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private void safeRollback(Connection conn) {
        try { conn.rollback(); } catch (SQLException ignore) {}
    }

    private void safeSetAutoCommitTrue(Connection conn) {
        try { conn.setAutoCommit(true); } catch (SQLException ignore) {}
    }

    // ---------- input / output helpers ----------

    /** DTO for incoming prescription item data (from UI or tests). */
    public static class PrescriptionItemInput {
        public String medicineName;
        public String dosage;
        public String frequency;
        public String duration;
        public String instructions;

        public PrescriptionItemInput() {}
        public PrescriptionItemInput(String medicineName, String dosage, String frequency,
                                     String duration, String instructions) {
            this.medicineName = medicineName;
            this.dosage = dosage;
            this.frequency = frequency;
            this.duration = duration;
            this.instructions = instructions;
        }
    }

    private record ValidatedItem(String medicineName, String dosage, String frequency,
                                 String duration, String instructions) {}
}
