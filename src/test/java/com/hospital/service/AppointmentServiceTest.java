package com.hospital.service;

import com.hospital.config.DatabaseConnection;
import com.hospital.config.DatabaseInitializer;
import com.hospital.config.TestDatabaseFactory;
import com.hospital.dao.AppointmentDao;
import com.hospital.dao.AppointmentDaoImpl;
import com.hospital.dao.DepartmentDao;
import com.hospital.dao.DepartmentDaoImpl;
import com.hospital.dao.DoctorDao;
import com.hospital.dao.DoctorDaoImpl;
import com.hospital.dao.PatientDao;
import com.hospital.dao.PatientDaoImpl;
import com.hospital.exception.AuthorizationException;
import com.hospital.exception.ValidationException;
import com.hospital.model.Appointment;
import com.hospital.model.Role;
import com.hospital.model.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.*;

class AppointmentServiceTest {

    private DatabaseConnection db;
    private AppointmentDao appointmentDao;
    private AppointmentService service;

    private int patientId;
    private int patient2Id;
    private int inactivePatientId;
    private int doctorId;
    private int doctor2Id;
    private int inactiveDoctorId;

    @BeforeEach
    void setUp() throws IOException {
        Session.reset();
        db = TestDatabaseFactory.createTempDatabase();
        new DatabaseInitializer(db).initialize();

        PatientDao patientDao = new PatientDaoImpl(db);
        DoctorDao doctorDao = new DoctorDaoImpl(db);
        DepartmentDao deptDao = new DepartmentDaoImpl(db);
        appointmentDao = new AppointmentDaoImpl(db);

        int cardId = deptDao.create("Cardiology", null);
        int neuroId = deptDao.create("Neurology", null);

        patientId = patientDao.create("PAT-000001", "Alice", null, null, null, null, null, null, null, null);
        patient2Id = patientDao.create("PAT-000002", "Bob", null, null, null, null, null, null, null, null);
        inactivePatientId = patientDao.create("PAT-000003", "Carol", null, null, null, null, null, null, null, null);
        patientDao.updateActiveStatus(inactivePatientId, false);

        doctorId = doctorDao.create(null, cardId, "Dr. Khan", "Cardiologist", null, null, 0);
        doctor2Id = doctorDao.create(null, neuroId, "Dr. Lee", "Neurologist", null, null, 0);
        inactiveDoctorId = doctorDao.create(null, cardId, "Dr. Inactive", "Cardiologist", null, null, 0);
        doctorDao.updateActiveStatus(inactiveDoctorId, false);

        service = new AppointmentService(appointmentDao, patientDao, doctorDao);
    }

    @AfterEach
    void tearDown() throws IOException {
        Session.reset();
        TestDatabaseFactory.deleteDatabaseFile(db);
    }

    private void loginAs(Role role) {
        User u = new User(999, role.name().toLowerCase(), "hash", role, true, LocalDateTime.now());
        Session.getInstance().setCurrentUser(u);
    }

    private String futureDate(int daysAhead) {
        return LocalDate.now().plusDays(daysAhead).toString();
    }

    // ---------- Authorization ----------

    @Test
    void adminCanCreateEditCancelComplete() {
        loginAs(Role.ADMIN);
        String tomorrow = futureDate(1);
        Appointment a = service.createAppointment(patientId, doctorId, tomorrow, "09:30", "Checkup", "notes");
        assertNotNull(a);
        assertEquals("SCHEDULED", a.getStatus());
        assertEquals(patientId, a.getPatientId());
        assertEquals(doctorId, a.getDoctorId());

        String dayAfter = futureDate(2);
        Appointment updated = service.updateAppointment(a.getId(), patient2Id, doctor2Id, dayAfter, "10:00", "Follow-up", "new notes");
        assertEquals(patient2Id, updated.getPatientId());
        assertEquals(doctor2Id, updated.getDoctorId());
        assertEquals("Follow-up", updated.getReason());

        service.cancelAppointment(a.getId());
        assertEquals("CANCELLED", appointmentDao.findById(a.getId()).orElseThrow().getStatus());

        // Create another to test complete
        Appointment b = service.createAppointment(patientId, doctorId, futureDate(3), "11:00", "X", null);
        service.completeAppointment(b.getId());
        assertEquals("COMPLETED", appointmentDao.findById(b.getId()).orElseThrow().getStatus());
    }

    @Test
    void receptionistCanCreateEditCancelButNotComplete() {
        loginAs(Role.RECEPTIONIST);
        Appointment a = service.createAppointment(patientId, doctorId, futureDate(1), "09:30", null, null);
        assertNotNull(a);
        service.updateAppointment(a.getId(), patient2Id, doctorId, futureDate(2), "10:00", null, null);
        service.cancelAppointment(a.getId());
        assertEquals("CANCELLED", appointmentDao.findById(a.getId()).orElseThrow().getStatus());

        Appointment b = service.createAppointment(patientId, doctorId, futureDate(3), "11:00", null, null);
        assertThrows(AuthorizationException.class, () -> service.completeAppointment(b.getId()));
    }

