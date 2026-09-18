package com.hospital.dao;

import com.hospital.config.DatabaseConnection;
import com.hospital.config.DatabaseInitializer;
import com.hospital.config.TestDatabaseFactory;
import com.hospital.exception.DatabaseException;
import com.hospital.model.Bill;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BillDaoTest {

    private DatabaseConnection db;
    private BillDao dao;
    private BillItemDao itemDao;
    private int patientId;
    private int patient2Id;
    private int doctorId;
    private int doctor2Id;
    private int appt1Id;
    private int appt2Id;

    @BeforeEach
    void setUp() throws IOException {
        db = TestDatabaseFactory.createTempDatabase();
        new DatabaseInitializer(db).initialize();
        dao = new BillDaoImpl(db);
        itemDao = new BillItemDaoImpl(db);

        PatientDao patientDao = new PatientDaoImpl(db);
        DoctorDao doctorDao = new DoctorDaoImpl(db);
        DepartmentDao deptDao = new DepartmentDaoImpl(db);
        AppointmentDao apptDao = new AppointmentDaoImpl(db);

        patientId = patientDao.create("PAT-000001", "Alice", null, null, null, null, null, null, null, null);
        patient2Id = patientDao.create("PAT-000002", "Bob", null, null, null, null, null, null, null, null);
        int cardId = deptDao.create("Cardiology", null);
        doctorId = doctorDao.create(null, cardId, "Dr. Khan", "Cardiologist", null, null, 0);
        doctor2Id = doctorDao.create(null, cardId, "Dr. Lee", "Cardiologist", null, null, 0);
        appt1Id = apptDao.create(patientId, doctorId, "2030-03-01", "09:30", "Checkup", null);
        appt2Id = apptDao.create(patient2Id, doctor2Id, "2030-03-02", "10:00", "Follow-up", null);
    }

    @AfterEach
    void tearDown() throws IOException {
        TestDatabaseFactory.deleteDatabaseFile(db);
    }

    @Test
    void create_andFindById_loadsJoinedFields() {
        int id = dao.create("BILL-000001", patientId, appt1Id, "2030-03-01",
                Bill.STATUS_UNPAID, "first bill",
                new BigDecimal("100.00"), new BigDecimal("10.00"), new BigDecimal("90.00"));
        itemDao.create(id, "Consultation", 1, new BigDecimal("100.00"), new BigDecimal("100.00"));
        Bill b = dao.findById(id).orElseThrow();
        assertEquals("BILL-000001", b.getBillNumber());
        assertEquals(patientId, b.getPatientId());
        assertEquals(appt1Id, b.getAppointmentId());
        assertEquals(LocalDate.of(2030, 3, 1), b.getBillDate());
        assertEquals(Bill.STATUS_UNPAID, b.getStatus());
        assertEquals("first bill", b.getNotes());
        assertEquals(0, new BigDecimal("100.00").compareTo(b.getSubtotal()));
        assertEquals(0, new BigDecimal("10.00").compareTo(b.getDiscount()));
        assertEquals(0, new BigDecimal("90.00").compareTo(b.getTotalAmount()));
        assertEquals("Alice", b.getPatientName());
        assertEquals("PAT-000001", b.getPatientCode());
        assertEquals("Dr. Khan", b.getDoctorName());
        assertEquals(1, b.getItemCount());
        assertNotNull(b.getCreatedAt());
    }

    @Test
    void findByBillNumber_returnsBill() {
        int id = dao.create("BILL-000099", patientId, null, "2030-03-01", Bill.STATUS_UNPAID, null,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        assertTrue(dao.findByBillNumber("BILL-000099").isPresent());
        assertEquals(id, dao.findByBillNumber("BILL-000099").get().getId());
        assertTrue(dao.findByBillNumber("BILL-999999").isEmpty());
    }

    @Test
    void findById_unknownReturnsEmpty() {
        assertTrue(dao.findById(9999).isEmpty());
    }

    @Test
    void update_modifiesFields() {
        int id = dao.create("BILL-000001", patientId, null, "2030-03-01", Bill.STATUS_UNPAID, null,
                new BigDecimal("100.00"), BigDecimal.ZERO, new BigDecimal("100.00"));
        dao.update(id, appt1Id, "2030-03-05", "updated",
                new BigDecimal("200.00"), new BigDecimal("20.00"), new BigDecimal("180.00"));
        Bill b = dao.findById(id).orElseThrow();
        assertEquals(appt1Id, b.getAppointmentId());
        assertEquals(LocalDate.of(2030, 3, 5), b.getBillDate());
        assertEquals("updated", b.getNotes());
        assertEquals(0, new BigDecimal("200.00").compareTo(b.getSubtotal()));
    }

    @Test
    void updateStatus_changesStatus() {
        int id = dao.create("BILL-000001", patientId, null, "2030-03-01", Bill.STATUS_UNPAID, null,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        dao.updateStatus(id, Bill.STATUS_PAID);
        assertEquals(Bill.STATUS_PAID, dao.findById(id).orElseThrow().getStatus());
    }

    @Test
    void updateBillNumber_setsFinalNumber() {
        int id = dao.create("PENDING", patientId, null, "2030-03-01", Bill.STATUS_UNPAID, null,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        dao.updateBillNumber(id, "BILL-000001");
        assertEquals("BILL-000001", dao.findById(id).orElseThrow().getBillNumber());
    }

    @Test
    void duplicateBillNumberRejected() {
        dao.create("BILL-000001", patientId, null, "2030-03-01", Bill.STATUS_UNPAID, null,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        assertThrows(DatabaseException.class, () ->
                dao.create("BILL-000001", patient2Id, null, "2030-03-02", Bill.STATUS_UNPAID, null,
                        BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
    }

    @Test
    void foreignKeyRejectsBadPatient() {
        assertThrows(DatabaseException.class, () ->
                dao.create("BILL-000001", 9999, null, "2030-03-01", Bill.STATUS_UNPAID, null,
                        BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
    }

    @Test
    void foreignKeyRejectsBadAppointment() {
        assertThrows(DatabaseException.class, () ->
                dao.create("BILL-000001", patientId, 9999, "2030-03-01", Bill.STATUS_UNPAID, null,
                        BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
    }

    @Test
    void search_byBillNumberPatientCodeDoctorAndItemDescription() {
        int id1 = dao.create("BILL-000001", patientId, appt1Id, "2030-03-01", Bill.STATUS_UNPAID, null,
                new BigDecimal("100.00"), BigDecimal.ZERO, new BigDecimal("100.00"));
        int id2 = dao.create("BILL-000002", patient2Id, appt2Id, "2030-03-02", Bill.STATUS_PAID, null,
                new BigDecimal("200.00"), BigDecimal.ZERO, new BigDecimal("200.00"));
        itemDao.create(id1, "Consultation", 1, new BigDecimal("100.00"), new BigDecimal("100.00"));
        itemDao.create(id2, "X-Ray", 1, new BigDecimal("200.00"), new BigDecimal("200.00"));

        assertEquals(1, dao.search("BILL-000001", null).size());
        assertEquals(1, dao.search("bill-000002", null).size());
        assertEquals(1, dao.search("alice", null).size());
        assertEquals(1, dao.search("BOB", null).size());
        assertEquals(1, dao.search("PAT-000001", null).size());
        assertEquals(1, dao.search("khan", null).size());
        assertEquals(1, dao.search("LEE", null).size());
        assertEquals(1, dao.search("consult", null).size());
        assertEquals(1, dao.search("x-ray", null).size());
        assertEquals(0, dao.search("zzzz", null).size());
        assertEquals(2, dao.search(null, null).size());
    }

    @Test
    void search_filterByStatus() {
        dao.create("BILL-000001", patientId, null, "2030-03-01", Bill.STATUS_UNPAID, null,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        dao.create("BILL-000002", patient2Id, null, "2030-03-02", Bill.STATUS_PAID, null,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        assertEquals(1, dao.search(null, Bill.STATUS_UNPAID).size());
        assertEquals(1, dao.search(null, Bill.STATUS_PAID).size());
        assertEquals(0, dao.search(null, Bill.STATUS_CANCELLED).size());
    }

    @Test
    void count_reflectsInsertions() {
        assertEquals(0, dao.count());
        dao.create("BILL-000001", patientId, null, "2030-03-01", Bill.STATUS_UNPAID, null, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        dao.create("BILL-000002", patient2Id, null, "2030-03-02", Bill.STATUS_PAID, null, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        assertEquals(2, dao.count());
    }
}
