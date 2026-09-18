package com.hospital.service;

import com.hospital.config.DatabaseConnection;
import com.hospital.config.DatabaseInitializer;
import com.hospital.config.TestDatabaseFactory;
import com.hospital.dao.UserDao;
import com.hospital.dao.UserDaoImpl;
import com.hospital.exception.AuthorizationException;
import com.hospital.exception.ValidationException;
import com.hospital.model.Role;
import com.hospital.model.User;
import com.hospital.util.PasswordUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class UserServiceTest {

    private DatabaseConnection db;
    private UserDao dao;
    private UserService service;

    @BeforeEach
    void setUp() throws IOException {
        Session.reset();
        db = TestDatabaseFactory.createTempDatabase();
        new DatabaseInitializer(db).initialize();
        dao = new UserDaoImpl(db);
        service = new UserService(dao);
    }

    @AfterEach
    void tearDown() throws IOException {
        Session.reset();
        TestDatabaseFactory.deleteDatabaseFile(db);
    }

    // helpers
    private void loginAs(Role role, int id) {
        User u = new User(id, role.name().toLowerCase(), "hash", role, true, LocalDateTime.now());
        Session.getInstance().setCurrentUser(u);
    }
    // Use a session ID (999) that cannot collide with auto-increment IDs of users
    // created by the tests (which start at 1). Tests that intentionally verify
    // self-protection use loginAs(role, 1) explicitly.
    private void loginAs(Role role) { loginAs(role, 999); }

    // ---------- Authorization ----------

    @Test
    void adminCanCreateUpdateChangePasswordDeactivateActivate() {
        loginAs(Role.ADMIN); // uses non-colliding session id 999
        User created = service.createUser("jdoe", "password1", "password1", Role.DOCTOR);
        assertNotNull(created);
        assertTrue(created.isActive());

        User updated = service.updateUser(created.getId(), "jdoe2", Role.RECEPTIONIST);
        assertEquals("jdoe2", updated.getUsername());
        assertEquals(Role.RECEPTIONIST, updated.getRole());

        service.changePassword(created.getId(), "newpass12", "newpass12");
        User fetched = dao.findById(created.getId()).orElseThrow();
        assertTrue(PasswordUtil.verify("newpass12", fetched.getPasswordHash()));

        service.deactivateUser(created.getId());
        assertFalse(dao.findById(created.getId()).orElseThrow().isActive());

        service.activateUser(created.getId());
        assertTrue(dao.findById(created.getId()).orElseThrow().isActive());
    }

    @Test
    void doctorCannotManageUsers() {
        loginAs(Role.DOCTOR);
        assertThrows(AuthorizationException.class,
                () -> service.createUser("x", "password1", "password1", Role.DOCTOR));
        assertThrows(AuthorizationException.class,
                () -> service.updateUser(1, "x", Role.DOCTOR));
        assertThrows(AuthorizationException.class, () -> service.changePassword(1, "password1", "password1"));
        assertThrows(AuthorizationException.class, () -> service.deactivateUser(1));
        assertThrows(AuthorizationException.class, () -> service.activateUser(1));
    }

    @Test
    void receptionistCannotManageUsers() {
        loginAs(Role.RECEPTIONIST);
        assertThrows(AuthorizationException.class,
                () -> service.createUser("x", "password1", "password1", Role.DOCTOR));
        assertThrows(AuthorizationException.class,
                () -> service.updateUser(1, "x", Role.DOCTOR));
    }

    @Test
    void unauthenticatedCannotManageUsers() {
        assertThrows(AuthorizationException.class, () -> service.getAllUsers());
        assertThrows(AuthorizationException.class,
                () -> service.createUser("x", "password1", "password1", Role.DOCTOR));
    }

    // ---------- Validation ----------

    @Test
    void blankUsernameRejected() {
        loginAs(Role.ADMIN);
        assertThrows(ValidationException.class,
                () -> service.createUser("", "password1", "password1", Role.DOCTOR));
        assertThrows(ValidationException.class,
                () -> service.createUser("   ", "password1", "password1", Role.DOCTOR));
        assertThrows(ValidationException.class,
                () -> service.createUser(null, "password1", "password1", Role.DOCTOR));
    }

    @Test
    void usernameIsTrimmed() {
        loginAs(Role.ADMIN);
        User u = service.createUser("  alice  ", "password1", "password1", Role.DOCTOR);
        assertEquals("alice", u.getUsername());
    }

    @Test
    void usernameTooLongRejected() {
        loginAs(Role.ADMIN);
        String longName = "a".repeat(51);
        assertThrows(ValidationException.class,
                () -> service.createUser(longName, "password1", "password1", Role.DOCTOR));
    }

    @Test
    void duplicateUsernameRejected() {
        loginAs(Role.ADMIN);
        service.createUser("Alice", "password1", "password1", Role.DOCTOR);
        assertThrows(ValidationException.class,
                () -> service.createUser("Alice", "password1", "password1", Role.RECEPTIONIST));
    }

    @Test
    void caseInsensitiveDuplicateRejected() {
        loginAs(Role.ADMIN);
        service.createUser("Alice", "password1", "password1", Role.DOCTOR);
        assertThrows(ValidationException.class,
                () -> service.createUser("alice", "password1", "password1", Role.RECEPTIONIST));
        assertThrows(ValidationException.class,
                () -> service.createUser("ALICE", "password1", "password1", Role.RECEPTIONIST));
    }

    @Test
    void shortPasswordRejected() {
        loginAs(Role.ADMIN);
        assertThrows(ValidationException.class,
                () -> service.createUser("bob", "short", "short", Role.DOCTOR));
    }

    @Test
    void passwordConfirmationMismatchRejected() {
        loginAs(Role.ADMIN);
        assertThrows(ValidationException.class,
                () -> service.createUser("bob", "password1", "different", Role.DOCTOR));
    }

    @Test
    void passwordIsStoredHashed() {
        loginAs(Role.ADMIN);
        User u = service.createUser("bob", "password1", "password1", Role.DOCTOR);
        assertNotEquals("password1", u.getPasswordHash());
        assertTrue(PasswordUtil.verify("password1", u.getPasswordHash()));
    }

    @Test
    void roleIsRequired() {
        loginAs(Role.ADMIN);
        assertThrows(ValidationException.class,
                () -> service.createUser("bob", "password1", "password1", null));
    }

    // ---------- Self-protection ----------

    @Test
    void currentAdminCannotDeactivateSelf() {
        // Admin has id=1 in the session.
        User admin = new User(1, "admin", "hash", Role.ADMIN, true, LocalDateTime.now());
        Session.getInstance().setCurrentUser(admin);
        dao.create("admin", "hash", Role.ADMIN); // create user id=1 in DB (id will autoincrement; use known id)
        // Easier: create admin user via dao at id 1? SQLite autoincrement starts at 1 so first insert is id=1.
        int id = dao.findByUsername("admin").orElseThrow().getId();
        assertEquals(1, id);
        assertThrows(ValidationException.class, () -> service.deactivateUser(id));
    }

    @Test
    void currentAdminCannotChangeOwnRole() {
        User admin = new User(1, "admin", "hash", Role.ADMIN, true, LocalDateTime.now());
        Session.getInstance().setCurrentUser(admin);
        dao.create("admin", "hash", Role.ADMIN);
        int id = dao.findByUsername("admin").orElseThrow().getId();
        assertThrows(ValidationException.class,
                () -> service.updateUser(id, "admin", Role.DOCTOR));
    }

    // ---------- Missing user ----------

    @Test
    void missingUserThrowsValidation() {
        loginAs(Role.ADMIN);
        assertThrows(ValidationException.class, () -> service.getUser(9999));
        assertThrows(ValidationException.class, () -> service.updateUser(9999, "x", Role.DOCTOR));
        assertThrows(ValidationException.class, () -> service.changePassword(9999, "password1", "password1"));
        assertThrows(ValidationException.class, () -> service.deactivateUser(9999));
        assertThrows(ValidationException.class, () -> service.activateUser(9999));
    }

    @Test
    void doubleDeactivateRejected() {
        loginAs(Role.ADMIN);
        User u = service.createUser("bob", "password1", "password1", Role.DOCTOR);
        service.deactivateUser(u.getId());
        assertThrows(ValidationException.class, () -> service.deactivateUser(u.getId()));
    }

    @Test
    void doubleActivateRejected() {
        loginAs(Role.ADMIN);
        User u = service.createUser("bob", "password1", "password1", Role.DOCTOR);
        service.deactivateUser(u.getId());
        service.activateUser(u.getId());
        assertThrows(ValidationException.class, () -> service.activateUser(u.getId()));
    }

    @Test
    void updateToDuplicateNameRejected() {
        loginAs(Role.ADMIN);
        User a = service.createUser("alice", "password1", "password1", Role.DOCTOR);
        service.createUser("bob", "password1", "password1", Role.DOCTOR);
        assertThrows(ValidationException.class,
                () -> service.updateUser(a.getId(), "bob", Role.DOCTOR));
        assertThrows(ValidationException.class,
                () -> service.updateUser(a.getId(), "BOB", Role.DOCTOR));
    }

    @Test
    void updateToSameNameIsAllowed() {
        loginAs(Role.ADMIN);
        User a = service.createUser("Alice", "password1", "password1", Role.DOCTOR);
        // changing name to same-but-different-case is allowed; treated as same user.
        User updated = service.updateUser(a.getId(), "alice", Role.DOCTOR);
        assertEquals("alice", updated.getUsername());
    }
}