    @Test
    void doctorIsReadOnly() {
        loginAs(Role.ADMIN);
        Appointment a = service.createAppointment(patientId, doctorId, futureDate(1), "09:30", null, null);

        loginAs(Role.DOCTOR);
        assertFalse(service.getAllAppointments().isEmpty());
        assertNotNull(service.getAppointment(a.getId()));
        assertEquals(1, service.searchAppointments(null, null, null, null).size());

        assertThrows(AuthorizationException.class,
                () -> service.createAppointment(patientId, doctorId, futureDate(2), "10:00", null, null));
        assertThrows(AuthorizationException.class,
                () -> service.updateAppointment(a.getId(), patientId, doctorId, futureDate(2), "10:00", null, null));
        assertThrows(AuthorizationException.class, () -> service.cancelAppointment(a.getId()));
        assertThrows(AuthorizationException.class, () -> service.completeAppointment(a.getId()));
    }

    @Test
    void unauthenticatedRejected() {
        assertThrows(AuthorizationException.class, () -> service.getAllAppointments());
        assertThrows(AuthorizationException.class, () -> service.getAppointment(1));
        assertThrows(AuthorizationException.class,
                () -> service.createAppointment(patientId, doctorId, futureDate(1), "09:30", null, null));
        assertThrows(AuthorizationException.class, () -> service.cancelAppointment(1));
        assertThrows(AuthorizationException.class, () -> service.completeAppointment(1));
    }

    // ---------- Validation ----------

    @Test
    void patientRequiredAndMustExistAndBeActive() {
        loginAs(Role.ADMIN);
        assertThrows(ValidationException.class,
                () -> service.createAppointment(null, doctorId, futureDate(1), "09:30", null, null));
        assertThrows(ValidationException.class,
                () -> service.createAppointment(0, doctorId, futureDate(1), "09:30", null, null));
        assertThrows(ValidationException.class,
                () -> service.createAppointment(9999, doctorId, futureDate(1), "09:30", null, null));
        assertThrows(ValidationException.class,
                () -> service.createAppointment(inactivePatientId, doctorId, futureDate(1), "09:30", null, null));
    }

    @Test
    void doctorRequiredAndMustExistAndBeActive() {
        loginAs(Role.ADMIN);
        assertThrows(ValidationException.class,
                () -> service.createAppointment(patientId, null, futureDate(1), "09:30", null, null));
        assertThrows(ValidationException.class,
                () -> service.createAppointment(patientId, 0, futureDate(1), "09:30", null, null));
        assertThrows(ValidationException.class,
                () -> service.createAppointment(patientId, 9999, futureDate(1), "09:30", null, null));
        assertThrows(ValidationException.class,
                () -> service.createAppointment(patientId, inactiveDoctorId, futureDate(1), "09:30", null, null));
    }

    @Test
    void dateRequiredValidAndNotPast() {
        loginAs(Role.ADMIN);
        assertThrows(ValidationException.class,
                () -> service.createAppointment(patientId, doctorId, null, "09:30", null, null));
        assertThrows(ValidationException.class,
                () -> service.createAppointment(patientId, doctorId, "", "09:30", null, null));
        assertThrows(ValidationException.class,
                () -> service.createAppointment(patientId, doctorId, "not-a-date", "09:30", null, null));
        String past = LocalDate.now().minusDays(1).toString();
        assertThrows(ValidationException.class,
                () -> service.createAppointment(patientId, doctorId, past, "09:30", null, null));
        // today is acceptable (no "in the past" check for today)
        String today = LocalDate.now().toString();
        Appointment a = service.createAppointment(patientId, doctorId, today, "09:30", null, null);
        assertNotNull(a);
    }

    @Test
    void timeRequiredAndValid() {
        loginAs(Role.ADMIN);
        String tomorrow = futureDate(1);
        assertThrows(ValidationException.class,
                () -> service.createAppointment(patientId, doctorId, tomorrow, null, null, null));
        assertThrows(ValidationException.class,
                () -> service.createAppointment(patientId, doctorId, tomorrow, "", null, null));
        assertThrows(ValidationException.class,
                () -> service.createAppointment(patientId, doctorId, tomorrow, "9:99", null, null));
        assertThrows(ValidationException.class,
                () -> service.createAppointment(patientId, doctorId, tomorrow, "abc", null, null));
        Appointment a = service.createAppointment(patientId, doctorId, tomorrow, "14:30", null, null);
        assertNotNull(a);
    }

    @Test
    void reasonAndNotesLengthLimits() {
        loginAs(Role.ADMIN);
        String tomorrow = futureDate(1);
        assertThrows(ValidationException.class,
                () -> service.createAppointment(patientId, doctorId, tomorrow, "09:30", "r".repeat(251), null));
        assertThrows(ValidationException.class,
                () -> service.createAppointment(patientId, doctorId, tomorrow, "09:30", null, "n".repeat(1001)));
        Appointment a = service.createAppointment(patientId, doctorId, tomorrow, "09:30", "r".repeat(250), "n".repeat(1000));
        assertEquals(250, a.getReason().length());
        assertEquals(1000, a.getNotes().length());
    }

