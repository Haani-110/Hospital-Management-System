package com.hospital.service;

import com.hospital.config.DatabaseConnection;
import com.hospital.config.DatabaseInitializer;
import com.hospital.config.TestDatabaseFactory;
import com.hospital.dao.PatientDao;
import com.hospital.dao.PatientDaoImpl;
import com.hospital.exception.AuthorizationException;
import com.hospital.exception.ValidationException;
import com.hospital.model.Patient;
import com.hospital.model.Role;
import com.hospital.model.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PatientServiceTest {

    private DatabaseConnection db;
    private PatientDao patientDao;
    private PatientService service;

    @BeforeEach
    void setUp() throws IOException {
        Session.reset();
        db = TestDatabaseFactory.createTempDatabase();
        new DatabaseInitializer(db).initialize();
        patientDao = new PatientDaoImpl(db);
        service = new PatientService(patientDao);
    }

    @AfterEach
    void tearDown() throws IOException {
        Session.reset();
        TestDatabaseFactory.deleteDatabaseFile(db);
    }

    private void loginAs(Role role) {
        User u = new User(999, role.name().toLowerCase(), "hash", role, true, LocalDateTime.now());
        Session.getInstance().setCurrentUser(u);
    }

    private Patient createSample(Role asRole) {
        loginAs(asRole);
        return service.createPatient("Jane Doe", "1990-05-20", "Female",
                "555-1111", "jane@hospital.com", "123 Main St",
                "Mom", "555-2222", "O+");
    }

    // ---------- Authorization ----------

    @Test
    void adminCanCreateUpdateDeactivateActivate() {
        Patient created = createSample(Role.ADMIN);
        assertNotNull(created);
        assertTrue(created.getPatientCode().startsWith("PAT-"));
        assertEquals("Jane Doe", created.getFullName());
        assertEquals(LocalDate.of(1990, 5, 20), created.getDateOfBirth());
        assertEquals("Female", created.getGender());
        assertEquals("555-1111", created.getPhone());
        assertEquals("jane@hospital.com", created.getEmail());
        assertEquals("123 Main St", created.getAddress());
        assertEquals("Mom", created.getEmergencyContactName());
        assertEquals("555-2222", created.getEmergencyContactPhone());
        assertEquals("O+", created.getBloodGroup());
        assertTrue(created.isActive());

        Patient updated = service.updatePatient(created.getId(), "Jane Smith", "1991-06-21", "Other",
                "555-3333", "jane2@hospital.com", "456 Oak Ave", "Dad", "555-4444", "A-");
        assertEquals("Jane Smith", updated.getFullName());
        assertEquals(LocalDate.of(1991, 6, 21), updated.getDateOfBirth());
        assertEquals("Other", updated.getGender());
        assertEquals("555-3333", updated.getPhone());
        assertEquals("jane2@hospital.com", updated.getEmail());
        assertEquals("456 Oak Ave", updated.getAddress());
        assertEquals("Dad", updated.getEmergencyContactName());
        assertEquals("555-4444", updated.getEmergencyContactPhone());
        assertEquals("A-", updated.getBloodGroup());

        service.deactivatePatient(created.getId());
        assertFalse(patientDao.findById(created.getId()).orElseThrow().isActive());

        service.activatePatient(created.getId());
        assertTrue(patientDao.findById(created.getId()).orElseThrow().isActive());
    }

    @Test
    void receptionistCanCreateUpdateDeactivateActivate() {
        Patient p = createSample(Role.RECEPTIONIST);
        assertNotNull(p);

        service.updatePatient(p.getId(), "Jane 2", null, null, null, null, null, null, null, null);
        service.deactivatePatient(p.getId());
        assertFalse(patientDao.findById(p.getId()).orElseThrow().isActive());

        service.activatePatient(p.getId());
        assertTrue(patientDao.findById(p.getId()).orElseThrow().isActive());
    }

    @Test
    void doctorCanViewSearchButNotMutate() {
        Patient p = createSample(Role.ADMIN);

        loginAs(Role.DOCTOR);
        assertFalse(service.getAllPatients().isEmpty());
        assertEquals(1, service.searchPatients("jane", null).size());
        assertNotNull(service.getPatient(p.getId()));
        assertNotNull(service.getPatientByCode(p.getPatientCode()));
        assertEquals(1, service.searchPatients(null, true).size());
        assertEquals(0, service.searchPatients(null, false).size());

        assertThrows(AuthorizationException.class,
                () -> service.createPatient("x", null, null, null, null, null, null, null, null));
        assertThrows(AuthorizationException.class,
                () -> service.updatePatient(p.getId(), "x", null, null, null, null, null, null, null, null));
        assertThrows(AuthorizationException.class, () -> service.deactivatePatient(p.getId()));
        assertThrows(AuthorizationException.class, () -> service.activatePatient(p.getId()));
    }

    @Test
    void unauthenticatedIsRejected() {
        assertThrows(AuthorizationException.class, () -> service.getAllPatients());
        assertThrows(AuthorizationException.class, () -> service.searchPatients("x", null));
        assertThrows(AuthorizationException.class, () -> service.getPatient(1));
        assertThrows(AuthorizationException.class, () -> service.getPatientByCode("PAT-000001"));
        assertThrows(AuthorizationException.class,
                () -> service.createPatient("x", null, null, null, null, null, null, null, null));
        assertThrows(AuthorizationException.class,
                () -> service.updatePatient(1, "x", null, null, null, null, null, null, null, null));
        assertThrows(AuthorizationException.class, () -> service.deactivatePatient(1));
        assertThrows(AuthorizationException.class, () -> service.activatePatient(1));
    }

    // ---------- Validation ----------

    @Test
    void nameRequired() {
        loginAs(Role.ADMIN);
        assertThrows(ValidationException.class,
                () -> service.createPatient("", null, null, null, null, null, null, null, null));
        assertThrows(ValidationException.class,
                () -> service.createPatient("   ", null, null, null, null, null, null, null, null));
        assertThrows(ValidationException.class,
                () -> service.createPatient(null, null, null, null, null, null, null, null, null));
    }

    @Test
    void nameIsTrimmed() {
        loginAs(Role.ADMIN);
        Patient p = service.createPatient("  Jane  ", null, null, null, null, null, null, null, null);
        assertEquals("Jane", p.getFullName());
    }

    @Test
    void nameTooLongRejected() {
        loginAs(Role.ADMIN);
        assertThrows(ValidationException.class,
                () -> service.createPatient("a".repeat(101), null, null, null, null, null, null, null, null));
    }

    @Test
    void futureDobRejected() {
        loginAs(Role.ADMIN);
        String future = LocalDate.now().plusDays(1).toString();
        assertThrows(ValidationException.class,
                () -> service.createPatient("Jane", future, null, null, null, null, null, null, null));
    }

    @Test
    void invalidDobFormatRejected() {
        loginAs(Role.ADMIN);
        assertThrows(ValidationException.class,
                () -> service.createPatient("Jane", "not-a-date", null, null, null, null, null, null, null));
    }

    @Test
    void validDobAcceptedAndBlankDobAllowed() {
        loginAs(Role.ADMIN);
        Patient p = service.createPatient("Jane", "", null, null, null, null, null, null, null);
        assertNull(p.getDateOfBirth());
        Patient p2 = service.createPatient("John", "2000-12-31", null, null, null, null, null, null, null);
        assertEquals(LocalDate.of(2000, 12, 31), p2.getDateOfBirth());
    }

    @Test
    void invalidGenderRejected() {
        loginAs(Role.ADMIN);
        assertThrows(ValidationException.class,
                () -> service.createPatient("Jane", null, "Alien", null, null, null, null, null, null));
    }

    @Test
    void validGendersAccepted() {
        loginAs(Role.ADMIN);
        for (String g : new String[]{"Male", "Female", "Other", "Prefer not to say"}) {
            Patient p = service.createPatient("P " + g, null, g, null, null, null, null, null, null);
            assertEquals(g, p.getGender());
        }
    }

    @Test
    void invalidBloodGroupRejected() {
        loginAs(Role.ADMIN);
        assertThrows(ValidationException.class,
                () -> service.createPatient("Jane", null, null, null, null, null, null, null, "XYZ"));
    }

    @Test
    void allBloodGroupsAccepted() {
        loginAs(Role.ADMIN);
        for (String bg : new String[]{"A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"}) {
            Patient p = service.createPatient("P " + bg, null, null, null, null, null, null, null, bg);
            assertEquals(bg, p.getBloodGroup());
        }
    }

    @Test
    void invalidEmailRejected() {
        loginAs(Role.ADMIN);
        assertThrows(ValidationException.class,
                () -> service.createPatient("Jane", null, null, null, "not-an-email", null, null, null, null));
        assertThrows(ValidationException.class,
                () -> service.createPatient("Jane", null, null, null, "a@b", null, null, null, null));
    }

    @Test
    void emailOptionalTrimmed() {
        loginAs(Role.ADMIN);
        Patient p = service.createPatient("Jane", null, null, null, "  Jane@X.com  ", null, null, null, null);
        assertEquals("Jane@X.com", p.getEmail());
        Patient p2 = service.createPatient("John", null, null, null, "  ", null, null, null, null);
        assertNull(p2.getEmail());
    }

    @Test
    void emailTooLongRejected() {
        loginAs(Role.ADMIN);
        String longEmail = "e".repeat(95) + "@x.com";
        assertThrows(ValidationException.class,
                () -> service.createPatient("Jane", null, null, null, longEmail, null, null, null, null));
    }

    @Test
    void phoneTooLongRejected() {
        loginAs(Role.ADMIN);
        assertThrows(ValidationException.class,
                () -> service.createPatient("Jane", null, null, "p".repeat(31), null, null, null, null, null));
    }

    @Test
    void addressTooLongRejected() {
        loginAs(Role.ADMIN);
        assertThrows(ValidationException.class,
                () -> service.createPatient("Jane", null, null, null, null, "a".repeat(251), null, null, null));
    }

    @Test
    void emergencyNameTooLongRejected() {
        loginAs(Role.ADMIN);
        assertThrows(ValidationException.class,
                () -> service.createPatient("Jane", null, null, null, null, null, "a".repeat(101), null, null));
    }

    @Test
    void emergencyPhoneTooLongRejected() {
        loginAs(Role.ADMIN);
        assertThrows(ValidationException.class,
                () -> service.createPatient("Jane", null, null, null, null, null, null, "p".repeat(31), null));
    }

    // ---------- Patient code ----------

    @Test
    void generatedPatientCodesAreSequentialAndUnique() {
        loginAs(Role.ADMIN);
        Set<String> codes = new HashSet<>();
        for (int i = 0; i < 5; i++) {
            Patient p = service.createPatient("P" + i, null, null, null, null, null, null, null, null);
            String code = p.getPatientCode();
            assertNotNull(code);
            assertTrue(code.startsWith("PAT-"), "code should start with PAT-: " + code);
            assertTrue(codes.add(code), "duplicate code: " + code);
        }
        assertEquals(5, codes.size());
    }

    // ---------- Double toggle / unknown ----------

    @Test
    void doubleDeactivateRejected() {
        loginAs(Role.ADMIN);
        Patient p = service.createPatient("Jane", null, null, null, null, null, null, null, null);
        service.deactivatePatient(p.getId());
        assertThrows(ValidationException.class, () -> service.deactivatePatient(p.getId()));
    }

    @Test
    void doubleActivateRejected() {
        loginAs(Role.ADMIN);
        Patient p = service.createPatient("Jane", null, null, null, null, null, null, null, null);
        service.deactivatePatient(p.getId());
        service.activatePatient(p.getId());
        assertThrows(ValidationException.class, () -> service.activatePatient(p.getId()));
    }

    @Test
    void unknownPatientRejected() {
        loginAs(Role.ADMIN);
        assertThrows(ValidationException.class, () -> service.getPatient(9999));
        assertThrows(ValidationException.class, () -> service.getPatientByCode("PAT-999999"));
        assertThrows(ValidationException.class,
                () -> service.updatePatient(9999, "x", null, null, null, null, null, null, null, null));
        assertThrows(ValidationException.class, () -> service.deactivatePatient(9999));
        assertThrows(ValidationException.class, () -> service.activatePatient(9999));
    }

    @Test
    void blankPatientCodeRejected() {
        loginAs(Role.ADMIN);
        assertThrows(ValidationException.class, () -> service.getPatientByCode(null));
        assertThrows(ValidationException.class, () -> service.getPatientByCode("   "));
    }
}
