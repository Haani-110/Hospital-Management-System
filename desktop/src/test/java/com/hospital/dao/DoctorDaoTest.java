package com.hospital.dao;

import com.hospital.config.DatabaseConnection;
import com.hospital.config.DatabaseInitializer;
import com.hospital.config.TestDatabaseFactory;
import com.hospital.exception.DatabaseException;
import com.hospital.model.Doctor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DoctorDaoTest {

    private DatabaseConnection db;
    private DepartmentDao departmentDao;
    private DoctorDao dao;

    @BeforeEach
    void setUp() throws IOException {
        db = TestDatabaseFactory.createTempDatabase();
        new DatabaseInitializer(db).initialize();
        departmentDao = new DepartmentDaoImpl(db);
        dao = new DoctorDaoImpl(db);
    }

    @AfterEach
    void tearDown() throws IOException {
        TestDatabaseFactory.deleteDatabaseFile(db);
    }

    private int seedDepartment(String name) {
        return departmentDao.create(name, name + " description");
    }

    @Test
    void create_defaultsToActive() {
        int cardId = seedDepartment("Cardiology");
        int id = dao.create(null, cardId, "John Smith", "Cardiologist", "555-1234", "j@x.com", 200.0);
        assertTrue(id > 0);
        Doctor d = dao.findById(id).orElseThrow();
        assertEquals("John Smith", d.getFullName());
        assertEquals("Cardiologist", d.getSpecialization());
        assertEquals(cardId, d.getDepartmentId());
        assertEquals("Cardiology", d.getDepartmentName());
        assertEquals("555-1234", d.getPhone());
        assertEquals("j@x.com", d.getEmail());
        assertEquals(200.0, d.getConsultationFee());
        assertTrue(d.isActive());
        assertNull(d.getUserId());
        assertNotNull(d.getCreatedAt());
        assertNotNull(d.getUpdatedAt());
    }

    @Test
    void findById_unknownReturnsEmpty() {
        assertTrue(dao.findById(9999).isEmpty());
    }

    @Test
    void findAll_ordersByNameCaseInsensitive() {
        int cardId = seedDepartment("Cardiology");
        dao.create(null, cardId, "Zoe", "x", null, null, 0);
        dao.create(null, cardId, "alice", "y", null, null, 0);
        dao.create(null, cardId, "Bob", "z", null, null, 0);
        List<Doctor> all = dao.findAll();
        assertEquals(3, all.size());
        assertEquals("alice", all.get(0).getFullName());
        assertEquals("Bob", all.get(1).getFullName());
        assertEquals("Zoe", all.get(2).getFullName());
    }

    @Test
    void update_changesFields() {
        int cardId = seedDepartment("Cardiology");
        int neuroId = seedDepartment("Neurology");
        int id = dao.create(null, cardId, "John", "Cardio", null, null, 100);
        dao.update(id, null, neuroId, "John Updated", "Neuro", "555", "j@x.com", 300);
        Doctor d = dao.findById(id).orElseThrow();
        assertEquals("John Updated", d.getFullName());
        assertEquals("Neuro", d.getSpecialization());
        assertEquals(neuroId, d.getDepartmentId());
        assertEquals("Neurology", d.getDepartmentName());
        assertEquals("555", d.getPhone());
        assertEquals("j@x.com", d.getEmail());
        assertEquals(300.0, d.getConsultationFee());
    }

    @Test
    void update_unknownIdThrowsDatabaseException() {
        assertThrows(DatabaseException.class,
                () -> dao.update(9999, null, 1, "x", "y", null, null, 0));
    }

    @Test
    void updateActiveStatus_deactivatesAndReactivates() {
        int cardId = seedDepartment("Cardiology");
        int id = dao.create(null, cardId, "John", "Cardio", null, null, 0);
        assertTrue(dao.findById(id).orElseThrow().isActive());
        dao.updateActiveStatus(id, false);
        assertFalse(dao.findById(id).orElseThrow().isActive());
        dao.updateActiveStatus(id, true);
        assertTrue(dao.findById(id).orElseThrow().isActive());
    }

    @Test
    void updateActiveStatus_unknownIdThrows() {
        assertThrows(DatabaseException.class, () -> dao.updateActiveStatus(9999, false));
    }

    @Test
    void search_matchesByNameSpecPhoneEmail() {
        int cardId = seedDepartment("Cardiology");
        int neuroId = seedDepartment("Neurology");
        dao.create(null, cardId, "Alice Smith", "Cardiologist", "111-1111", "alice@hospital.com", 0);
        dao.create(null, neuroId, "Bob Jones", "Neurologist", "222-2222", "bob@hospital.com", 0);
        dao.create(null, cardId, "Carol", "Pediatric Cardiologist", "333-3333", null, 0);

        assertEquals(2, dao.search("cardi", 0).size()); // alice (Cardiologist) + carol (Pediatric Cardiologist)
        assertEquals(2, dao.search("card", cardId).size()); // alice + carol both in cardiology matching "card*"
        assertEquals(1, dao.search("bob", 0).size());
        assertEquals(1, dao.search("222", 0).size());
        assertEquals(1, dao.search("ALICE@", 0).size());
        assertTrue(dao.search("zzz", 0).isEmpty());
    }

    @Test
    void search_filtersByDepartment() {
        int cardId = seedDepartment("Cardiology");
        int neuroId = seedDepartment("Neurology");
        dao.create(null, cardId, "Alice", "Cardio", null, null, 0);
        dao.create(null, neuroId, "Bob", "Neuro", null, null, 0);
        dao.create(null, cardId, "Carol", "Cardio", null, null, 0);
        assertEquals(2, dao.search(null, cardId).size());
        assertEquals(1, dao.search(null, neuroId).size());
        assertEquals(3, dao.search(null, 0).size());
    }

    @Test
    void existsById_checksExistence() {
        int cardId = seedDepartment("Cardiology");
        int id = dao.create(null, cardId, "Alice", "Cardio", null, null, 0);
        assertTrue(dao.existsById(id));
        assertFalse(dao.existsById(9999));
    }

    @Test
    void count_reflectsInsertionsAndUpdates() {
        int cardId = seedDepartment("Cardiology");
        assertEquals(0, dao.count());
        dao.create(null, cardId, "Alice", "x", null, null, 0);
        dao.create(null, cardId, "Bob", "y", null, null, 0);
        assertEquals(2, dao.count());
        // deactivation should not remove row
        dao.updateActiveStatus(1, false);
        assertEquals(2, dao.count());
    }
}
