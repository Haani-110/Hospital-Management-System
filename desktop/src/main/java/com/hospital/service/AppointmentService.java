package com.hospital.service;

import com.hospital.dao.AppointmentDao;
import com.hospital.dao.DoctorDao;
import com.hospital.dao.PatientDao;
import com.hospital.exception.AuthorizationException;
import com.hospital.exception.DatabaseException;
import com.hospital.exception.ValidationException;
import com.hospital.model.Appointment;
import com.hospital.model.Doctor;
import com.hospital.model.Patient;
import com.hospital.model.Role;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Set;

/**
 * Business logic for managing appointments (Phase 3).
 *
 * <h3>Lifecycle</h3>
 * <ul>
 *   <li>SCHEDULED → COMPLETED or SCHEDULED → CANCELLED (one-way).</li>
 *   <li>Completed and cancelled appointments cannot be edited or re-transitioned.</li>
 *   <li>Appointments are never physically deleted.</li>
 * </ul>
 *
 * <h3>Authorization</h3>
 * <ul>
 *   <li>ADMIN: full CRUD + cancel + complete.</li>
 *   <li>RECEPTIONIST: create, edit, cancel, view/search/filter (no completing).</li>
 *   <li>DOCTOR: view/search/filter only in this phase, because the application
 *       does not maintain a reliable user↔doctor profile mapping (the {@code user_id}
 *       field exists on doctors but is not populated through the UI or demo seed
 *       data, so we cannot safely determine "which doctor is logged in" without
 *       introducing an unsafe assumption). A future phase that adds a
 *       doctor-to-user linking flow can re-enable doctor-specific completion.</li>
 *   <li>Unauthenticated: all operations rejected.</li>
 * </ul>
 */
public class AppointmentService {

    private static final int MAX_REASON_LENGTH = 250;
    private static final int MAX_NOTES_LENGTH = 1000;

    private static final Set<String> ALLOWED_STATUSES = Set.of(
            Appointment.STATUS_SCHEDULED,
            Appointment.STATUS_COMPLETED,
            Appointment.STATUS_CANCELLED
    );

    private final AppointmentDao appointmentDao;
    private final PatientDao patientDao;
    private final DoctorDao doctorDao;

    public AppointmentService(AppointmentDao appointmentDao, PatientDao patientDao, DoctorDao doctorDao) {
        this.appointmentDao = appointmentDao;
        this.patientDao = patientDao;
        this.doctorDao = doctorDao;
    }

    // ---------- view operations (any authenticated user) ----------

    public List<Appointment> getAllAppointments() {
        requireLoggedIn();
        return appointmentDao.findAll();
    }

    public List<Appointment> searchAppointments(String query, String status, LocalDate dateFilter, Integer doctorId) {
        requireLoggedIn();
        String statusFilter = normalizeStatusFilter(status);
        return appointmentDao.search(query, statusFilter, dateFilter, doctorId == null ? 0 : doctorId);
    }

    public Appointment getAppointment(int id) {
        requireLoggedIn();
        return appointmentDao.findById(id)
                .orElseThrow(() -> new ValidationException("Appointment not found (id=" + id + ")"));
    }

    // ---------- mutations ----------

    public Appointment createAppointment(Integer patientId, Integer doctorId,
                                         String appointmentDate, String appointmentTime,
                                         String reason, String notes) {
        requireCanCreateOrEdit();

        int pid = validatePatient(patientId);
        int did = validateDoctor(doctorId);
        LocalDate date = validateDate(appointmentDate, true);
        LocalTime time = validateTime(appointmentTime);
        String cleanedReason = validateReason(reason);
        String cleanedNotes = validateNotes(notes);

        if (appointmentDao.hasConflict(did, date.toString(), time.toString(), 0)) {
            throw new ValidationException("This doctor already has an active appointment at "
                    + time + " on " + date + ". Please pick a different time.");
        }

        try {
            int id = appointmentDao.create(pid, did, date.toString(), time.toString(), cleanedReason, cleanedNotes);
            return appointmentDao.findById(id).orElseThrow(
                    () -> new DatabaseException("Appointment was created but could not be loaded back."));
        } catch (DatabaseException e) {
            throw e;
        }
    }

    public Appointment updateAppointment(int id, Integer patientId, Integer doctorId,
                                         String appointmentDate, String appointmentTime,
                                         String reason, String notes) {
        requireCanCreateOrEdit();

        Appointment existing = appointmentDao.findById(id)
                .orElseThrow(() -> new ValidationException("Appointment not found (id=" + id + ")"));
        if (!Appointment.STATUS_SCHEDULED.equals(existing.getStatus())) {
            throw new ValidationException("Only scheduled appointments can be edited.");
        }

        int pid = validatePatient(patientId);
        int did = validateDoctor(doctorId);
        LocalDate date = validateDate(appointmentDate, false);
        LocalTime time = validateTime(appointmentTime);
        String cleanedReason = validateReason(reason);
        String cleanedNotes = validateNotes(notes);

        if (appointmentDao.hasConflict(did, date.toString(), time.toString(), id)) {
            throw new ValidationException("This doctor already has an active appointment at "
                    + time + " on " + date + ". Please pick a different time.");
        }

        appointmentDao.update(id, pid, did, date.toString(), time.toString(), cleanedReason, cleanedNotes);
        return appointmentDao.findById(id).orElseThrow(
                () -> new DatabaseException("Appointment was updated but could not be loaded back."));
    }

