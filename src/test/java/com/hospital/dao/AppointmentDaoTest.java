package com.hospital.dao;

import com.hospital.config.DatabaseConnection;
import com.hospital.config.DatabaseInitializer;
import com.hospital.config.TestDatabaseFactory;
import com.hospital.exception.DatabaseException;
import com.hospital.model.Appointment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AppointmentDaoTest {

    private DatabaseConnection db;
    private AppointmentDao dao;

    private int patientId;
    private int patient2Id;
    private int doctorId;
    private int doctor2Id;

    @BeforeEach
    void setUp() throws IOException {
        db = TestDatabaseFactory.createTempDatabase();
        new DatabaseInitializer(db).initialize();
        dao = new AppointmentDaoImpl(db);
        PatientDao patientDao = new PatientDaoImpl(db);
        DepartmentDao deptDao = new DepartmentDaoImpl(db);
        DoctorDao doctorDao = new DoctorDaoImpl(db);

        patientId = patientDao.create("PAT-000001", "Alice", null, null, null, null, null, null, null, null);
        patient2Id = patientDao.create("PAT-000002", "Bob", null, null, null, null, null, null, null, null);
        int cardId = deptDao.create("Cardiology", null);
        int neuroId = deptDao.create("Neurology", null);
        doctorId = doctorDao.create(null, cardId, "Dr. Khan", "Cardiologist", null, null, 0);
        doctor2Id = doctorDao.create(null, neuroId, "Dr. Lee", "Neurologist", null, null, 0);
    }

    @AfterEach
    void tearDown() throws IOException {
        TestDatabaseFactory.deleteDatabaseFile(db);
    }

    @Test
    void create_defaultsToScheduledAndLoadsJoinedFields() {
        int id = dao.create(patientId, doctorId, "2030-01-15", "09:30", "Checkup", "notes");
        assertTrue(id > 0);
        Appointment a = dao.findById(id).orElseThrow();
        assertEquals(patientId, a.getPatientId());
        assertEquals(doctorId, a.getDoctorId());
        assertEquals(LocalDate.of(2030, 1, 15), a.getAppointmentDate());
        assertEquals(LocalTime.of(9, 30), a.getAppointmentTime());
        assertEquals("Checkup", a.getReason());
        assertEquals("notes", a.getNotes());
        assertEquals("SCHEDULED", a.getStatus());
        assertEquals("Alice", a.getPatientName());
        assertEquals("PAT-000001", a.getPatientCode());
        assertEquals("Dr. Khan", a.getDoctorName());
        assertEquals("Cardiologist", a.getSpecialization());
        assertNotNull(a.getCreatedAt());
        assertNotNull(a.getUpdatedAt());
    }

    @Test
    void findById_unknownReturnsEmpty() {
        assertTrue(dao.findById(9999).isEmpty());
    }

    @Test
    void findAll_ordersByDateDescTimeAsc() {
        dao.create(patientId, doctorId, "2030-01-15", "09:30", "a", null);
        dao.create(patientId, doctorId, "2030-01-15", "10:00", "b", null);
        dao.create(patientId, doctorId, "2030-01-14", "11:00", "c", null);
        List<Appointment> all = dao.findAll();
        assertEquals(3, all.size());
        assertEquals(LocalTime.of(9, 30), all.get(0).getAppointmentTime()); // 2030-01-15 09:30
        assertEquals(LocalTime.of(10, 0), all.get(1).getAppointmentTime()); // 2030-01-15 10:00
        assertEquals(LocalDate.of(2030, 1, 14), all.get(2).getAppointmentDate());
    }

    @Test
    void update_changesAllEditableFields() {
        int id = dao.create(patientId, doctorId, "2030-01-15", "09:30", "Checkup", null);
        dao.update(id, patient2Id, doctor2Id, "2030-02-20", "14:00", "Follow-up", "new notes");
        Appointment a = dao.findById(id).orElseThrow();
        assertEquals(patient2Id, a.getPatientId());
        assertEquals(doctor2Id, a.getDoctorId());
        assertEquals(LocalDate.of(2030, 2, 20), a.getAppointmentDate());
        assertEquals(LocalTime.of(14, 0), a.getAppointmentTime());
        assertEquals("Follow-up", a.getReason());
        assertEquals("new notes", a.getNotes());
        assertEquals("SCHEDULED", a.getStatus());
    }

    @Test
    void update_unknownIdThrows() {
        assertThrows(DatabaseException.class,
                () -> dao.update(9999, patientId, doctorId, "2030-01-15", "09:30", null, null));
    }

    @Test
    void updateStatus_transitionsStatus() {
        int id = dao.create(patientId, doctorId, "2030-01-15", "09:30", null, null);
        assertEquals("SCHEDULED", dao.findById(id).orElseThrow().getStatus());
        dao.updateStatus(id, "COMPLETED");
        assertEquals("COMPLETED", dao.findById(id).orElseThrow().getStatus());
        dao.updateStatus(id, "CANCELLED");
        assertEquals("CANCELLED", dao.findById(id).orElseThrow().getStatus());
    }

    @Test
    void updateStatus_unknownThrows() {
        assertThrows(DatabaseException.class, () -> dao.updateStatus(9999, "CANCELLED"));
    }

    @Test
    void search_filtersByQueryCaseInsensitively() {
        dao.create(patientId, doctorId, "2030-01-15", "09:30", "Headache", null);
        dao.create(patient2Id, doctor2Id, "2030-01-16", "10:00", "Checkup", null);
        assertEquals(2, dao.search(null, null, null, 0).size());
        assertEquals(1, dao.search("alice", null, null, 0).size());
        assertEquals(1, dao.search("PAT-000002", null, null, 0).size());
        assertEquals(1, dao.search("khan", null, null, 0).size());
        assertEquals(1, dao.search("HEADACHE", null, null, 0).size());
        assertTrue(dao.search("zzz", null, null, 0).isEmpty());
    }

    @Test
    void search_filtersByStatus() {
        int a = dao.create(patientId, doctorId, "2030-01-15", "09:30", null, null);
        dao.create(patientId, doctorId, "2030-01-16", "10:00", null, null);
        dao.updateStatus(a, "CANCELLED");
        assertEquals(1, dao.search(null, "SCHEDULED", null, 0).size());
        assertEquals(1, dao.search(null, "CANCELLED", null, 0).size());
        assertEquals(0, dao.search(null, "COMPLETED", null, 0).size());
        assertEquals(2, dao.search(null, null, null, 0).size());
    }

    @Test
    void search_filtersByDate() {
        dao.create(patientId, doctorId, "2030-01-15", "09:30", null, null);
        dao.create(patientId, doctorId, "2030-01-16", "10:00", null, null);
        assertEquals(1, dao.search(null, null, LocalDate.of(2030, 1, 15), 0).size());
        assertEquals(2, dao.search(null, null, null, 0).size());
    }

    @Test
    void search_filtersByDoctor() {
        dao.create(patientId, doctorId, "2030-01-15", "09:30", null, null);
        dao.create(patientId, doctor2Id, "2030-01-15", "10:00", null, null);
        assertEquals(1, dao.search(null, null, null, doctorId).size());
        assertEquals(1, dao.search(null, null, null, doctor2Id).size());
        assertEquals(2, dao.search(null, null, null, 0).size());
    }

    @Test
    void hasConflict_detectsSameSlot() {
        dao.create(patientId, doctorId, "2030-01-15", "09:30", null, null);
        assertTrue(dao.hasConflict(doctorId, "2030-01-15", "09:30", 0));
        assertFalse(dao.hasConflict(doctorId, "2030-01-15", "10:00", 0));
        assertFalse(dao.hasConflict(doctor2Id, "2030-01-15", "09:30", 0)); // different doctor
    }

    @Test
    void hasConflict_cancelledDoesNotBlock() {
        int a = dao.create(patientId, doctorId, "2030-01-15", "09:30", null, null);
        dao.updateStatus(a, "CANCELLED");
        assertFalse(dao.hasConflict(doctorId, "2030-01-15", "09:30", 0));
    }

    @Test
    void hasConflict_excludesOwnId() {
        int a = dao.create(patientId, doctorId, "2030-01-15", "09:30", null, null);
        // Same slot as itself, but excluding id a → no conflict
        assertFalse(dao.hasConflict(doctorId, "2030-01-15", "09:30", a));
        // Different id still conflicts
        assertTrue(dao.hasConflict(doctorId, "2030-01-15", "09:30", 999));
    }

    @Test
    void count_reflectsInsertions() {
        assertEquals(0, dao.count());
        dao.create(patientId, doctorId, "2030-01-15", "09:30", null, null);
        dao.create(patientId, doctorId, "2030-01-16", "10:00", null, null);
        assertEquals(2, dao.count());
    }
}
