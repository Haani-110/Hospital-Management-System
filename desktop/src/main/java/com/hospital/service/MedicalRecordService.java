package com.hospital.service;

import com.hospital.dao.AppointmentDao;
import com.hospital.dao.DoctorDao;
import com.hospital.dao.MedicalRecordDao;
import com.hospital.exception.AuthorizationException;
import com.hospital.exception.DatabaseException;
import com.hospital.exception.ValidationException;
import com.hospital.model.Appointment;
import com.hospital.model.Doctor;
import com.hospital.model.MedicalRecord;
import com.hospital.model.Role;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

/**
 * Business logic for managing medical records (Phase 4).
 *
 * <h3>Relationship to appointments</h3>
 * <ul>
 *   <li>A medical record is attached to exactly one appointment (UNIQUE FK).</li>
 *   <li>Only {@code COMPLETED} appointments may receive a record; SCHEDULED or
 *       CANCELLED appointments are rejected.</li>
 *   <li>At most one record per appointment; duplicate creation is rejected.</li>
 *   <li>Patient and doctor are derived from the appointment and cannot be
 *       selected independently.</li>
 * </ul>
 *
 * <h3>Authorization</h3>
 * <ul>
 *   <li>ADMIN: full view/search/create/edit.</li>
 *   <li>DOCTOR: view/search/create/edit. For mutations (create/edit) the
 *       appointment's doctor profile must be reliably linked to the logged-in
 *       user via {@code doctors.user_id}. If there is no such link (which is the
 *       case for seed/demo data because the demo {@code doctor} account is not
 *       attached to any doctor profile), doctor mutations are rejected with an
 *       {@link AuthorizationException} directing them to ask an admin to link
 *       their profile. This is intentionally conservative to avoid guessing a
 *       doctor identity by name or id.</li>
 *   <li>RECEPTIONIST: view/search only (no create/edit).</li>
 *   <li>Unauthenticated: all operations rejected.</li>
 * </ul>
 */
public class MedicalRecordService {

    private static final int MAX_DIAGNOSIS_LENGTH = 500;
    private static final int MAX_SYMPTOMS_LENGTH = 2000;
    private static final int MAX_EXAMINATION_LENGTH = 3000;
    private static final int MAX_TREATMENT_LENGTH = 3000;

    private final MedicalRecordDao medicalRecordDao;
    private final AppointmentDao appointmentDao;
    private final DoctorDao doctorDao;

    public MedicalRecordService(MedicalRecordDao medicalRecordDao,
                                AppointmentDao appointmentDao,
                                DoctorDao doctorDao) {
        this.medicalRecordDao = medicalRecordDao;
        this.appointmentDao = appointmentDao;
        this.doctorDao = doctorDao;
    }

    // ---------- view operations (any authenticated user) ----------

    public List<MedicalRecord> getAllRecords() {
        requireLoggedIn();
        return medicalRecordDao.findAll();
    }

    public List<MedicalRecord> searchRecords(String query) {
        requireLoggedIn();
        return medicalRecordDao.search(query);
    }

    public MedicalRecord getRecord(int id) {
        requireLoggedIn();
        return medicalRecordDao.findById(id)
                .orElseThrow(() -> new ValidationException("Medical record not found (id=" + id + ")"));
    }

    public MedicalRecord getRecordForAppointment(int appointmentId) {
        requireLoggedIn();
        return medicalRecordDao.findByAppointment(appointmentId).orElse(null);
    }

    /**
     * Return COMPLETED appointments that do not already have a medical record,
     * for use in the create-record appointment chooser.
     */
    public List<Appointment> getCompletedAppointmentsForCreate() {
        requireLoggedIn();
        return appointmentDao.search(null, Appointment.STATUS_COMPLETED, null, 0).stream()
                .filter(a -> !medicalRecordDao.existsByAppointment(a.getId()))
                .toList();
    }

    /** Return every COMPLETED appointment (useful for general lookup). */
    public List<Appointment> getCompletedAppointments() {
        requireLoggedIn();
        return appointmentDao.search(null, Appointment.STATUS_COMPLETED, null, 0);
    }

    // ---------- mutations ----------

    public MedicalRecord createRecord(Integer appointmentId, String diagnosis, String symptoms,
                                      String examination, String treatmentNotes, String recordDate) {
        requireCanCreateOrEdit(false);

        Appointment appt = validateAppointmentForCreate(appointmentId);
        String d = validateDiagnosis(diagnosis);
        String s = trimToNull(symptoms);
        String e = trimToNull(examination);
        String t = trimToNull(treatmentNotes);
        LocalDate rdate = validateRecordDate(recordDate);
        validateLengths(s, e, t);
        verifyDoctorOwnsAppointmentIfDoctorRole(appt);

        try {
            int id = medicalRecordDao.create(appt.getId(), appt.getPatientId(), appt.getDoctorId(),
                    d, s, e, t, rdate.toString());
            return medicalRecordDao.findById(id).orElseThrow(
                    () -> new DatabaseException("Medical record was created but could not be loaded back."));
        } catch (DatabaseException ex) {
            // Surface duplicate-appointment UNIQUE violations as a ValidationException.
            if (ex.getMessage() != null && (ex.getMessage().contains("UNIQUE")
                    || ex.getMessage().contains("appointment_id"))) {
                throw new ValidationException("A medical record already exists for this appointment.");
            }
            throw ex;
        }
    }

