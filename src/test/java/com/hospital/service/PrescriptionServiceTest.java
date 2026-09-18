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
import com.hospital.dao.PrescriptionDao;
import com.hospital.dao.PrescriptionDaoImpl;
import com.hospital.dao.PrescriptionItemDao;
import com.hospital.dao.PrescriptionItemDaoImpl;
import com.hospital.dao.UserDao;
import com.hospital.dao.UserDaoImpl;
import com.hospital.exception.AuthorizationException;
import com.hospital.exception.ValidationException;
import com.hospital.model.Appointment;
import com.hospital.model.Prescription;
import com.hospital.model.PrescriptionItem;
import com.hospital.model.Role;
import com.hospital.model.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PrescriptionServiceTest {

    private DatabaseConnection db;
    private PrescriptionService service;
    private PrescriptionDao prescriptionDao;
    private PrescriptionItemDao itemDao;
    private MedicalRecordDao medicalRecordDao;

    private int patientId;
    private int doctorId;
    private int linkedDoctorId;
    private int recordId;        // Belongs to unlinked doctor (doctorId)
    private int ownRecordId;     // Belongs to linked doctor (linkedDoctorId)

    private User adminUser;
    private User receptionistUser;
    private User unlinkedDoctorUser;
    private User linkedDoctorUser;

    @BeforeEach
    void setUp() throws IOException {
        Session.reset();
        db = TestDatabaseFactory.createTempDatabase();
        new DatabaseInitializer(db).initialize();

        PatientDao patientDao = new PatientDaoImpl(db);
        DoctorDao doctorDao = new DoctorDaoImpl(db);
        DepartmentDao deptDao = new DepartmentDaoImpl(db);
        UserDao userDao = new UserDaoImpl(db);
        AppointmentDao appointmentDao = new AppointmentDaoImpl(db);
        medicalRecordDao = new MedicalRecordDaoImpl(db);
        prescriptionDao = new PrescriptionDaoImpl(db);
        itemDao = new PrescriptionItemDaoImpl(db);

        service = new PrescriptionService(prescriptionDao, itemDao, medicalRecordDao, doctorDao, db);

        int deptId = deptDao.create("Cardiology", null);
        patientId = patientDao.create("PAT-000001", "Alice", null, null, null, null, null, null, null, null);
        doctorId = doctorDao.create(null, deptId, "Dr. Khan", "Cardiologist", null, null, 0);

        int linkedUserId = userDao.create("dr_linked", "h", Role.DOCTOR);
        linkedDoctorId = doctorDao.create(linkedUserId, deptId, "Dr. Linked", "Cardiologist", null, null, 0);

        int apptId = appointmentDao.create(patientId, doctorId, "2030-01-15", "09:30", null, null);
        appointmentDao.updateStatus(apptId, Appointment.STATUS_COMPLETED);
        recordId = medicalRecordDao.create(apptId, patientId, doctorId, "D1", null, null, null, "2030-01-15");

        int apptOwnId = appointmentDao.create(patientId, linkedDoctorId, "2030-02-01", "09:00", null, null);
        appointmentDao.updateStatus(apptOwnId, Appointment.STATUS_COMPLETED);
        ownRecordId = medicalRecordDao.create(apptOwnId, patientId, linkedDoctorId, "D2", null, null, null, "2030-02-01");

        adminUser = new User(901, "admin", "h", Role.ADMIN, true, LocalDateTime.now());
        receptionistUser = new User(902, "rec", "h", Role.RECEPTIONIST, true, LocalDateTime.now());
        unlinkedDoctorUser = new User(903, "dr", "h", Role.DOCTOR, true, LocalDateTime.now());
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

    private List<PrescriptionService.PrescriptionItemInput> oneItem(String name, String dose, String freq, String dur) {
        return List.of(new PrescriptionService.PrescriptionItemInput(name, dose, freq, dur, null));
    }

    private String futureDate() {
        return LocalDate.now().plusDays(5).toString();
    }

    // ---------- auth ----------

    @Test
    void unauthenticatedDenied() {
        Session.getInstance().clear();
        assertThrows(AuthorizationException.class, () -> service.getAllPrescriptions());
        assertThrows(AuthorizationException.class,
                () -> service.createPrescription(recordId, futureDate(), null,
                        oneItem("Paracetamol", "500mg", "BD", "5d")));
    }

    @Test
    void receptionistCanViewButNotCreateOrEdit() {
        login(adminUser);
        Prescription p = service.createPrescription(recordId, futureDate(), null,
                oneItem("Paracetamol", "500mg", "BD", "5d"));
        login(receptionistUser);
        assertEquals(1, service.getAllPrescriptions().size());
        assertThrows(AuthorizationException.class,
                () -> service.createPrescription(ownRecordId, futureDate(), null,
                        oneItem("Paracetamol", "500mg", "BD", "5d")));
        assertThrows(AuthorizationException.class,
                () -> service.updatePrescription(p.getId(), futureDate(), "x",
                        oneItem("Paracetamol", "500mg", "BD", "5d")));
    }

    @Test
    void doctorWithoutLinkedProfileCannotCreate() {
        login(unlinkedDoctorUser);
        assertThrows(AuthorizationException.class,
                () -> service.createPrescription(recordId, futureDate(), null,
                        oneItem("Paracetamol", "500mg", "BD", "5d")));
    }

    @Test
    void doctorCannotEditOthersPrescriptions() {
        login(adminUser);
        Prescription p = service.createPrescription(recordId, futureDate(), null,
                oneItem("Paracetamol", "500mg", "BD", "5d"));
        login(linkedDoctorUser);
        assertThrows(AuthorizationException.class,
                () -> service.updatePrescription(p.getId(), futureDate(), "x",
                        oneItem("Paracetamol", "500mg", "BD", "5d")));
    }

    @Test
    void doctorCanCreateAndEditOwnPrescriptions() {
        login(linkedDoctorUser);
        Prescription p = service.createPrescription(ownRecordId, futureDate(), "notes",
                oneItem("Paracetamol", "500mg", "BD", "5d"));
        assertNotNull(p);
        Prescription updated = service.updatePrescription(p.getId(), futureDate(), "notes2",
                oneItem("Ibuprofen", "200mg", "SOS", "3d"));
        assertEquals("notes2", updated.getNotes());
        assertEquals(1, itemDao.findByPrescriptionId(p.getId()).size());
        assertEquals("Ibuprofen", itemDao.findByPrescriptionId(p.getId()).get(0).getMedicineName());
    }

    @Test
    void adminCanCreateAndEdit() {
        login(adminUser);
        Prescription p = service.createPrescription(recordId, futureDate(), null,
                oneItem("Paracetamol", "500mg", "BD", "5d"));
        assertNotNull(p);
        Prescription updated = service.updatePrescription(p.getId(), futureDate(), "ok",
                List.of(
                        new PrescriptionService.PrescriptionItemInput("A", "1", "BD", "1d", null),
                        new PrescriptionService.PrescriptionItemInput("B", "2", "TDS", "2d", "with food")));
        assertEquals("ok", updated.getNotes());
        assertEquals(2, itemDao.findByPrescriptionId(p.getId()).size());
    }

    // ---------- validation ----------

    @Test
    void duplicateMedicalRecordRejected() {
        login(adminUser);
        service.createPrescription(recordId, futureDate(), null, oneItem("A", "1", "BD", "1d"));
        assertThrows(ValidationException.class,
                () -> service.createPrescription(recordId, futureDate(), null, oneItem("A", "1", "BD", "1d")));
    }

    @Test
    void invalidMedicalRecordRejected() {
        login(adminUser);
        assertThrows(ValidationException.class,
                () -> service.createPrescription(null, futureDate(), null, oneItem("A", "1", "BD", "1d")));
        assertThrows(ValidationException.class,
                () -> service.createPrescription(0, futureDate(), null, oneItem("A", "1", "BD", "1d")));
        assertThrows(ValidationException.class,
                () -> service.createPrescription(9999, futureDate(), null, oneItem("A", "1", "BD", "1d")));
    }

    @Test
    void dateValidation() {
        login(adminUser);
        assertThrows(ValidationException.class,
                () -> service.createPrescription(recordId, null, null, oneItem("A", "1", "BD", "1d")));
        assertThrows(ValidationException.class,
                () -> service.createPrescription(recordId, "", null, oneItem("A", "1", "BD", "1d")));
        assertThrows(ValidationException.class,
                () -> service.createPrescription(recordId, "not-a-date", null, oneItem("A", "1", "BD", "1d")));
        // Accepts past dates (no restriction per spec)
        Prescription p = service.createPrescription(ownRecordIdAsAdmin(), "2020-01-01", null,
                oneItem("A", "1", "BD", "1d"));
        assertEquals(LocalDate.of(2020, 1, 1), p.getPrescriptionDate());
    }

    private int ownRecordIdAsAdmin() {
        // Already logged in as admin; returns recordId for "own" record (doctorId=linkedDoctorId).
        return ownRecordId;
    }

    @Test
    void notesLengthValidation() {
        login(adminUser);
        assertThrows(ValidationException.class,
                () -> service.createPrescription(recordId, futureDate(), "n".repeat(2001),
                        oneItem("A", "1", "BD", "1d")));
    }

    @Test
    void atLeastOneItemRequired() {
        login(adminUser);
        assertThrows(ValidationException.class,
                () -> service.createPrescription(recordId, futureDate(), null, List.of()));
        assertThrows(ValidationException.class,
                () -> service.createPrescription(recordId, futureDate(), null, null));
    }

    @Test
    void itemFieldValidation() {
        login(adminUser);
        // Blank medicine
        assertThrows(ValidationException.class,
                () -> service.createPrescription(recordId, futureDate(), null,
                        List.of(new PrescriptionService.PrescriptionItemInput("  ", "1", "BD", "1d", null))));
        // Overlong
        assertThrows(ValidationException.class,
                () -> service.createPrescription(recordId, futureDate(), null,
                        List.of(new PrescriptionService.PrescriptionItemInput("m".repeat(201), "1", "BD", "1d", null))));
        assertThrows(ValidationException.class,
                () -> service.createPrescription(recordId, futureDate(), null,
                        List.of(new PrescriptionService.PrescriptionItemInput("A", "d".repeat(101), "BD", "1d", null))));
        assertThrows(ValidationException.class,
                () -> service.createPrescription(recordId, futureDate(), null,
                        List.of(new PrescriptionService.PrescriptionItemInput("A", "1", "f".repeat(101), "1d", null))));
        assertThrows(ValidationException.class,
                () -> service.createPrescription(recordId, futureDate(), null,
                        List.of(new PrescriptionService.PrescriptionItemInput("A", "1", "BD", "d".repeat(101), null))));
        assertThrows(ValidationException.class,
                () -> service.createPrescription(recordId, futureDate(), null,
                        List.of(new PrescriptionService.PrescriptionItemInput("A", "1", "BD", "1d", "i".repeat(501)))));
    }

    @Test
    void unknownPrescriptionRejectedOnUpdate() {
        login(adminUser);
        assertThrows(ValidationException.class,
                () -> service.updatePrescription(9999, futureDate(), null, oneItem("A", "1", "BD", "1d")));
    }

    @Test
    void transactionRollsBackOnItemFailure() {
        login(adminUser);
        // First item valid, second item invalid -> whole transaction must roll back.
        var bad = List.of(
                new PrescriptionService.PrescriptionItemInput("Paracetamol", "500mg", "BD", "5d", null),
                new PrescriptionService.PrescriptionItemInput(null, "1", "BD", "1d", null)
        );
        assertThrows(ValidationException.class,
                () -> service.createPrescription(recordId, futureDate(), null, bad));
        assertEquals(0, prescriptionDao.count());
        assertEquals(0, itemDao.count());
    }

    @Test
    void getEligibleMedicalRecords_excludesRecordsWithPrescription() {
        login(adminUser);
        service.createPrescription(recordId, futureDate(), null, oneItem("A", "1", "BD", "1d"));
        var eligible = service.getEligibleMedicalRecordsForCreate();
        assertTrue(eligible.stream().noneMatch(r -> r.getId() == recordId));
        assertTrue(eligible.stream().anyMatch(r -> r.getId() == ownRecordId));
    }

    @Test
    void getItems_returnsInsertedItems() {
        login(adminUser);
        Prescription p = service.createPrescription(recordId, futureDate(), null,
                List.of(
                        new PrescriptionService.PrescriptionItemInput("A", "1", "BD", "1d", null),
                        new PrescriptionService.PrescriptionItemInput("B", "2", "TDS", "2d", "after meals")));
        List<PrescriptionItem> items = service.getItems(p.getId());
        assertEquals(2, items.size());
        assertEquals("A", items.get(0).getMedicineName());
        assertEquals("after meals", items.get(1).getInstructions());
    }

    @Test
    void blankNotesNormalizedToNull() {
        login(adminUser);
        Prescription p = service.createPrescription(recordId, futureDate(), "   ",
                oneItem("A", "1", "BD", "1d"));
        assertNull(p.getNotes());
    }

    @Test
    void blankInstructionsNormalizedToNull() {
        login(adminUser);
        Prescription p = service.createPrescription(recordId, futureDate(), null,
                List.of(new PrescriptionService.PrescriptionItemInput("A", "1", "BD", "1d", "   ")));
        assertNull(itemDao.findByPrescriptionId(p.getId()).get(0).getInstructions());
    }
}
