package com.hospital.service;

import com.hospital.config.DatabaseConnection;
import com.hospital.config.DatabaseInitializer;
import com.hospital.config.TestDatabaseFactory;
import com.hospital.dao.UserDao;
import com.hospital.dao.UserDaoImpl;
import com.hospital.exception.AuthenticationException;
import com.hospital.exception.DatabaseException;
import com.hospital.model.Role;
import com.hospital.model.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for authentication: valid / invalid credentials, role retrieval,
 * duplicate-username handling, and demo-user seeding.
 */
class AuthServiceTest {

    private DatabaseConnection db;
    private UserDao userDao;
    private AuthService authService;

    @BeforeEach
    void setUp() throws IOException {
        Session.reset();
        db = TestDatabaseFactory.createTempDatabase();
        new DatabaseInitializer(db).initialize();
        userDao = new UserDaoImpl(db);
        authService = new AuthService(userDao);
    }

    @AfterEach
    void tearDown() throws IOException {
        Session.reset();
        TestDatabaseFactory.deleteDatabaseFile(db);
    }

    // ---------- Seeding ----------

    @Test
    void demoUsersAreSeededWhenTableIsEmpty() {
        assertEquals(0, userDao.count());
        authService.seedDemoUsersIfEmpty();
        assertEquals(3, userDao.count());

        User admin = userDao.findByUsername("admin").orElseThrow();
        User doctor = userDao.findByUsername("doctor").orElseThrow();
        User receptionist = userDao.findByUsername("receptionist").orElseThrow();

        assertEquals(Role.ADMIN, admin.getRole());
        assertEquals(Role.DOCTOR, doctor.getRole());
        assertEquals(Role.RECEPTIONIST, receptionist.getRole());
    }

    @Test
    void seedDemoUsersIsIdempotent() {
        authService.seedDemoUsersIfEmpty();
        authService.seedDemoUsersIfEmpty();
        assertEquals(3, userDao.count());
    }

    // ---------- Authentication ----------

    @Test
    void seededAdminCredentialsAuthenticate() {
        authService.seedDemoUsersIfEmpty();
        User u = authService.login("admin", "admin123");
        assertNotNull(u);
        assertEquals("admin", u.getUsername());
        assertEquals(Role.ADMIN, u.getRole());
        assertTrue(Session.getInstance().isLoggedIn());
    }

    @Test
    void seededDoctorCredentialsAuthenticate() {
        authService.seedDemoUsersIfEmpty();
        User u = authService.login("doctor", "doctor123");
        assertNotNull(u);
        assertEquals("doctor", u.getUsername());
        assertEquals(Role.DOCTOR, u.getRole());
        assertTrue(Session.getInstance().isLoggedIn());
    }

    @Test
    void seededReceptionistCredentialsAuthenticate() {
        authService.seedDemoUsersIfEmpty();
        User u = authService.login("receptionist", "receptionist123");
        assertNotNull(u);
        assertEquals("receptionist", u.getUsername());
        assertEquals(Role.RECEPTIONIST, u.getRole());
        assertTrue(Session.getInstance().isLoggedIn());
    }

    @Test
    void invalidPasswordIsRejected() {
        authService.seedDemoUsersIfEmpty();
        assertThrows(AuthenticationException.class,
                () -> authService.login("admin", "wrongpass"));
        assertFalse(Session.getInstance().isLoggedIn());
    }

    @Test
    void unknownUsernameIsRejected() {
        authService.seedDemoUsersIfEmpty();
        assertThrows(AuthenticationException.class,
                () -> authService.login("ghost", "admin123"));
    }

    @Test
    void blankUsernameOrPasswordIsRejected() {
        authService.seedDemoUsersIfEmpty();
        assertThrows(AuthenticationException.class, () -> authService.login("", "admin123"));
        assertThrows(AuthenticationException.class, () -> authService.login("admin", ""));
        assertThrows(AuthenticationException.class, () -> authService.login(null, "admin123"));
        assertThrows(AuthenticationException.class, () -> authService.login("admin", null));
    }

    @Test
    void userRolesReturnedCorrectly() {
        authService.seedDemoUsersIfEmpty();
        assertEquals(Role.ADMIN, authService.login("admin", "admin123").getRole());
        authService.logout();
        assertEquals(Role.DOCTOR, authService.login("doctor", "doctor123").getRole());
        authService.logout();
        assertEquals(Role.RECEPTIONIST, authService.login("receptionist", "receptionist123").getRole());
    }

    @Test
    void logoutClearsSession() {
        authService.seedDemoUsersIfEmpty();
        authService.login("admin", "admin123");
        assertTrue(Session.getInstance().isLoggedIn());
        authService.logout();
        assertFalse(Session.getInstance().isLoggedIn());
        assertNull(Session.getInstance().getCurrentUser());
    }

    // ---------- Duplicate username ----------

    @Test
    void inactiveUserCannotLogIn() {
        authService.seedDemoUsersIfEmpty();
        User doc = userDao.findByUsername("doctor").orElseThrow();
        userDao.updateActiveStatus(doc.getId(), false);
        assertThrows(AuthenticationException.class,
                () -> authService.login("doctor", "doctor123"));
        assertFalse(Session.getInstance().isLoggedIn());
    }

    @Test
    void inactiveErrorMessageIsSpecific() {
        authService.seedDemoUsersIfEmpty();
        User doc = userDao.findByUsername("doctor").orElseThrow();
        userDao.updateActiveStatus(doc.getId(), false);
        AuthenticationException ex = assertThrows(AuthenticationException.class,
                () -> authService.login("doctor", "doctor123"));
        assertTrue(ex.getMessage().toLowerCase().contains("inactive"));
    }

    @Test
    void activeUserCanLogIn() {
        authService.seedDemoUsersIfEmpty();
        // receptionist is active by default
        User u = authService.login("receptionist", "receptionist123");
        assertTrue(u.isActive());
    }

    @Test
    void duplicateUsernamesAreHandled() {
        authService.registerUser("jdoe", "pass1", Role.DOCTOR);
        assertThrows(DatabaseException.class,
                () -> authService.registerUser("jdoe", "pass2", Role.RECEPTIONIST));
    }

    @Test
    void passwordsAreNotStoredInPlainText() {
        authService.seedDemoUsersIfEmpty();
        User admin = userDao.findByUsername("admin").orElseThrow();
        assertNotEquals("admin123", admin.getPasswordHash());
        assertTrue(admin.getPasswordHash().contains(":"));
    }
}
