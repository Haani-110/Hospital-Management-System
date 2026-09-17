package com.hospital.dao;

import com.hospital.config.DatabaseConnection;
import com.hospital.config.DatabaseInitializer;
import com.hospital.config.TestDatabaseFactory;
import com.hospital.exception.DatabaseException;
import com.hospital.model.PrescriptionItem;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PrescriptionItemDaoTest {

    private DatabaseConnection db;
    private PrescriptionItemDao dao;
    private int prescriptionId;

    @BeforeEach
    void setUp() throws IOException {
        db = TestDatabaseFactory.createTempDatabase();
        new DatabaseInitializer(db).initialize();
        dao = new PrescriptionItemDaoImpl(db);

        PatientDao patientDao = new PatientDaoImpl(db);
        DoctorDao doctorDao = new DoctorDaoImpl(db);
        DepartmentDao deptDao = new DepartmentDaoImpl(db);
        AppointmentDao apptDao = new AppointmentDaoImpl(db);
        MedicalRecordDao recordDao = new MedicalRecordDaoImpl(db);
        PrescriptionDao prescriptionDao = new PrescriptionDaoImpl(db);

        int patientId = patientDao.create("PAT-000001", "Alice", null, null, null, null, null, null, null, null);
        int deptId = deptDao.create("Cardiology", null);
        int doctorId = doctorDao.create(null, deptId, "Dr. Khan", "Cardiologist", null, null, 0);
        int apptId = apptDao.create(patientId, doctorId, "2030-01-15", "09:30", null, null);
        int recordId = recordDao.create(apptId, patientId, doctorId, "D", null, null, null, "2030-01-15");
        prescriptionId = prescriptionDao.create(recordId, patientId, doctorId, "2030-01-15", null);
    }

    @AfterEach
    void tearDown() throws IOException {
        TestDatabaseFactory.deleteDatabaseFile(db);
    }

    @Test
    void create_andFindById() {
        int id = dao.create(prescriptionId, "Paracetamol", "500mg", "BD", "5 days", "after meals");
        PrescriptionItem it = dao.findById(id).orElseThrow();
        assertEquals(prescriptionId, it.getPrescriptionId());
        assertEquals("Paracetamol", it.getMedicineName());
        assertEquals("500mg", it.getDosage());
        assertEquals("BD", it.getFrequency());
        assertEquals("5 days", it.getDuration());
        assertEquals("after meals", it.getInstructions());
        assertNotNull(it.getCreatedAt());
        assertNotNull(it.getUpdatedAt());
    }

    @Test
    void findById_unknownReturnsEmpty() {
        assertTrue(dao.findById(9999).isEmpty());
    }

    @Test
    void findByPrescriptionId_returnsItemsInOrder() {
        dao.create(prescriptionId, "Med A", "1", "BD", "1d", null);
        dao.create(prescriptionId, "Med B", "2", "TDS", "2d", null);
        List<PrescriptionItem> items = dao.findByPrescriptionId(prescriptionId);
        assertEquals(2, items.size());
        assertEquals("Med A", items.get(0).getMedicineName());
        assertEquals("Med B", items.get(1).getMedicineName());
    }

    @Test
    void update_modifiesFields() {
        int id = dao.create(prescriptionId, "A", "1", "BD", "1d", null);
        dao.update(id, "B", "2", "TDS", "2d", "with food");
        PrescriptionItem it = dao.findById(id).orElseThrow();
        assertEquals("B", it.getMedicineName());
        assertEquals("2", it.getDosage());
        assertEquals("TDS", it.getFrequency());
        assertEquals("2d", it.getDuration());
        assertEquals("with food", it.getInstructions());
    }

    @Test
    void update_unknownThrows() {
        assertThrows(DatabaseException.class,
                () -> dao.update(9999, "A", "1", "BD", "1d", null));
    }

    @Test
    void delete_removesItem() {
        int id = dao.create(prescriptionId, "A", "1", "BD", "1d", null);
        dao.delete(id);
        assertTrue(dao.findById(id).isEmpty());
        assertEquals(0, dao.findByPrescriptionId(prescriptionId).size());
    }

    @Test
    void deleteByPrescriptionId_removesAll() {
        dao.create(prescriptionId, "A", "1", "BD", "1d", null);
        dao.create(prescriptionId, "B", "2", "TDS", "2d", null);
        dao.deleteByPrescriptionId(prescriptionId);
        assertEquals(0, dao.findByPrescriptionId(prescriptionId).size());
    }

    @Test
    void countAndCountByPrescriptionId() {
        assertEquals(0, dao.count());
        assertEquals(0, dao.countByPrescriptionId(prescriptionId));
        dao.create(prescriptionId, "A", "1", "BD", "1d", null);
        dao.create(prescriptionId, "B", "2", "TDS", "2d", null);
        assertEquals(2, dao.count());
        assertEquals(2, dao.countByPrescriptionId(prescriptionId));
    }

    @Test
    void foreignKeyRejectsBadPrescription() {
        assertThrows(DatabaseException.class,
                () -> dao.create(9999, "A", "1", "BD", "1d", null));
    }
}