    public MedicalRecord updateRecord(int id, String diagnosis, String symptoms,
                                      String examination, String treatmentNotes, String recordDate) {
        requireCanCreateOrEdit(true);

        MedicalRecord existing = medicalRecordDao.findById(id)
                .orElseThrow(() -> new ValidationException("Medical record not found (id=" + id + ")"));

        // Load the appointment to enforce doctor-ownership if the logged-in role is DOCTOR.
        Appointment appt = appointmentDao.findById(existing.getAppointmentId())
                .orElseThrow(() -> new ValidationException("The appointment attached to this record no longer exists."));

        String d = validateDiagnosis(diagnosis);
        String s = trimToNull(symptoms);
        String e = trimToNull(examination);
        String t = trimToNull(treatmentNotes);
        LocalDate rdate = validateRecordDate(recordDate);
        validateLengths(s, e, t);
        verifyDoctorOwnsAppointmentIfDoctorRole(appt);

        medicalRecordDao.update(id, d, s, e, t, rdate.toString());
        return medicalRecordDao.findById(id).orElseThrow(
                () -> new DatabaseException("Medical record was updated but could not be loaded back."));
    }

    // ---------- authorization ----------

    private void requireLoggedIn() {
        Session session = Session.getInstance();
        if (!session.isLoggedIn() || session.getRole() == null) {
            throw new AuthorizationException("You must be logged in to access medical records.");
        }
    }

    private void requireCanCreateOrEdit(boolean isEdit) {
        requireLoggedIn();
        Role role = Session.getInstance().getRole();
        if (role == Role.ADMIN) return;
        if (role == Role.DOCTOR) return; // per-record ownership checked separately
        if (role == Role.RECEPTIONIST) {
            throw new AuthorizationException(
                    isEdit ? "Receptionists cannot edit medical records."
                           : "Receptionists cannot create medical records.");
        }
        throw new AuthorizationException("You are not allowed to modify medical records.");
    }

    /**
     * When the acting user is a DOCTOR, ensure the appointment's doctor profile
     * is linked to the logged-in user id via {@code doctors.user_id}. If no
     * reliable link exists, deny the mutation. This avoids inventing a fragile
     * mapping.
     */
    private void verifyDoctorOwnsAppointmentIfDoctorRole(Appointment appt) {
        Session session = Session.getInstance();
        if (session.getRole() != Role.DOCTOR) return;
        var user = session.getCurrentUser();
        if (user == null) throw new AuthorizationException("No logged-in user.");
        Optional<Doctor> linked = doctorDao.findByUserId(user.getId());
        if (linked.isEmpty()) {
            throw new AuthorizationException(
                    "Your user account is not linked to a doctor profile, so you cannot create or edit medical records. "
                            + "Please ask an administrator to link your account to your doctor profile.");
        }
        if (linked.get().getId() != appt.getDoctorId()) {
            throw new AuthorizationException(
                    "You can only create or edit medical records for your own appointments.");
        }
    }

    // ---------- validation ----------

    private Appointment validateAppointmentForCreate(Integer appointmentId) {
        if (appointmentId == null || appointmentId <= 0) {
            throw new ValidationException("Appointment is required.");
        }
        Appointment appt = appointmentDao.findById(appointmentId)
                .orElseThrow(() -> new ValidationException("Selected appointment does not exist."));
        if (!Appointment.STATUS_COMPLETED.equals(appt.getStatus())) {
            throw new ValidationException(
                    "Medical records can only be created for COMPLETED appointments. "
                            + "This appointment is " + appt.getStatus() + ".");
        }
        if (medicalRecordDao.existsByAppointment(appt.getId())) {
            throw new ValidationException("A medical record already exists for this appointment.");
        }
        return appt;
    }

    private String validateDiagnosis(String diagnosis) {
        if (diagnosis == null) throw new ValidationException("Diagnosis is required.");
        String t = diagnosis.trim();
        if (t.isEmpty()) throw new ValidationException("Diagnosis is required.");
        if (t.length() > MAX_DIAGNOSIS_LENGTH) {
            throw new ValidationException("Diagnosis is too long (maximum " + MAX_DIAGNOSIS_LENGTH + " characters).");
        }
        return t;
    }

    private LocalDate validateRecordDate(String recordDate) {
        if (recordDate == null) throw new ValidationException("Record date is required.");
        String t = recordDate.trim();
        if (t.isEmpty()) throw new ValidationException("Record date is required.");
        try {
            return LocalDate.parse(t);
        } catch (DateTimeParseException e) {
            throw new ValidationException("Record date must be a valid date in YYYY-MM-DD format.");
        }
    }

    private void validateLengths(String symptoms, String examination, String treatment) {
        if (symptoms != null && symptoms.length() > MAX_SYMPTOMS_LENGTH) {
            throw new ValidationException("Symptoms are too long (maximum " + MAX_SYMPTOMS_LENGTH + " characters).");
        }
        if (examination != null && examination.length() > MAX_EXAMINATION_LENGTH) {
            throw new ValidationException("Examination is too long (maximum " + MAX_EXAMINATION_LENGTH + " characters).");
        }
        if (treatment != null && treatment.length() > MAX_TREATMENT_LENGTH) {
            throw new ValidationException("Treatment notes are too long (maximum " + MAX_TREATMENT_LENGTH + " characters).");
        }
    }

    private String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
