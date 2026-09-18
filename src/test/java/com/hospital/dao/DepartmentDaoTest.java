package com.hospital.dao;

import com.hospital.config.DatabaseConnection;
import com.hospital.config.DatabaseInitializer;
import com.hospital.config.TestDatabaseFactory;
import com.hospital.exception.DatabaseException;
import com.hospital.model.Department;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DepartmentDaoTest {

    private DatabaseConnection db;
    private DepartmentDao dao;

    @BeforeEach
    void setUp() throws IOException {
        db = TestDatabaseFactory.createTempDatabase();
        new DatabaseInitializer(db).initialize();
        dao = new DepartmentDaoImpl(db);
    }

    @AfterEach
    void tearDown() throws IOException {
        TestDatabaseFactory.deleteDatabaseFile(db);
    }

    @Test
    void createDepartment_andRetrieveById() {
        int id = dao.create("Cardiology", "Heart-related care");
        assertTrue(id > 0);
        Department d = dao.findById(id).orElseThrow();
        assertEquals("Cardiology", d.getName());
        assertEquals("Heart-related care", d.getDescription());
        assertEquals(id, d.getId());
    }

    @Test
    void createDepartment_allowsNullDescription() {
        int id = dao.create("Cardiology", null);
        Department d = dao.findById(id).orElseThrow();
        assertEquals("Cardiology", d.getName());
        assertNull(d.getDescription());
    }

    @Test
    void findByName() {
        dao.create("Neurology", "Brain & nerves");
        Department d = dao.findByName("Neurology").orElseThrow();
        assertEquals("Neurology", d.getName());
        assertTrue(dao.findByName("Does Not Exist").isEmpty());
    }

    @Test
    void findAll_listsDepartmentsSortedByName() {
        dao.create("Pediatrics", null);
        dao.create("Cardiology", null);
        dao.create("Neurology", null);
        List<Department> all = dao.findAll();
        assertEquals(3, all.size());
        // NOCASE ordering: Cardiology, Neurology, Pediatrics.
        assertEquals("Cardiology", all.get(0).getName());
        assertEquals("Neurology", all.get(1).getName());
        assertEquals("Pediatrics", all.get(2).getName());
    }

    @Test
    void updateDepartment_changesNameAndDescription() {
        int id = dao.create("Old Name", "old desc");
        dao.update(id, "New Name", "new desc");
        Department d = dao.findById(id).orElseThrow();
        assertEquals("New Name", d.getName());
        assertEquals("new desc", d.getDescription());
    }

    @Test
    void updateDepartment_canSetDescriptionToNull() {
        int id = dao.create("Surgery", "surgical care");
        dao.update(id, "Surgery", null);
        Department d = dao.findById(id).orElseThrow();
        assertNull(d.getDescription());
    }

    @Test
    void updateDepartment_unknownIdThrowsDatabaseException() {
        assertThrows(DatabaseException.class, () -> dao.update(9999, "X", "y"));
    }

    @Test
    void deleteDepartment_removesRow() {
        int id = dao.create("Pediatrics", null);
        assertEquals(1, dao.count());
        dao.delete(id);
        assertEquals(0, dao.count());
        assertTrue(dao.findById(id).isEmpty());
    }

    @Test
    void deleteDepartment_isNoOpWhenIdMissing() {
        // Should not throw.
        dao.delete(9999);
        assertEquals(0, dao.count());
    }

    @Test
    void duplicateNameThrowsDatabaseException() {
        dao.create("Cardiology", null);
        assertThrows(DatabaseException.class, () -> dao.create("Cardiology", "another"));
    }

    @Test
    void existsByNameIgnoreCase_detectsCaseVariants() {
        dao.create("Cardiology", null);
        assertTrue(dao.existsByNameIgnoreCase("cardiology", -1));
        assertTrue(dao.existsByNameIgnoreCase("CARDIOLOGY", -1));
        assertTrue(dao.existsByNameIgnoreCase("Cardiology", -1));
        assertFalse(dao.existsByNameIgnoreCase("Neurology", -1));
    }

    @Test
    void existsByNameIgnoreCase_excludesGivenId() {
        int id = dao.create("Cardiology", null);
        dao.create("Neurology", null);
        // Same name, same id -> excluded, so not considered a duplicate.
        assertFalse(dao.existsByNameIgnoreCase("Cardiology", id));
        // Different case, different id -> duplicate.
        assertTrue(dao.existsByNameIgnoreCase("cardiology", 9999));
    }
}