    @Test
    void reasonAndNotesOptionalTrimmed() {
        loginAs(Role.ADMIN);
        Appointment a = service.createAppointment(patientId, doctorId, futureDate(1), "09:30", "  ", "  ");
        assertNull(a.getReason());
        assertNull(a.getNotes());
    }

    // ---------- Conflict detection ----------

    @Test
    void duplicateDoctorDateTimeRejected() {
        loginAs(Role.ADMIN);
        String tomorrow = futureDate(1);
        service.createAppointment(patientId, doctorId, tomorrow, "09:30", null, null);
        assertThrows(ValidationException.class,
                () -> service.createAppointment(patient2Id, doctorId, tomorrow, "09:30", null, null));
        // Different doctor at same time is OK
        Appointment ok = service.createAppointment(patientId, doctor2Id, tomorrow, "09:30", null, null);
        assertNotNull(ok);
    }

    @Test
    void cancelledAppointmentFreesSlot() {
        loginAs(Role.ADMIN);
        String tomorrow = futureDate(1);
        Appointment a = service.createAppointment(patientId, doctorId, tomorrow, "09:30", null, null);
        service.cancelAppointment(a.getId());
        // Same slot can now be booked
        Appointment b = service.createAppointment(patient2Id, doctorId, tomorrow, "09:30", null, null);
        assertNotNull(b);
        assertNotEquals(a.getId(), b.getId());
    }

    @Test
    void editExcludesSelfFromConflict() {
        loginAs(Role.ADMIN);
        String tomorrow = futureDate(1);
        Appointment a = service.createAppointment(patientId, doctorId, tomorrow, "09:30", null, null);
        // Editing same appointment with same patient/doctor/date/time should NOT conflict with itself.
        Appointment updated = service.updateAppointment(a.getId(), patientId, doctorId, tomorrow, "09:30", "updated", null);
        assertEquals("updated", updated.getReason());
    }

    // ---------- Lifecycle ----------

    @Test
    void completedCannotBeCancelled() {
        loginAs(Role.ADMIN);
        Appointment a = service.createAppointment(patientId, doctorId, futureDate(1), "09:30", null, null);
        service.completeAppointment(a.getId());
        assertThrows(ValidationException.class, () -> service.cancelAppointment(a.getId()));
    }

    @Test
    void cancelledCannotBeCompleted() {
        loginAs(Role.ADMIN);
        Appointment a = service.createAppointment(patientId, doctorId, futureDate(1), "09:30", null, null);
        service.cancelAppointment(a.getId());
        assertThrows(ValidationException.class, () -> service.completeAppointment(a.getId()));
    }

    @Test
    void doubleCompleteRejected() {
        loginAs(Role.ADMIN);
        Appointment a = service.createAppointment(patientId, doctorId, futureDate(1), "09:30", null, null);
        service.completeAppointment(a.getId());
        assertThrows(ValidationException.class, () -> service.completeAppointment(a.getId()));
    }

    @Test
    void doubleCancelRejected() {
        loginAs(Role.ADMIN);
        Appointment a = service.createAppointment(patientId, doctorId, futureDate(1), "09:30", null, null);
        service.cancelAppointment(a.getId());
        assertThrows(ValidationException.class, () -> service.cancelAppointment(a.getId()));
    }

    @Test
    void nonScheduledCannotBeEdited() {
        loginAs(Role.ADMIN);
        Appointment a = service.createAppointment(patientId, doctorId, futureDate(1), "09:30", null, null);
        service.completeAppointment(a.getId());
        assertThrows(ValidationException.class,
                () -> service.updateAppointment(a.getId(), patientId, doctorId, futureDate(2), "10:00", null, null));
    }

    @Test
    void unknownAppointmentRejected() {
        loginAs(Role.ADMIN);
        assertThrows(ValidationException.class, () -> service.getAppointment(9999));
        assertThrows(ValidationException.class,
                () -> service.updateAppointment(9999, patientId, doctorId, futureDate(1), "10:00", null, null));
        assertThrows(ValidationException.class, () -> service.cancelAppointment(9999));
        assertThrows(ValidationException.class, () -> service.completeAppointment(9999));
    }

    @Test
    void canEditCompletedOrCancelledDatePastDateAllowedForEdit() {
        // Updates don't enforce future-date (only create does) because a receptionist may need
        // to correct a typo on an already-scheduled record even if the date has passed.
        loginAs(Role.ADMIN);
        Appointment a = service.createAppointment(patientId, doctorId, futureDate(1), "09:30", null, null);
        String past = LocalDate.now().minusDays(5).format(DateTimeFormatter.ISO_LOCAL_DATE);
        Appointment updated = service.updateAppointment(a.getId(), patientId, doctorId, past, "10:00", null, null);
        assertEquals(LocalDate.now().minusDays(5), updated.getAppointmentDate());
    }
}
