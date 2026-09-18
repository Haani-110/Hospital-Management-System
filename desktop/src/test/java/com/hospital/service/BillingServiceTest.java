package com.hospital.service;

import com.hospital.config.DatabaseConnection;
import com.hospital.config.DatabaseInitializer;
import com.hospital.config.TestDatabaseFactory;
import com.hospital.dao.AppointmentDao;
import com.hospital.dao.AppointmentDaoImpl;
import com.hospital.dao.BillDao;
import com.hospital.dao.BillDaoImpl;
import com.hospital.dao.BillItemDao;
import com.hospital.dao.BillItemDaoImpl;
import com.hospital.dao.DepartmentDao;
import com.hospital.dao.DepartmentDaoImpl;
import com.hospital.dao.DoctorDao;
import com.hospital.dao.DoctorDaoImpl;
import com.hospital.dao.PatientDao;
import com.hospital.dao.PatientDaoImpl;
import com.hospital.dao.UserDao;
import com.hospital.dao.UserDaoImpl;
import com.hospital.exception.AuthorizationException;
import com.hospital.exception.ValidationException;
import com.hospital.model.Appointment;
import com.hospital.model.Bill;
import com.hospital.model.BillItem;
import com.hospital.model.Role;
import com.hospital.model.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BillingServiceTest {

    private DatabaseConnection db;
    private BillingService service;
    private BillDao billDao;

    private int patientId;
    private int patient2Id;
    private int doctorId;
    private int appt1Id;  // belongs to patientId
    private int appt2Id;  // belongs to patient2Id

    private User adminUser;
    private User receptionistUser;
    private User doctorUser;
    private User unlinkedDoctorUser;
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
        AppointmentDao appointmentDao = new AppointmentDaoImpl(db);
        billDao = new BillDaoImpl(db);
        BillItemDao itemDao = new BillItemDaoImpl(db);

        service = new BillingService(billDao, itemDao, patientDao, appointmentDao, db);

        int deptId = deptDao.create("Cardiology", null);
        patientId = patientDao.create("PAT-000001", "Alice", null, null, null, null, null, null, null, null);
        patient2Id = patientDao.create("PAT-000002", "Bob", null, null, null, null, null, null, null, null);
        doctorId = doctorDao.create(null, deptId, "Dr. Khan", "Cardiologist", null, null, 0);
        int linkedUserId = userDao.create("dr_linked", "h", Role.DOCTOR);
        linkedDoctorId = doctorDao.create(linkedUserId, deptId, "Dr. Linked", "Cardiologist", null, null, 0);
        appt1Id = appointmentDao.create(patientId, doctorId, "2030-03-01", "09:30", null, null);
        appt2Id = appointmentDao.create(patient2Id, doctorId, "2030-03-02", "10:00", null, null);

        adminUser = new User(901, "admin", "h", Role.ADMIN, true, LocalDateTime.now());
        receptionistUser = new User(902, "rec", "h", Role.RECEPTIONIST, true, LocalDateTime.now());
        unlinkedDoctorUser = new User(903, "dr", "h", Role.DOCTOR, true, LocalDateTime.now());
        doctorUser = new User(linkedUserId, "drl", "h", Role.DOCTOR, true, LocalDateTime.now());
    }

    @AfterEach
    void tearDown() throws IOException {
        Session.reset();
        TestDatabaseFactory.deleteDatabaseFile(db);
    }

    private void login(User u) { Session.getInstance().setCurrentUser(u); }

    private List<BillingService.BillItemInput> oneItem(String desc, String qty, String price) {
        return List.of(new BillingService.BillItemInput(desc, qty, price));
    }

    private String futureDate() { return LocalDate.now().plusDays(5).toString(); }

    // ---------- auth ----------

    @Test
    void unauthenticatedDenied() {
        Session.getInstance().clear();
        assertThrows(AuthorizationException.class, () -> service.getAllBills());
        assertThrows(AuthorizationException.class, () -> service.createBill(patientId, null, futureDate(),
                "0", null, oneItem("Consultation", "1", "100")));
    }

    @Test
    void adminCanCreateEditCancelMarkPaid() {
        login(adminUser);
        Bill b = service.createBill(patientId, null, futureDate(), "10", null,
                List.of(new BillingService.BillItemInput("Consultation", "1", "100.00")));
        assertNotNull(b);
        assertEquals("BILL-000001", b.getBillNumber());
        assertEquals(Bill.STATUS_UNPAID, b.getStatus());
        assertEquals(0, new BigDecimal("90.00").compareTo(b.getTotalAmount()));

        Bill edited = service.updateBill(b.getId(), null, futureDate(), "5", null,
                List.of(new BillingService.BillItemInput("Consultation", "2", "100.00")));
        assertEquals(0, new BigDecimal("195.00").compareTo(edited.getTotalAmount()));

        Bill paid = service.markStatus(b.getId(), Bill.STATUS_PAID);
        assertEquals(Bill.STATUS_PAID, paid.getStatus());
    }

    @Test
    void receptionistCanCreateAndEditButCannotMarkPaidOrCancel() {
        login(receptionistUser);
        Bill b = service.createBill(patientId, null, futureDate(), "0", null,
                oneItem("Consultation", "1", "100"));
        assertNotNull(b);

        Bill partial = service.markStatus(b.getId(), Bill.STATUS_PARTIALLY_PAID);
        assertEquals(Bill.STATUS_PARTIALLY_PAID, partial.getStatus());

        assertThrows(AuthorizationException.class,
                () -> service.markStatus(b.getId(), Bill.STATUS_PAID));
        assertThrows(AuthorizationException.class,
                () -> service.markStatus(b.getId(), Bill.STATUS_CANCELLED));

        // Can edit
        Bill edited = service.updateBill(b.getId(), null, futureDate(), "0", null,
                oneItem("Consultation", "2", "50.00"));
        assertEquals(0, new BigDecimal("100.00").compareTo(edited.getTotalAmount()));
    }

    @Test
    void doctorCanViewButNotCreateOrEdit() {
        login(adminUser);
        Bill b = service.createBill(patientId, null, futureDate(), "0", null,
                oneItem("Consultation", "1", "100"));
        login(doctorUser);
        assertEquals(1, service.getAllBills().size());
        assertNotNull(service.getBill(b.getId()));
        assertThrows(AuthorizationException.class,
                () -> service.createBill(patientId, null, futureDate(), "0", null,
                        oneItem("A", "1", "1")));
        assertThrows(AuthorizationException.class,
                () -> service.updateBill(b.getId(), null, futureDate(), "0", null,
                        oneItem("A", "1", "1")));
        assertThrows(AuthorizationException.class,
                () -> service.markStatus(b.getId(), Bill.STATUS_PAID));
    }

    // ---------- validation ----------

    @Test
    void requiresPatient() {
        login(adminUser);
        assertThrows(ValidationException.class,
                () -> service.createBill(null, null, futureDate(), "0", null,
                        oneItem("A", "1", "1")));
        assertThrows(ValidationException.class,
                () -> service.createBill(9999, null, futureDate(), "0", null,
                        oneItem("A", "1", "1")));
    }

    @Test
    void requiresDate() {
        login(adminUser);
        assertThrows(ValidationException.class,
                () -> service.createBill(patientId, null, null, "0", null,
                        oneItem("A", "1", "1")));
        assertThrows(ValidationException.class,
                () -> service.createBill(patientId, null, "not-a-date", "0", null,
                        oneItem("A", "1", "1")));
    }

    @Test
    void requiresAtLeastOneItem() {
        login(adminUser);
        assertThrows(ValidationException.class,
                () -> service.createBill(patientId, null, futureDate(), "0", null, List.of()));
    }

    @Test
    void itemDescriptionRequired() {
        login(adminUser);
        assertThrows(ValidationException.class,
                () -> service.createBill(patientId, null, futureDate(), "0", null,
                        oneItem("   ", "1", "10")));
        assertThrows(ValidationException.class,
                () -> service.createBill(patientId, null, futureDate(), "0", null,
                        oneItem(null, "1", "10")));
        assertThrows(ValidationException.class,
                () -> service.createBill(patientId, null, futureDate(), "0", null,
                        oneItem("a".repeat(251), "1", "10")));
    }

    @Test
    void quantityValidation() {
        login(adminUser);
        assertThrows(ValidationException.class,
                () -> service.createBill(patientId, null, futureDate(), "0", null,
                        oneItem("A", "0", "10")));
        assertThrows(ValidationException.class,
                () -> service.createBill(patientId, null, futureDate(), "0", null,
                        oneItem("A", "-1", "10")));
        assertThrows(ValidationException.class,
                () -> service.createBill(patientId, null, futureDate(), "0", null,
                        oneItem("A", "abc", "10")));
        assertThrows(ValidationException.class,
                () -> service.createBill(patientId, null, futureDate(), "0", null,
                        oneItem("A", "10000", "10")));
    }

    @Test
    void unitPriceValidation() {
        login(adminUser);
        assertThrows(ValidationException.class,
                () -> service.createBill(patientId, null, futureDate(), "0", null,
                        oneItem("A", "1", "-1")));
        assertThrows(ValidationException.class,
                () -> service.createBill(patientId, null, futureDate(), "0", null,
                        oneItem("A", "1", "abc")));
    }

    @Test
    void discountCannotExceedSubtotalOrBeNegative() {
        login(adminUser);
        assertThrows(ValidationException.class,
                () -> service.createBill(patientId, null, futureDate(), "-1", null,
                        oneItem("A", "1", "100")));
        assertThrows(ValidationException.class,
                () -> service.createBill(patientId, null, futureDate(), "200", null,
                        oneItem("A", "1", "100")));
        assertThrows(ValidationException.class,
                () -> service.createBill(patientId, null, futureDate(), "abc", null,
                        oneItem("A", "1", "100")));
    }

    @Test
    void appointmentPatientMismatchRejected() {
        login(adminUser);
        assertThrows(ValidationException.class,
                () -> service.createBill(patientId, appt2Id, futureDate(), "0", null,
                        oneItem("A", "1", "10")));
    }

    @Test
    void notesLengthValidation() {
        login(adminUser);
        assertThrows(ValidationException.class,
                () -> service.createBill(patientId, null, futureDate(), "0", "n".repeat(2001),
                        oneItem("A", "1", "10")));
    }

    // ---------- calculations ----------

    @Test
    void multiItemCalculation_exampleFromSpec() {
        login(adminUser);
        List<BillingService.BillItemInput> items = List.of(
                new BillingService.BillItemInput("Med1", "2", "150.50"),
                new BillingService.BillItemInput("Med2", "3", "99.99"));
        Bill b = service.createBill(patientId, null, futureDate(), "50.97", null, items);
        // 2*150.50 = 301.00; 3*99.99 = 299.97; subtotal = 600.97; discount 50.97; total = 550.00
        assertEquals(0, new BigDecimal("600.97").compareTo(b.getSubtotal()),
                "subtotal: " + b.getSubtotal());
        assertEquals(0, new BigDecimal("50.97").compareTo(b.getDiscount()),
                "discount: " + b.getDiscount());
        assertEquals(0, new BigDecimal("550.00").compareTo(b.getTotalAmount()),
                "total: " + b.getTotalAmount());

        List<BillItem> stored = service.getItems(b.getId());
        assertEquals(2, stored.size());
        assertEquals(0, new BigDecimal("301.00").compareTo(stored.get(0).getAmount()));
        assertEquals(0, new BigDecimal("299.97").compareTo(stored.get(1).getAmount()));
    }

    @Test
    void freeItemAllowed_zeroPrice() {
        login(adminUser);
        Bill b = service.createBill(patientId, null, futureDate(), "0", null,
                oneItem("Sample", "1", "0"));
        assertEquals(0, BigDecimal.ZERO.compareTo(b.getTotalAmount()));
    }

    @Test
    void billNumberGeneratedAndUnique() {
        login(adminUser);
        Bill b1 = service.createBill(patientId, null, futureDate(), "0", null, oneItem("A", "1", "10"));
        Bill b2 = service.createBill(patient2Id, null, futureDate(), "0", null, oneItem("B", "1", "20"));
        assertEquals("BILL-000001", b1.getBillNumber());
        assertEquals("BILL-000002", b2.getBillNumber());
    }

    @Test
    void billStartsUnpaid() {
        login(adminUser);
        Bill b = service.createBill(patientId, null, futureDate(), "0", null, oneItem("A", "1", "10"));
        assertEquals(Bill.STATUS_UNPAID, b.getStatus());
    }

    // ---------- lifecycle ----------

    @Test
    void cancelledBillCannotBeEditedOrChangeStatus() {
        login(adminUser);
        Bill b = service.createBill(patientId, null, futureDate(), "0", null, oneItem("A", "1", "10"));
        service.markStatus(b.getId(), Bill.STATUS_CANCELLED);
        assertThrows(ValidationException.class,
                () -> service.updateBill(b.getId(), null, futureDate(), "0", null,
                        oneItem("A", "1", "20")));
        assertThrows(ValidationException.class,
                () -> service.markStatus(b.getId(), Bill.STATUS_PAID));
    }

    @Test
    void invalidStatusTransitionsRejected() {
        login(adminUser);
        Bill b = service.createBill(patientId, null, futureDate(), "0", null, oneItem("A", "1", "10"));
        service.markStatus(b.getId(), Bill.STATUS_PARTIALLY_PAID);
        service.markStatus(b.getId(), Bill.STATUS_PAID); // partial → paid ok
        assertThrows(ValidationException.class,
                () -> service.markStatus(b.getId(), Bill.STATUS_UNPAID)); // paid → unpaid invalid
    }

    @Test
    void appointmentCreatesOkAndConsistent() {
        login(adminUser);
        Bill b = service.createBill(patientId, appt1Id, futureDate(), "0", null,
                oneItem("Consultation", "1", "500"));
        assertEquals(appt1Id, b.getAppointmentId());
        assertEquals(patientId, b.getPatientId());
    }

    @Test
    void unknownBillEditRejected() {
        login(adminUser);
        assertThrows(ValidationException.class,
                () -> service.updateBill(9999, null, futureDate(), "0", null, oneItem("A", "1", "10")));
    }

    @Test
    void transactionRollback_partialFailureLeavesNothing() {
        login(adminUser);
        var bad = List.of(
                new BillingService.BillItemInput("A", "1", "10"),
                new BillingService.BillItemInput(null, "1", "10")); // blank description → fail
        assertThrows(ValidationException.class,
                () -> service.createBill(patientId, null, futureDate(), "0", null, bad));
        assertEquals(0, billDao.count());
    }

    @Test
    void inactivePatientCannotBeBilled() {
        login(adminUser);
        // Deactivate patient via PatientDao (we can toggle using raw SQL since we only need the flag)
        try (var conn = db.getConnection(); var ps = conn.prepareStatement("UPDATE patients SET is_active = 0 WHERE id = ?")) {
            ps.setInt(1, patientId); ps.executeUpdate();
        } catch (Exception e) { throw new RuntimeException(e); }
        assertThrows(ValidationException.class,
                () -> service.createBill(patientId, null, futureDate(), "0", null,
                        oneItem("A", "1", "10")));
    }
}
