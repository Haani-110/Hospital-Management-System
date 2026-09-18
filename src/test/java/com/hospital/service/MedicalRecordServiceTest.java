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
import com.hospital.dao.MedicalRecordDao;
import com.hospital.dao.MedicalRecordDaoImpl;
import com.hospital.dao.PatientDao;
import com.hospital.dao.PatientDaoImpl;
import com.hospital.dao.UserDao;
import com.hospital.dao.UserDaoImpl;
import com.hospital.exception.AuthorizationException;
import com.hospital.exception.ValidationException;
import com.hospital.model.Appointment;
import com.hospital.model.MedicalRecord;
import com.hospital.model.Role;
import com.hospital.model.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class MedicalRecordServiceTest {

    private DatabaseConnection db;
    private MedicalRecordService service;
    private AppointmentDao appointmentDao;

    private int patientId;
    private int doctorId;
    private int completedApptId;
    private int scheduledApptId;
    private int cancelledApptId;

    private User adminUser;
    private User receptionistUser;
    private User unlinkedDoctorUser;
    private User linkedDoctorUser;
    private int linkedDoctorId;

    @BeforeEach
    void setUp() throws IOException {
        Session.reset();
        db = TestDatabaseFactory.createTempDatabase();
        new DatabaseInitializer(db).initialize();

        PatientDao patientDao = new PatientDaoImpl(db);
        DoctorDao doctorDao = new DoctorDaoImpl(db);
        DepartmentDao deptDao = new DepartmentDaoImpl(db);
        UserDao userDao = new UserDaoImpl(db);
        appointmentDao = new AppointmentDaoImpl(db);
        MedicalRecordDao medicalRecordDao = new MedicalRecordDaoImpl(db);

        service = new MedicalRecordService(medicalRecordDao, appointmentDao, doctorDao);

        int cardId = deptDao.create("Cardiology", null);
        patientId = patientDao.create("PAT-000001", "Alice", null, null, null, null, null, null, null, null);
        doctorId = doctorDao.create(null, cardId, "Dr. Khan", "Cardiologist", null, null, 0);

        // The "linked" doctor must have a real user row because doctors.user_id is a foreign key.
        int linkedUserId = userDao.create("dr_linked", "h", Role.DOCTOR);
        linkedDoctorId = doctorDao.create(linkedUserId, cardId, "Dr. Linked", "Cardiologist", null, null, 0);

        completedApptId = appointmentDao.create(patientId, doctorId, "2030-01-15", "09:30", null, null);
        appointmentDao.updateStatus(completedApptId, Appointment.STATUS_COMPLETED);

        scheduledApptId = appointmentDao.create(patientId, doctorId, "2030-01-20", "10:00", null, null);
        cancelledApptId = appointmentDao.create(patientId, doctorId, "2030-01-21", "11:00", null, null);
        appointmentDao.updateStatus(cancelledApptId, Appointment.STATUS_CANCELLED);

        // A second COMPLETED appointment owned by the linked doctor, for DOCTOR-role ownership tests.
        int completedApptLinkedId = appointmentDao.create(patientId, linkedDoctorId, "2030-02-01", "09:00", null, null);
        appointmentDao.updateStatus(completedApptLinkedId, Appointment.STATUS_COMPLETED);

        adminUser = new User(901, "admin", "h", Role.ADMIN, true, LocalDateTime.now());
        receptionistUser = new User(902, "rec", "h", Role.RECEPTIONIST, true, LocalDateTime.now());
        unlinkedDoctorUser = new User(903, "dr", "h", Role.DOCTOR, true, LocalDateTime.now());
        // The linked DOCTOR session user must carry the same id as the persisted user row
        // so that DoctorDao.findByUserId matches.
        linkedDoctorUser = new User(linkedUserId, "drl", "h", Role.DOCTOR, true, LocalDateTime.now());
    }

    @AfterEach
    void tearDown() throws IOException {
        Session.reset();
        TestDatabaseFactory.deleteDatabaseFile(db);
    }

    private void login(User u) {
        Session.getInstance().setCurrentUser(u);
    }

    private String futureDate() { return LocalDate.now().plusDays(5).toString(); }

    @Test
    void adminCanCreate() {
        login(adminUser);
        MedicalRecord r = service.createRecord(completedApptId, "Diagnosis", "sym", "exam", "treat", futureDate());
        assertNotNull(r);
        assertEquals("Diagnosis", r.getDiagnosis());
        assertEquals(patientId, r.getPatientId());
        assertEquals(doctorId, r.getDoctorId());
        assertEquals(completedApptId, r.getAppointmentId());
    }

    @Test
    void doctorCanCreate_whenLinkedAndOwnAppointment() {
        login(linkedDoctorUser);
        // Find the completed appointment that belongs to linkedDoctorId
        var list = service.getCompletedAppointments();
        int ownId = list.stream()
                .filter(a -> a.getDoctorId() == linkedDoctorId)
                .findFirst().orElseThrow().getId();
        MedicalRecord r = service.createRecord(ownId, "D", null, null, null, futureDate());
        assertNotNull(r);
    }

    @Test
    void doctorCannotCreate_whenNoLinkedProfile() {
        login(unlinkedDoctorUser);
        assertThrows(AuthorizationException.class,
                () -> service.createRecord(completedApptId, "D", null, null, null, futureDate()));
    }

    @Test
    void doctorCannotCreate_forOtherDoctorsAppointment() {
        login(linkedDoctorUser);
        // completedApptId is for doctorId, not linkedDoctorId
        assertThrows(AuthorizationException.class,
                () -> service.createRecord(completedApptId, "D", null, null, null, futureDate()));
    }

    @Test
    void receptionistCannotCreate() {
        login(receptionistUser);
        assertThrows(AuthorizationException.class,
                () -> service.createRecord(completedApptId, "D", null, null, null, futureDate()));
    }

    @Test
    void adminCanEdit() {
        login(adminUser);
        MedicalRecord r = service.createRecord(completedApptId, "D", null, null, null, futureDate());
        MedicalRecord updated = service.updateRecord(r.getId(), "D2", "s", "e", "t", futureDate());
        assertEquals("D2", updated.getDiagnosis());
        assertEquals("s", updated.getSymptoms());
    }

    @Test
    void linkedDoctorCanEditOwnRecord() {
        // Create as admin first, then edit as linked doctor (for the appointment linkedDoctor owns).
        login(adminUser);
        var list = service.getCompletedAppointments();
        int ownId = list.stream()
                .filter(a -> a.getDoctorId() == linkedDoctorId)
                .findFirst().orElseThrow().getId();
        MedicalRecord r = service.createRecord(ownId, "D", null, null, null, futureDate());

        login(linkedDoctorUser);
        MedicalRecord updated = service.updateRecord(r.getId(), "DX", null, null, null, futureDate());
        assertEquals("DX", updated.getDiagnosis());
    }

    @Test
    void receptionistCannotEdit() {
        login(adminUser);
        MedicalRecord r = service.createRecord(completedApptId, "D", null, null, null, futureDate());
        login(receptionistUser);
        assertThrows(AuthorizationException.class,
                () -> service.updateRecord(r.getId(), "D2", null, null, null, futureDate()));
    }

    @Test
    void unauthenticatedDenied() {
        Session.getInstance().clear();
        assertThrows(AuthorizationException.class, () -> service.getAllRecords());
        assertThrows(AuthorizationException.class,
                () -> service.createRecord(completedApptId, "D", null, null, null, futureDate()));
    }

    @Test
    void scheduledAppointmentCannotReceiveRecord() {
        login(adminUser);
        assertThrows(ValidationException.class,
                () -> service.createRecord(scheduledApptId, "D", null, null, null, futureDate()));
    }

    @Test
    void cancelledAppointmentCannotReceiveRecord() {
        login(adminUser);
        assertThrows(ValidationException.class,
                () -> service.createRecord(cancelledApptId, "D", null, null, null, futureDate()));
    }

    @Test
    void completedAppointmentCanReceiveRecord() {
        login(adminUser);
        assertNotNull(service.createRecord(completedApptId, "D", null, null, null, futureDate()));
    }

    @Test
    void duplicateRecordRejected() {
        login(adminUser);
        service.createRecord(completedApptId, "D", null, null, null, futureDate());
        assertThrows(ValidationException.class,
                () -> service.createRecord(completedApptId, "D2", null, null, null, futureDate()));
    }

    @Test
    void diagnosisRequired() {
        login(adminUser);
        assertThrows(ValidationException.class,
                () -> service.createRecord(completedApptId, null, null, null, null, futureDate()));
        assertThrows(ValidationException.class,
                () -> service.createRecord(completedApptId, "  ", null, null, null, futureDate()));
    }

    @Test
    void recordDateRequired() {
        login(adminUser);
        assertThrows(ValidationException.class,
                () -> service.createRecord(completedApptId, "D", null, null, null, null));
        assertThrows(ValidationException.class,
                () -> service.createRecord(completedApptId, "D", null, null, null, ""));
        assertThrows(ValidationException.class,
                () -> service.createRecord(completedApptId, "D", null, null, null, "not-a-date"));
    }

    @Test
    void invalidAppointmentRejected() {
        login(adminUser);
        assertThrows(ValidationException.class,
                () -> service.createRecord(null, "D", null, null, null, futureDate()));
        assertThrows(ValidationException.class,
                () -> service.createRecord(0, "D", null, null, null, futureDate()));
        assertThrows(ValidationException.class,
                () -> service.createRecord(9999, "D", null, null, null, futureDate()));
    }

    @Test
    void lengthValidation() {
        login(adminUser);
        assertThrows(ValidationException.class,
                () -> service.createRecord(completedApptId, "d".repeat(501), null, null, null, futureDate()));
        assertThrows(ValidationException.class,
                () -> service.createRecord(completedApptId, "D", "s".repeat(2001), null, null, futureDate()));
        assertThrows(ValidationException.class,
                () -> service.createRecord(completedApptId, "D", null, "e".repeat(3001), null, futureDate()));
        assertThrows(ValidationException.class,
                () -> service.createRecord(completedApptId, "D", null, null, "t".repeat(3001), futureDate()));
    }

    @Test
    void unknownRecordRejected() {
        login(adminUser);
        assertThrows(ValidationException.class,
                () -> service.updateRecord(9999, "D", null, null, null, futureDate()));
    }
}
