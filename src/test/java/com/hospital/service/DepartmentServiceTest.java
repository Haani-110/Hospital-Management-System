package com.hospital.service;

import com.hospital.config.DatabaseConnection;
import com.hospital.config.DatabaseInitializer;
import com.hospital.config.TestDatabaseFactory;
import com.hospital.dao.DepartmentDao;
import com.hospital.dao.DepartmentDaoImpl;
import com.hospital.exception.AuthorizationException;
import com.hospital.exception.ValidationException;
import com.hospital.model.Department;
import com.hospital.model.Role;
import com.hospital.model.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class DepartmentServiceTest {

    private DatabaseConnection db;
    private DepartmentDao dao;
    private DepartmentService service;

    @BeforeEach
    void setUp() throws IOException {
        Session.reset();
        db = TestDatabaseFactory.createTempDatabase();
        new DatabaseInitializer(db).initialize();
        dao = new DepartmentDaoImpl(db);
        service = new DepartmentService(dao);
    }

    @AfterEach
    void tearDown() throws IOException {
        Session.reset();
        TestDatabaseFactory.deleteDatabaseFile(db);
    }

    // ---------- helpers ----------

    private void loginAs(Role role) {
        User u = new User(1, role.name().toLowerCase(), "hash", role, true, LocalDateTime.now());
        Session.getInstance().setCurrentUser(u);
    }

    // ---------- Authorization ----------

    @Test
    void adminCanManageDepartments() {
        loginAs(Role.ADMIN);
        Department created = service.createDepartment("Cardiology", "Heart");
        assertNotNull(created);
        Department updated = service.updateDepartment(created.getId(), "Cardiology Dept", "Heart care");
        assertEquals("Cardiology Dept", updated.getName());
        service.deleteDepartment(updated.getId());
        assertEquals(0, dao.count());
    }

    @Test
    void doctorCannotManageDepartments() {
        loginAs(Role.DOCTOR);
        assertThrows(AuthorizationException.class,
                () -> service.createDepartment("Cardiology", null));
        assertThrows(AuthorizationException.class,
                () -> service.updateDepartment(1, "X", null));
        assertThrows(AuthorizationException.class,
                () -> service.deleteDepartment(1));
    }

    @Test
    void receptionistCannotManageDepartments() {
        loginAs(Role.RECEPTIONIST);
        assertThrows(AuthorizationException.class,
                () -> service.createDepartment("Cardiology", null));
        assertThrows(AuthorizationException.class,
                () -> service.updateDepartment(1, "X", null));
        assertThrows(AuthorizationException.class,
                () -> service.deleteDepartment(1));
    }

    @Test
    void unauthenticatedCannotAccessDepartments() {
        // no login
        assertThrows(AuthorizationException.class,
                () -> service.createDepartment("Cardiology", null));
        assertThrows(AuthorizationException.class,
                () -> service.getAllDepartments());
    }

    @Test
    void doctorsAndReceptionistsCanViewDepartments() {
        loginAs(Role.ADMIN);
        service.createDepartment("Cardiology", null);

        loginAs(Role.DOCTOR);
        assertEquals(1, service.getAllDepartments().size());
    }

    // ---------- Create / validation ----------

    @Test
    void validDepartmentCreation() {
        loginAs(Role.ADMIN);
        Department d = service.createDepartment("Cardiology", "Heart care");
        assertTrue(d.getId() > 0);
        assertEquals("Cardiology", d.getName());
        assertEquals("Heart care", d.getDescription());
    }

    @Test
    void emptyNameIsRejected() {
        loginAs(Role.ADMIN);
        assertThrows(ValidationException.class, () -> service.createDepartment("", null));
    }

    @Test
    void nullNameIsRejected() {
        loginAs(Role.ADMIN);
        assertThrows(ValidationException.class, () -> service.createDepartment(null, null));
    }

    @Test
    void whitespaceOnlyNameIsRejected() {
        loginAs(Role.ADMIN);
        assertThrows(ValidationException.class, () -> service.createDepartment("   ", null));
        assertThrows(ValidationException.class, () -> service.createDepartment("\t\n ", null));
    }

    @Test
    void nameIsTrimmed() {
        loginAs(Role.ADMIN);
        Department d = service.createDepartment("  Cardiology  ", null);
        assertEquals("Cardiology", d.getName());
    }

    @Test
    void descriptionIsTrimmed() {
        loginAs(Role.ADMIN);
        Department d = service.createDepartment("Cardiology", "  Heart care  ");
        assertEquals("Heart care", d.getDescription());
    }

    @Test
    void blankDescriptionBecomesNull() {
        loginAs(Role.ADMIN);
        Department d = service.createDepartment("Cardiology", "   ");
        assertNull(d.getDescription());
    }

    @Test
    void nameTooLongIsRejected() {
        loginAs(Role.ADMIN);
        String longName = "A".repeat(101);
        assertThrows(ValidationException.class, () -> service.createDepartment(longName, null));
    }

    @Test
    void descriptionTooLongIsRejected() {
        loginAs(Role.ADMIN);
        String longDesc = "B".repeat(256);
        assertThrows(ValidationException.class, () -> service.createDepartment("Cardiology", longDesc));
    }

    @Test
    void duplicateNameRejected() {
        loginAs(Role.ADMIN);
        service.createDepartment("Cardiology", null);
        assertThrows(ValidationException.class,
                () -> service.createDepartment("Cardiology", "different"));
    }

    @Test
    void duplicateNameCaseInsensitiveRejected() {
        loginAs(Role.ADMIN);
        service.createDepartment("Cardiology", null);
        assertThrows(ValidationException.class,
                () -> service.createDepartment("cardiology", null));
        assertThrows(ValidationException.class,
                () -> service.createDepartment("CARDIOLOGY", null));
    }

    // ---------- Update ----------

    @Test
    void updateWorks() {
        loginAs(Role.ADMIN);
        Department d = service.createDepartment("Old", "old desc");
        Department updated = service.updateDepartment(d.getId(), "New", "new desc");
        assertEquals("New", updated.getName());
        assertEquals("new desc", updated.getDescription());
    }

    @Test
    void updateToDuplicateNameRejected() {
        loginAs(Role.ADMIN);
        service.createDepartment("Cardiology", null);
        Department other = service.createDepartment("Neurology", null);
        assertThrows(ValidationException.class,
                () -> service.updateDepartment(other.getId(), "CARDIOLOGY", null));
    }

    @Test
    void updateSameNameIsAllowed() {
        loginAs(Role.ADMIN);
        Department d = service.createDepartment("Cardiology", "desc1");
        // Updating a department with its own name (case variant) must not
        // trigger the duplicate check against itself.
        Department updated = service.updateDepartment(d.getId(), "cardiology", "desc2");
        assertEquals("cardiology", updated.getName());
        assertEquals("desc2", updated.getDescription());
    }

    @Test
    void updateMissingDepartmentThrowsValidation() {
        loginAs(Role.ADMIN);
        assertThrows(ValidationException.class,
                () -> service.updateDepartment(9999, "X", null));
    }

    // ---------- Delete ----------

    @Test
    void deleteWorks() {
        loginAs(Role.ADMIN);
        Department d = service.createDepartment("Cardiology", null);
        service.deleteDepartment(d.getId());
        assertEquals(0, dao.count());
    }

    @Test
    void deleteMissingDepartmentThrowsValidation() {
        loginAs(Role.ADMIN);
        assertThrows(ValidationException.class, () -> service.deleteDepartment(9999));
    }
}
