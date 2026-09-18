package com.hospital.service;

import com.hospital.config.DatabaseConnection;
import com.hospital.config.DatabaseInitializer;
import com.hospital.config.TestDatabaseFactory;
import com.hospital.dao.DepartmentDao;
import com.hospital.dao.DepartmentDaoImpl;
import com.hospital.dao.DoctorDao;
import com.hospital.dao.DoctorDaoImpl;
import com.hospital.exception.AuthorizationException;
import com.hospital.exception.ValidationException;
import com.hospital.model.Doctor;
import com.hospital.model.Role;
import com.hospital.model.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class DoctorServiceTest {

    private DatabaseConnection db;
    private DoctorDao doctorDao;
    private DepartmentDao departmentDao;
    private DoctorService service;

    private int cardiologyId;
    private int neurologyId;

    @BeforeEach
    void setUp() throws IOException {
        Session.reset();
        db = TestDatabaseFactory.createTempDatabase();
        new DatabaseInitializer(db).initialize();
        doctorDao = new DoctorDaoImpl(db);
        departmentDao = new DepartmentDaoImpl(db);
        service = new DoctorService(doctorDao, departmentDao);

        cardiologyId = departmentDao.create("Cardiology", null);
        neurologyId = departmentDao.create("Neurology", null);
    }

    @AfterEach
    void tearDown() throws IOException {
        Session.reset();
        TestDatabaseFactory.deleteDatabaseFile(db);
    }

    private void loginAs(Role role) {
        // use non-colliding id 999 (mirrors UserServiceTest convention)
        User u = new User(999, role.name().toLowerCase(), "hash", role, true, LocalDateTime.now());
        Session.getInstance().setCurrentUser(u);
    }

    // ---------- Authorization ----------

    @Test
    void adminCanCreateUpdateDeactivateActivate() {
        loginAs(Role.ADMIN);
        Doctor created = service.createDoctor(null, cardiologyId, " Alice ", "Cardio", "555", "a@b.com", 150.0);
        assertNotNull(created);
        assertEquals("Alice", created.getFullName());
        assertEquals("Cardio", created.getSpecialization());
        assertEquals(150.0, created.getConsultationFee());
        assertTrue(created.isActive());
        assertEquals(cardiologyId, created.getDepartmentId());
        assertEquals("Cardiology", created.getDepartmentName());

        Doctor updated = service.updateDoctor(created.getId(), null, neurologyId, "Alice Updated",
                "Neuro", "555-2", "a2@b.com", 250.0);
        assertEquals("Alice Updated", updated.getFullName());
        assertEquals(neurologyId, updated.getDepartmentId());
        assertEquals(250.0, updated.getConsultationFee());

        service.deactivateDoctor(created.getId());
        assertFalse(doctorDao.findById(created.getId()).orElseThrow().isActive());

        service.activateDoctor(created.getId());
        assertTrue(doctorDao.findById(created.getId()).orElseThrow().isActive());
    }

    @Test
    void doctorCanViewAndSearchButNotMutate() {
        loginAs(Role.ADMIN);
        Doctor d = service.createDoctor(null, cardiologyId, "Alice", "Cardio", null, null, 0);

        loginAs(Role.DOCTOR);
        assertFalse(service.getAllDoctors().isEmpty());
        assertEquals(1, service.searchDoctors("ali", null).size());
        assertNotNull(service.getDoctor(d.getId()));

        assertThrows(AuthorizationException.class,
                () -> service.createDoctor(null, cardiologyId, "x", "y", null, null, 0));
        assertThrows(AuthorizationException.class,
                () -> service.updateDoctor(d.getId(), null, cardiologyId, "x", "y", null, null, 0));
        assertThrows(AuthorizationException.class, () -> service.deactivateDoctor(d.getId()));
        assertThrows(AuthorizationException.class, () -> service.activateDoctor(d.getId()));
    }

    @Test
    void receptionistCanViewButNotMutate() {
        loginAs(Role.ADMIN);
        Doctor d = service.createDoctor(null, cardiologyId, "Alice", "Cardio", null, null, 0);

        loginAs(Role.RECEPTIONIST);
        assertEquals(1, service.getAllDoctors().size());
        assertThrows(AuthorizationException.class,
                () -> service.createDoctor(null, cardiologyId, "x", "y", null, null, 0));
        assertThrows(AuthorizationException.class, () -> service.deactivateDoctor(d.getId()));
    }

    @Test
    void unauthenticatedIsRejected() {
        assertThrows(AuthorizationException.class, () -> service.getAllDoctors());
        assertThrows(AuthorizationException.class, () -> service.searchDoctors("ali", null));
        assertThrows(AuthorizationException.class, () -> service.getDoctor(1));
        assertThrows(AuthorizationException.class,
                () -> service.createDoctor(null, cardiologyId, "x", "y", null, null, 0));
        assertThrows(AuthorizationException.class,
                () -> service.updateDoctor(1, null, cardiologyId, "x", "y", null, null, 0));
        assertThrows(AuthorizationException.class, () -> service.deactivateDoctor(1));
        assertThrows(AuthorizationException.class, () -> service.activateDoctor(1));
    }

    // ---------- Validation ----------

    @Test
    void blankNameRejected() {
        loginAs(Role.ADMIN);
        assertThrows(ValidationException.class,
                () -> service.createDoctor(null, cardiologyId, "", "Cardio", null, null, 0));
        assertThrows(ValidationException.class,
                () -> service.createDoctor(null, cardiologyId, "   ", "Cardio", null, null, 0));
        assertThrows(ValidationException.class,
                () -> service.createDoctor(null, cardiologyId, null, "Cardio", null, null, 0));
    }

    @Test
    void blankSpecializationRejected() {
        loginAs(Role.ADMIN);
        assertThrows(ValidationException.class,
                () -> service.createDoctor(null, cardiologyId, "Alice", "", null, null, 0));
        assertThrows(ValidationException.class,
                () -> service.createDoctor(null, cardiologyId, "Alice", "  ", null, null, 0));
        assertThrows(ValidationException.class,
                () -> service.createDoctor(null, cardiologyId, "Alice", null, null, null, 0));
    }

    @Test
    void nameTooLongRejected() {
        loginAs(Role.ADMIN);
        String longName = "a".repeat(101);
        assertThrows(ValidationException.class,
                () -> service.createDoctor(null, cardiologyId, longName, "Cardio", null, null, 0));
    }

    @Test
    void specializationTooLongRejected() {
        loginAs(Role.ADMIN);
        String longSpec = "s".repeat(101);
        assertThrows(ValidationException.class,
                () -> service.createDoctor(null, cardiologyId, "Alice", longSpec, null, null, 0));
    }

    @Test
    void departmentRequired() {
        loginAs(Role.ADMIN);
        assertThrows(ValidationException.class,
                () -> service.createDoctor(null, null, "Alice", "Cardio", null, null, 0));
        assertThrows(ValidationException.class,
                () -> service.createDoctor(null, 0, "Alice", "Cardio", null, null, 0));
    }

    @Test
    void invalidDepartmentRejected() {
        loginAs(Role.ADMIN);
        assertThrows(ValidationException.class,
                () -> service.createDoctor(null, 9999, "Alice", "Cardio", null, null, 0));
    }

    @Test
    void phoneOptionalTrimmedButLimited() {
        loginAs(Role.ADMIN);
        Doctor d = service.createDoctor(null, cardiologyId, "Alice", "Cardio", "  ", null, 0);
        assertNull(d.getPhone());
        String longPhone = "p".repeat(31);
        assertThrows(ValidationException.class,
                () -> service.createDoctor(null, cardiologyId, "Bob", "Cardio", longPhone, null, 0));
    }

    @Test
    void emailOptionalButValidated() {
        loginAs(Role.ADMIN);
        Doctor d = service.createDoctor(null, cardiologyId, "Alice", "Cardio", null, "  ", 0);
        assertNull(d.getEmail());
        assertThrows(ValidationException.class,
                () -> service.createDoctor(null, cardiologyId, "Bob", "Cardio", null, "not-an-email", 0));
        assertThrows(ValidationException.class,
                () -> service.createDoctor(null, cardiologyId, "Bob", "Cardio", null, "a@b", 0));
        String longEmail = "e".repeat(95) + "@x.com"; // > 100 chars
        assertThrows(ValidationException.class,
                () -> service.createDoctor(null, cardiologyId, "Bob", "Cardio", null, longEmail, 0));
        Doctor ok = service.createDoctor(null, cardiologyId, "Bob", "Cardio", null, "  Bob@X.com  ", 0);
        assertEquals("Bob@X.com", ok.getEmail());
    }

    @Test
    void feeMustBeNonNegativeNumber() {
        loginAs(Role.ADMIN);
        assertThrows(ValidationException.class,
                () -> service.createDoctor(null, cardiologyId, "Alice", "Cardio", null, null, -1));
        assertThrows(ValidationException.class,
                () -> service.createDoctor(null, cardiologyId, "Alice", "Cardio", null, null, Double.NaN));
        assertThrows(ValidationException.class,
                () -> service.createDoctor(null, cardiologyId, "Alice", "Cardio", null, null, Double.POSITIVE_INFINITY));
        Doctor d = service.createDoctor(null, cardiologyId, "Alice", "Cardio", null, null, 0);
        assertEquals(0.0, d.getConsultationFee());
    }

    // ---------- Double toggle / missing ----------

    @Test
    void doubleDeactivateRejected() {
        loginAs(Role.ADMIN);
        Doctor d = service.createDoctor(null, cardiologyId, "Alice", "Cardio", null, null, 0);
        service.deactivateDoctor(d.getId());
        assertThrows(ValidationException.class, () -> service.deactivateDoctor(d.getId()));
    }

    @Test
    void doubleActivateRejected() {
        loginAs(Role.ADMIN);
        Doctor d = service.createDoctor(null, cardiologyId, "Alice", "Cardio", null, null, 0);
        service.deactivateDoctor(d.getId());
        service.activateDoctor(d.getId());
        assertThrows(ValidationException.class, () -> service.activateDoctor(d.getId()));
    }

    @Test
    void unknownDoctorRejected() {
        loginAs(Role.ADMIN);
        assertThrows(ValidationException.class, () -> service.getDoctor(9999));
        assertThrows(ValidationException.class,
                () -> service.updateDoctor(9999, null, cardiologyId, "x", "y", null, null, 0));
        assertThrows(ValidationException.class, () -> service.deactivateDoctor(9999));
        assertThrows(ValidationException.class, () -> service.activateDoctor(9999));
    }
}
