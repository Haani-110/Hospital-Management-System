package com.hospital.dao;

import com.hospital.config.DatabaseConnection;
import com.hospital.config.DatabaseInitializer;
import com.hospital.config.TestDatabaseFactory;
import com.hospital.exception.DatabaseException;
import com.hospital.model.Patient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PatientDaoTest {

    private DatabaseConnection db;
    private PatientDao dao;

    @BeforeEach
    void setUp() throws IOException {
        db = TestDatabaseFactory.createTempDatabase();
        new DatabaseInitializer(db).initialize();
        dao = new PatientDaoImpl(db);
    }

    @AfterEach
    void tearDown() throws IOException {
        TestDatabaseFactory.deleteDatabaseFile(db);
    }

    @Test
    void create_defaultsToActive() {
        int id = dao.create("PAT-000001", "Jane Doe", "1990-05-20", "Female",
                "555-1111", "j@x.com", "123 Main St", "Mom", "555-2222", "O+");
        assertTrue(id > 0);
        Patient p = dao.findById(id).orElseThrow();
        assertEquals("PAT-000001", p.getPatientCode());
        assertEquals("Jane Doe", p.getFullName());
        assertEquals(LocalDate.of(1990, 5, 20), p.getDateOfBirth());
        assertEquals("Female", p.getGender());
        assertEquals("555-1111", p.getPhone());
        assertEquals("j@x.com", p.getEmail());
        assertEquals("123 Main St", p.getAddress());
        assertEquals("Mom", p.getEmergencyContactName());
        assertEquals("555-2222", p.getEmergencyContactPhone());
        assertEquals("O+", p.getBloodGroup());
        assertTrue(p.isActive());
        assertNotNull(p.getCreatedAt());
        assertNotNull(p.getUpdatedAt());
    }

    @Test
    void create_allowsNullOptionalFields() {
        int id = dao.create("PAT-000001", "Jane Doe", null, null, null, null, null, null, null, null);
        Patient p = dao.findById(id).orElseThrow();
        assertNull(p.getDateOfBirth());
        assertNull(p.getGender());
        assertNull(p.getPhone());
        assertNull(p.getEmail());
        assertNull(p.getAddress());
        assertNull(p.getEmergencyContactName());
        assertNull(p.getEmergencyContactPhone());
        assertNull(p.getBloodGroup());
    }

    @Test
    void findByPatientCode_findsAndMisses() {
        dao.create("PAT-000001", "Jane", null, null, null, null, null, null, null, null);
        assertTrue(dao.findByPatientCode("PAT-000001").isPresent());
        assertTrue(dao.findByPatientCode("PAT-999999").isEmpty());
    }

    @Test
    void findById_unknownReturnsEmpty() {
        assertTrue(dao.findById(9999).isEmpty());
    }

    @Test
    void findAll_ordersByNameCaseInsensitive() {
        dao.create("PAT-000003", "Zoe", null, null, null, null, null, null, null, null);
        dao.create("PAT-000001", "alice", null, null, null, null, null, null, null, null);
        dao.create("PAT-000002", "Bob", null, null, null, null, null, null, null, null);
        List<Patient> all = dao.findAll();
        assertEquals(3, all.size());
        assertEquals("alice", all.get(0).getFullName());
        assertEquals("Bob", all.get(1).getFullName());
        assertEquals("Zoe", all.get(2).getFullName());
    }

    @Test
    void update_changesEditableFieldsButNotCodeOrActive() {
        int id = dao.create("PAT-000001", "Jane", "1990-01-01", "Female",
                "555", "j@x.com", "addr", "Mom", "555-2", "O+");
        dao.update(id, "Jane Updated", "1991-02-03", "Male",
                "555-9", "j2@x.com", "addr2", "Dad", "555-3", "A+");
        Patient p = dao.findById(id).orElseThrow();
        assertEquals("Jane Updated", p.getFullName());
        assertEquals(LocalDate.of(1991, 2, 3), p.getDateOfBirth());
        assertEquals("Male", p.getGender());
        assertEquals("555-9", p.getPhone());
        assertEquals("j2@x.com", p.getEmail());
        assertEquals("addr2", p.getAddress());
        assertEquals("Dad", p.getEmergencyContactName());
        assertEquals("555-3", p.getEmergencyContactPhone());
        assertEquals("A+", p.getBloodGroup());
        assertEquals("PAT-000001", p.getPatientCode());
        assertTrue(p.isActive());
    }

    @Test
    void update_unknownIdThrows() {
        assertThrows(DatabaseException.class,
                () -> dao.update(9999, "x", null, null, null, null, null, null, null, null));
    }

    @Test
    void updateActiveStatus_deactivatesAndReactivates() {
        int id = dao.create("PAT-000001", "Jane", null, null, null, null, null, null, null, null);
        assertTrue(dao.findById(id).orElseThrow().isActive());
        dao.updateActiveStatus(id, false);
        assertFalse(dao.findById(id).orElseThrow().isActive());
        dao.updateActiveStatus(id, true);
        assertTrue(dao.findById(id).orElseThrow().isActive());
    }

    @Test
    void updateActiveStatus_unknownThrows() {
        assertThrows(DatabaseException.class, () -> dao.updateActiveStatus(9999, false));
    }

    @Test
    void search_byCodeNamePhoneEmail_caseInsensitive() {
        dao.create("PAT-000001", "Alice Smith", null, null, "111-1111", "alice@h.com", null, null, null, null);
        dao.create("PAT-000002", "Bob Jones", null, null, "222-2222", "bob@h.com", null, null, null, null);
        dao.create("PAT-000003", "Carol", null, null, "333-3333", null, null, null, null, null);

        assertEquals(1, dao.search("PAT-000002", null).size());
        assertEquals(1, dao.search("pat-000002", null).size());
        assertEquals(1, dao.search("alice", null).size());
        assertEquals(1, dao.search("JONES", null).size());
        assertEquals(1, dao.search("222", null).size());
        assertEquals(1, dao.search("BOB@h.com", null).size());
        assertTrue(dao.search("zzz", null).isEmpty());
        assertEquals(3, dao.search(null, null).size());
        assertEquals(3, dao.search("  ", null).size());
    }

    @Test
    void search_filtersByActiveFlag() {
        dao.create("PAT-000001", "Alice", null, null, null, null, null, null, null, null);
        dao.create("PAT-000002", "Bob", null, null, null, null, null, null, null, null);
        dao.updateActiveStatus(1, false);

        List<Patient> active = dao.search(null, true);
        assertEquals(1, active.size());
        assertEquals("Bob", active.get(0).getFullName());

        List<Patient> inactive = dao.search(null, false);
        assertEquals(1, inactive.size());
        assertEquals("Alice", inactive.get(0).getFullName());

        assertEquals(2, dao.search(null, null).size());
    }

    @Test
    void existsByPatientCode_checksUniqueness() {
        dao.create("PAT-000001", "Alice", null, null, null, null, null, null, null, null);
        assertTrue(dao.existsByPatientCode("PAT-000001"));
        assertFalse(dao.existsByPatientCode("PAT-000002"));
    }

    @Test
    void existsById_checksExistence() {
        int id = dao.create("PAT-000001", "Alice", null, null, null, null, null, null, null, null);
        assertTrue(dao.existsById(id));
        assertFalse(dao.existsById(9999));
    }

    @Test
    void count_reflectsInsertions() {
        assertEquals(0, dao.count());
        dao.create("PAT-000001", "Alice", null, null, null, null, null, null, null, null);
        dao.create("PAT-000002", "Bob", null, null, null, null, null, null, null, null);
        assertEquals(2, dao.count());
        dao.updateActiveStatus(1, false);
        assertEquals(2, dao.count());
    }

    @Test
    void maxPatientCodeNumber_returnsHighestSuffixOrZero() {
        assertEquals(0, dao.maxPatientCodeNumber());
        dao.create("PAT-000005", "Alice", null, null, null, null, null, null, null, null);
        dao.create("PAT-000012", "Bob", null, null, null, null, null, null, null, null);
        assertEquals(12, dao.maxPatientCodeNumber());
    }
}
