package com.hospital.dao;

import com.hospital.config.DatabaseConnection;
import com.hospital.config.DatabaseInitializer;
import com.hospital.config.TestDatabaseFactory;
import com.hospital.exception.DatabaseException;
import com.hospital.model.BillItem;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BillItemDaoTest {

    private DatabaseConnection db;
    private BillItemDao dao;
    private int billId;

    @BeforeEach
    void setUp() throws IOException {
        db = TestDatabaseFactory.createTempDatabase();
        new DatabaseInitializer(db).initialize();
        dao = new BillItemDaoImpl(db);

        PatientDao patientDao = new PatientDaoImpl(db);
        DoctorDao doctorDao = new DoctorDaoImpl(db);
        DepartmentDao deptDao = new DepartmentDaoImpl(db);
        AppointmentDao apptDao = new AppointmentDaoImpl(db);
        BillDao billDao = new BillDaoImpl(db);

        int patientId = patientDao.create("PAT-000001", "Alice", null, null, null, null, null, null, null, null);
        int deptId = deptDao.create("Cardiology", null);
        int doctorId = doctorDao.create(null, deptId, "Dr. Khan", "Cardiologist", null, null, 0);
        int apptId = apptDao.create(patientId, doctorId, "2030-03-01", "09:30", null, null);
        billId = billDao.create("BILL-000001", patientId, apptId, "2030-03-01", "UNPAID", null,
                new BigDecimal("100.00"), BigDecimal.ZERO, new BigDecimal("100.00"));
    }

    @AfterEach
    void tearDown() throws IOException {
        TestDatabaseFactory.deleteDatabaseFile(db);
    }

    @Test
    void create_andFindById() {
        int id = dao.create(billId, "Consultation", 1, new BigDecimal("100.00"), new BigDecimal("100.00"));
        BillItem it = dao.findById(id).orElseThrow();
        assertEquals(billId, it.getBillId());
        assertEquals("Consultation", it.getDescription());
        assertEquals(1, it.getQuantity());
        assertEquals(0, new BigDecimal("100.00").compareTo(it.getUnitPrice()));
        assertEquals(0, new BigDecimal("100.00").compareTo(it.getAmount()));
        assertNotNull(it.getCreatedAt());
    }

    @Test
    void findById_unknownReturnsEmpty() {
        assertTrue(dao.findById(9999).isEmpty());
    }

    @Test
    void findByBillId_returnsAllInOrder() {
        dao.create(billId, "A", 1, new BigDecimal("10.00"), new BigDecimal("10.00"));
        dao.create(billId, "B", 2, new BigDecimal("20.00"), new BigDecimal("40.00"));
        List<BillItem> list = dao.findByBillId(billId);
        assertEquals(2, list.size());
        assertEquals("A", list.get(0).getDescription());
        assertEquals("B", list.get(1).getDescription());
    }

    @Test
    void update_modifiesFields() {
        int id = dao.create(billId, "A", 1, new BigDecimal("10.00"), new BigDecimal("10.00"));
        dao.update(id, "B", 3, new BigDecimal("5.00"), new BigDecimal("15.00"));
        BillItem it = dao.findById(id).orElseThrow();
        assertEquals("B", it.getDescription());
        assertEquals(3, it.getQuantity());
        assertEquals(0, new BigDecimal("5.00").compareTo(it.getUnitPrice()));
        assertEquals(0, new BigDecimal("15.00").compareTo(it.getAmount()));
    }

    @Test
    void update_unknownThrows() {
        assertThrows(DatabaseException.class,
                () -> dao.update(9999, "A", 1, BigDecimal.ONE, BigDecimal.ONE));
    }

    @Test
    void delete_removesItem() {
        int id = dao.create(billId, "A", 1, BigDecimal.ONE, BigDecimal.ONE);
        dao.delete(id);
        assertTrue(dao.findById(id).isEmpty());
    }

    @Test
    void deleteAllByBillId_removesAll() {
        dao.create(billId, "A", 1, BigDecimal.ONE, BigDecimal.ONE);
        dao.create(billId, "B", 2, BigDecimal.TEN, new BigDecimal("20.00"));
        dao.deleteAllByBillId(billId);
        assertEquals(0, dao.findByBillId(billId).size());
    }

    @Test
    void cascadeDeletesItemsWhenBillDeleted() {
        dao.create(billId, "A", 1, BigDecimal.ONE, BigDecimal.ONE);
        BillDao billDao = new BillDaoImpl(db);
        try (var conn = db.getConnection(); var ps = conn.prepareStatement("DELETE FROM bills WHERE id = ?")) {
            ps.setInt(1, billId);
            ps.executeUpdate();
        } catch (Exception e) { throw new RuntimeException(e); }
        assertEquals(0, dao.count());
    }

    @Test
    void foreignKeyRejectsBadBill() {
        assertThrows(DatabaseException.class,
                () -> dao.create(9999, "A", 1, BigDecimal.ONE, BigDecimal.ONE));
    }

    @Test
    void count() {
        assertEquals(0, dao.count());
        dao.create(billId, "A", 1, BigDecimal.ONE, BigDecimal.ONE);
        dao.create(billId, "B", 2, BigDecimal.TEN, new BigDecimal("20.00"));
        assertEquals(2, dao.count());
    }
}