    public void cancelAppointment(int id) {
        requireCanCancel();
        Appointment a = appointmentDao.findById(id)
                .orElseThrow(() -> new ValidationException("Appointment not found (id=" + id + ")"));
        if (Appointment.STATUS_CANCELLED.equals(a.getStatus())) {
            throw new ValidationException("This appointment is already cancelled.");
        }
        if (Appointment.STATUS_COMPLETED.equals(a.getStatus())) {
            throw new ValidationException("Cannot cancel a completed appointment.");
        }
        appointmentDao.updateStatus(id, Appointment.STATUS_CANCELLED);
    }

    public void completeAppointment(int id) {
        requireCanComplete();
        Appointment a = appointmentDao.findById(id)
                .orElseThrow(() -> new ValidationException("Appointment not found (id=" + id + ")"));
        if (Appointment.STATUS_COMPLETED.equals(a.getStatus())) {
            throw new ValidationException("This appointment is already completed.");
        }
        if (Appointment.STATUS_CANCELLED.equals(a.getStatus())) {
            throw new ValidationException("Cannot complete a cancelled appointment.");
        }
        appointmentDao.updateStatus(id, Appointment.STATUS_COMPLETED);
    }

    // ---------- authorization ----------

    private void requireLoggedIn() {
        Session session = Session.getInstance();
        if (!session.isLoggedIn() || session.getRole() == null) {
            throw new AuthorizationException("You must be logged in to perform this action.");
        }
    }

    private void requireCanCreateOrEdit() {
        requireLoggedIn();
        Role r = Session.getInstance().getRole();
        if (r != Role.ADMIN && r != Role.RECEPTIONIST) {
            throw new AuthorizationException("Only administrators and receptionists can schedule or edit appointments.");
        }
    }

    private void requireCanCancel() {
        requireLoggedIn();
        Role r = Session.getInstance().getRole();
        if (r != Role.ADMIN && r != Role.RECEPTIONIST) {
            throw new AuthorizationException("Only administrators and receptionists can cancel appointments.");
        }
    }

    private void requireCanComplete() {
        requireLoggedIn();
        if (Session.getInstance().getRole() != Role.ADMIN) {
            throw new AuthorizationException("Only administrators can mark appointments as completed in this phase.");
        }
    }

    // ---------- validation ----------

    private String normalizeStatusFilter(String status) {
        if (status == null) return null;
        String t = status.trim();
        if (t.isEmpty() || "All".equalsIgnoreCase(t)) return null;
        if (!ALLOWED_STATUSES.contains(t)) {
            // Be permissive in search — just ignore invalid filters rather than throwing.
            return null;
        }
        return t;
    }

    private int validatePatient(Integer patientId) {
        if (patientId == null || patientId <= 0) {
            throw new ValidationException("Patient is required.");
        }
        Patient p = patientDao.findById(patientId)
                .orElseThrow(() -> new ValidationException("Selected patient does not exist."));
        if (!p.isActive()) {
            throw new ValidationException("Cannot schedule appointments for inactive patients.");
        }
        return p.getId();
    }

    private int validateDoctor(Integer doctorId) {
        if (doctorId == null || doctorId <= 0) {
            throw new ValidationException("Doctor is required.");
        }
        Doctor d = doctorDao.findById(doctorId)
                .orElseThrow(() -> new ValidationException("Selected doctor does not exist."));
        if (!d.isActive()) {
            throw new ValidationException("Cannot schedule appointments with inactive doctors.");
        }
        return d.getId();
    }

    private LocalDate validateDate(String date, boolean requireFuture) {
        if (date == null) throw new ValidationException("Appointment date is required.");
        String t = date.trim();
        if (t.isEmpty()) throw new ValidationException("Appointment date is required.");
        LocalDate parsed;
        try {
            parsed = LocalDate.parse(t);
        } catch (DateTimeParseException e) {
            throw new ValidationException("Appointment date must be a valid date in YYYY-MM-DD format.");
        }
        if (requireFuture && parsed.isBefore(LocalDate.now())) {
            throw new ValidationException("Appointment date cannot be in the past.");
        }
        return parsed;
    }

    private LocalTime validateTime(String time) {
        if (time == null) throw new ValidationException("Appointment time is required.");
        String t = time.trim();
        if (t.isEmpty()) throw new ValidationException("Appointment time is required.");
        try {
            return LocalTime.parse(t);
        } catch (DateTimeParseException e) {
            throw new ValidationException("Appointment time must be a valid time in HH:mm format (e.g. 09:30).");
        }
    }

    private String validateReason(String reason) {
        if (reason == null) return null;
        String t = reason.trim();
        if (t.isEmpty()) return null;
        if (t.length() > MAX_REASON_LENGTH) {
            throw new ValidationException("Reason is too long (maximum " + MAX_REASON_LENGTH + " characters).");
        }
        return t;
    }

    private String validateNotes(String notes) {
        if (notes == null) return null;
        String t = notes.trim();
        if (t.isEmpty()) return null;
        if (t.length() > MAX_NOTES_LENGTH) {
            throw new ValidationException("Notes are too long (maximum " + MAX_NOTES_LENGTH + " characters).");
        }
        return t;
    }
}
