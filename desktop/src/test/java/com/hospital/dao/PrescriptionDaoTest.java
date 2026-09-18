package com.hospital.dao;

import com.hospital.config.DatabaseConnection;
import com.hospital.config.DatabaseInitializer;
import com.hospital.config.TestDatabaseFactory;
import com.hospital.exception.DatabaseException;
import com.hospital.model.Prescription;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PrescriptionDaoTest {

    private DatabaseConnection db;
    private PrescriptionDao dao;
    private PrescriptionItemDao itemDao;
    private int patientId;
    private int patient2Id;
    private int doctorId;
    private int doctor2Id;
    private int recordId1;
    private int recordId2;

    @BeforeEach
    void setUp() throws IOException {
        db = TestDatabaseFactory.createTempDatabase();
        new DatabaseInitializer(db).initialize();
        dao = new PrescriptionDaoImpl(db);
        itemDao = new PrescriptionItemDaoImpl(db);

        PatientDao patientDao = new PatientDaoImpl(db);
        DoctorDao doctorDao = new DoctorDaoImpl(db);
        DepartmentDao deptDao = new DepartmentDaoImpl(db);
        AppointmentDao apptDao = new AppointmentDaoImpl(db);
        MedicalRecordDao recordDao = new MedicalRecordDaoImpl(db);

        patientId = patientDao.create("PAT-000001", "Alice", null, null, null, null, null, null, null, null);
        patient2Id = patientDao.create("PAT-000002", "Bob", null, null, null, null, null, null, null, null);
        int cardId = deptDao.create("Cardiology", null);
        doctorId = doctorDao.create(null, cardId, "Dr. Khan", "Cardiologist", null, null, 0);
        doctor2Id = doctorDao.create(null, cardId, "Dr. Lee", "Cardiologist", null, null, 0);

        int appt1 = apptDao.create(patientId, doctorId, "2030-01-15", "09:30", null, null);
        int appt2 = apptDao.create(patient2Id, doctor2Id, "2030-01-16", "10:00", null, null);
        recordId1 = recordDao.create(appt1, patientId, doctorId, "D1", null, null, null, "2030-01-15");
        recordId2 = recordDao.create(appt2, patient2Id, doctor2Id, "D2", null, null, null, "2030-01-16");
    }

    @AfterEach
    void tearDown() throws IOException {
        TestDatabaseFactory.deleteDatabaseFile(db);
    }

    @Test
    void create_andFindById_loadsJoinedFieldsAndCount() {
        int id = dao.create(recordId1, patientId, doctorId, "2030-01-15", "rest");
        itemDao.create(id, "Paracetamol", "500mg", "BD", "5 days", "after meals");
        itemDao.create(id, "Ibuprofen", "200mg", "SOS", "3 days", null);
        Prescription p = dao.findById(id).orElseThrow();
        assertEquals(recordId1, p.getMedicalRecordId());
        assertEquals(patientId, p.getPatientId());
        assertEquals(doctorId, p.getDoctorId());
        assertEquals(LocalDate.of(2030, 1, 15), p.getPrescriptionDate());
        assertEquals("rest", p.getNotes());
        assertEquals("Alice", p.getPatientName());
        assertEquals("PAT-000001", p.getPatientCode());
        assertEquals("Dr. Khan", p.getDoctorName());
        assertEquals(2, p.getItemCount());
        assertNotNull(p.getCreatedAt());
        assertNotNull(p.getUpdatedAt());
    }

    @Test
    void findByMedicalRecordId_returnsPrescription() {
        int id = dao.create(recordId1, patientId, doctorId, "2030-01-15", null);
        assertTrue(dao.findByMedicalRecordId(recordId2).isEmpty());
        Prescription p = dao.findByMedicalRecordId(recordId1).orElseThrow();
        assertEquals(id, p.getId());
    }

    @Test
    void findById_unknownReturnsEmpty() {
        assertTrue(dao.findById(9999).isEmpty());
    }

    @Test
    void update_modifiesDateAndNotes() {
        int id = dao.create(recordId1, patientId, doctorId, "2030-01-15", "a");
        dao.update(id, "2030-02-20", "b");
        Prescription p = dao.findById(id).orElseThrow();
        assertEquals(LocalDate.of(2030, 2, 20), p.getPrescriptionDate());
        assertEquals("b", p.getNotes());
    }

    @Test
    void update_unknownThrows() {
        assertThrows(DatabaseException.class, () -> dao.update(9999, "2030-01-01", null));
    }

    @Test
    void duplicateMedicalRecordRejected() {
        dao.create(recordId1, patientId, doctorId, "2030-01-15", null);
        assertThrows(DatabaseException.class,
                () -> dao.create(recordId1, patientId, doctorId, "2030-01-16", null));
    }

    @Test
    void foreignKeyRejectsBadRecord() {
        assertThrows(DatabaseException.class,
                () -> dao.create(9999, patientId, doctorId, "2030-01-15", null));
    }

    @Test
    void search_byPatientNameDoctorAndMedicine() {
        int id1 = dao.create(recordId1, patientId, doctorId, "2030-01-15", null);
        int id2 = dao.create(recordId2, patient2Id, doctor2Id, "2030-01-16", null);
        itemDao.create(id1, "Paracetamol", "500mg", "BD", "5d", null);
        itemDao.create(id2, "Amoxicillin", "250mg", "TDS", "7d", null);
        assertEquals(1, dao.search("alice").size());
        assertEquals(1, dao.search("BOB").size());
        assertEquals(1, dao.search("khan").size());
        assertEquals(1, dao.search("LEE").size());
        assertEquals(1, dao.search("paracetamol").size());
        assertEquals(1, dao.search("AMOXI").size());
        // Date is not part of the search contract (which covers patient, code, doctor, medicine).
        // A date fragment therefore matches no prescriptions.
        assertEquals(0, dao.search("2030").size());
        // An empty/blank query returns all prescriptions (findAll).
        assertEquals(2, dao.search(null).size());
        assertEquals(2, dao.search("").size());
    }

    @Test
    void existsByMedicalRecord_detectsDuplicate() {
        assertFalse(dao.existsByMedicalRecord(recordId1));
        dao.create(recordId1, patientId, doctorId, "2030-01-15", null);
        assertTrue(dao.existsByMedicalRecord(recordId1));
        assertFalse(dao.existsByMedicalRecord(recordId2));
    }

    @Test
    void count_reflectsInsertions() {
        assertEquals(0, dao.count());
        dao.create(recordId1, patientId, doctorId, "2030-01-15", null);
        dao.create(recordId2, patient2Id, doctor2Id, "2030-01-16", null);
        assertEquals(2, dao.count());
    }
}
