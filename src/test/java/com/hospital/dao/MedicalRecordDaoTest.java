package com.hospital.dao;

import com.hospital.config.DatabaseConnection;
import com.hospital.config.DatabaseInitializer;
import com.hospital.config.TestDatabaseFactory;
import com.hospital.exception.DatabaseException;
import com.hospital.model.MedicalRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MedicalRecordDaoTest {

    private DatabaseConnection db;
    private MedicalRecordDao dao;
    private int patientId;
    private int doctorId;
    private int patient2Id;
    private int doctor2Id;
    private int appt1Id;
    private int appt2Id;

    @BeforeEach
    void setUp() throws IOException {
        db = TestDatabaseFactory.createTempDatabase();
        new DatabaseInitializer(db).initialize();
        dao = new MedicalRecordDaoImpl(db);

        PatientDao patientDao = new PatientDaoImpl(db);
        DoctorDao doctorDao = new DoctorDaoImpl(db);
        DepartmentDao deptDao = new DepartmentDaoImpl(db);
        AppointmentDao apptDao = new AppointmentDaoImpl(db);

        patientId = patientDao.create("PAT-000001", "Alice", null, null, null, null, null, null, null, null);
        patient2Id = patientDao.create("PAT-000002", "Bob", null, null, null, null, null, null, null, null);
        int cardId = deptDao.create("Cardiology", null);
        int neuroId = deptDao.create("Neurology", null);
        doctorId = doctorDao.create(null, cardId, "Dr. Khan", "Cardiologist", null, null, 0);
        doctor2Id = doctorDao.create(null, neuroId, "Dr. Lee", "Neurologist", null, null, 0);

        appt1Id = apptDao.create(patientId, doctorId, "2030-01-15", "09:30", "Headache", null);
        appt2Id = apptDao.create(patient2Id, doctor2Id, "2030-01-16", "10:00", "Checkup", null);
    }

    @AfterEach
    void tearDown() throws IOException {
        TestDatabaseFactory.deleteDatabaseFile(db);
    }

    @Test
    void create_andFindById_loadsJoinedFields() {
        int id = dao.create(appt1Id, patientId, doctorId, "Migraine", "nausea", "normal", "rest", "2030-01-15");
        assertTrue(id > 0);
        MedicalRecord m = dao.findById(id).orElseThrow();
        assertEquals(appt1Id, m.getAppointmentId());
        assertEquals(patientId, m.getPatientId());
        assertEquals(doctorId, m.getDoctorId());
        assertEquals("Migraine", m.getDiagnosis());
        assertEquals("nausea", m.getSymptoms());
        assertEquals("normal", m.getExamination());
        assertEquals("rest", m.getTreatmentNotes());
        assertEquals(LocalDate.of(2030, 1, 15), m.getRecordDate());
        assertEquals("Alice", m.getPatientName());
        assertEquals("PAT-000001", m.getPatientCode());
        assertEquals("Dr. Khan", m.getDoctorName());
        assertEquals(LocalDate.of(2030, 1, 15), m.getAppointmentDate());
        assertNotNull(m.getCreatedAt());
        assertNotNull(m.getUpdatedAt());
    }

    @Test
    void findById_unknownReturnsEmpty() {
        assertTrue(dao.findById(9999).isEmpty());
    }

    @Test
    void findByAppointment_returnsRecord() {
        dao.create(appt1Id, patientId, doctorId, "D", null, null, null, "2030-01-15");
        assertTrue(dao.findByAppointment(appt2Id).isEmpty());
        MedicalRecord m = dao.findByAppointment(appt1Id).orElseThrow();
        assertEquals("D", m.getDiagnosis());
    }

    @Test
    void update_modifiesFields() {
        int id = dao.create(appt1Id, patientId, doctorId, "D", "s", "e", "t", "2030-01-15");
        dao.update(id, "D2", "s2", "e2", "t2", "2030-01-16");
        MedicalRecord m = dao.findById(id).orElseThrow();
        assertEquals("D2", m.getDiagnosis());
        assertEquals("s2", m.getSymptoms());
        assertEquals("e2", m.getExamination());
        assertEquals("t2", m.getTreatmentNotes());
        assertEquals(LocalDate.of(2030, 1, 16), m.getRecordDate());
    }

    @Test
    void update_unknownIdThrows() {
        assertThrows(DatabaseException.class,
                () -> dao.update(9999, "D", null, null, null, "2030-01-15"));
    }

    @Test
    void search_byPatientNameCaseInsensitive() {
        dao.create(appt1Id, patientId, doctorId, "Dx", null, null, null, "2030-01-15");
        dao.create(appt2Id, patient2Id, doctor2Id, "Dy", null, null, null, "2030-01-16");
        assertEquals(1, dao.search("ALICE").size());
        assertEquals(1, dao.search("bob").size());
    }

    @Test
    void search_byPatientCode() {
        dao.create(appt1Id, patientId, doctorId, "Dx", null, null, null, "2030-01-15");
        dao.create(appt2Id, patient2Id, doctor2Id, "Dy", null, null, null, "2030-01-16");
        assertEquals(1, dao.search("PAT-000002").size());
    }

    @Test
    void search_byDoctor() {
        dao.create(appt1Id, patientId, doctorId, "Dx", null, null, null, "2030-01-15");
        dao.create(appt2Id, patient2Id, doctor2Id, "Dy", null, null, null, "2030-01-16");
        assertEquals(1, dao.search("lee").size());
        assertEquals(1, dao.search("KHAN").size());
    }

    @Test
    void search_byDiagnosisCaseInsensitive() {
        dao.create(appt1Id, patientId, doctorId, "Migraine", null, null, null, "2030-01-15");
        dao.create(appt2Id, patient2Id, doctor2Id, "Fracture", null, null, null, "2030-01-16");
        assertEquals(1, dao.search("migraine").size());
        assertEquals(1, dao.search("FRACTURE").size());
    }

    @Test
    void search_bySymptoms() {
        dao.create(appt1Id, patientId, doctorId, "Dx", "fever and cough", null, null, "2030-01-15");
        dao.create(appt2Id, patient2Id, doctor2Id, "Dy", "headache", null, null, "2030-01-16");
        assertEquals(1, dao.search("fever").size());
    }

    @Test
    void existsByAppointment_detectsDuplicate() {
        assertFalse(dao.existsByAppointment(appt1Id));
        dao.create(appt1Id, patientId, doctorId, "Dx", null, null, null, "2030-01-15");
        assertTrue(dao.existsByAppointment(appt1Id));
        assertFalse(dao.existsByAppointment(appt2Id));
    }

    @Test
    void duplicateAppointmentRejectedAtDbLevel() {
        dao.create(appt1Id, patientId, doctorId, "Dx", null, null, null, "2030-01-15");
        assertThrows(DatabaseException.class,
                () -> dao.create(appt1Id, patientId, doctorId, "Dy", null, null, null, "2030-01-15"));
    }

    @Test
    void foreignKeyRejectsBadAppointment() {
        assertThrows(DatabaseException.class,
                () -> dao.create(9999, patientId, doctorId, "Dx", null, null, null, "2030-01-15"));
    }

    @Test
    void count_reflectsInsertions() {
        assertEquals(0, dao.count());
        dao.create(appt1Id, patientId, doctorId, "Dx", null, null, null, "2030-01-15");
        dao.create(appt2Id, patient2Id, doctor2Id, "Dy", null, null, null, "2030-01-16");
        assertEquals(2, dao.count());
    }
}
