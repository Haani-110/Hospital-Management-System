package com.hospital.dao;

import com.hospital.config.DatabaseConnection;
import com.hospital.config.DatabaseInitializer;
import com.hospital.config.TestDatabaseFactory;
import com.hospital.exception.DatabaseException;
import com.hospital.model.Role;
import com.hospital.model.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class UserDaoTest {

    private DatabaseConnection db;
    private UserDao dao;

    @BeforeEach
    void setUp() throws IOException {
        db = TestDatabaseFactory.createTempDatabase();
        new DatabaseInitializer(db).initialize();
        dao = new UserDaoImpl(db);
    }

    @AfterEach
    void tearDown() throws IOException {
        TestDatabaseFactory.deleteDatabaseFile(db);
    }

    @Test
    void createUser_defaultsActive() {
        int id = dao.create("alice", "hash", Role.DOCTOR);
        assertTrue(id > 0);
        User u = dao.findById(id).orElseThrow();
        assertEquals("alice", u.getUsername());
        assertEquals(Role.DOCTOR, u.getRole());
        assertEquals("hash", u.getPasswordHash());
        assertTrue(u.isActive());
        assertNotNull(u.getCreatedAt());
    }

    @Test
    void findByUsername() {
        dao.create("bob", "h", Role.ADMIN);
        assertTrue(dao.findByUsername("bob").isPresent());
        assertTrue(dao.findByUsername("nobody").isEmpty());
    }

    @Test
    void findAll_ordersByUsernameCaseInsensitive() {
        dao.create("Zack", "h", Role.DOCTOR);
        dao.create("alice", "h", Role.DOCTOR);
        dao.create("Bob", "h", Role.RECEPTIONIST);
        List<User> all = dao.findAll();
        assertEquals(3, all.size());
        assertEquals("alice", all.get(0).getUsername());
        assertEquals("Bob", all.get(1).getUsername());
        assertEquals("Zack", all.get(2).getUsername());
    }

    @Test
    void existsByUsernameIgnoreCase_handlesCaseAndExclude() {
        int id = dao.create("Admin", "h", Role.ADMIN);
        assertTrue(dao.existsByUsernameIgnoreCase("admin", -1));
        assertTrue(dao.existsByUsernameIgnoreCase("ADMIN", -1));
        assertFalse(dao.existsByUsernameIgnoreCase("admin", id)); // exclude self
        assertFalse(dao.existsByUsernameIgnoreCase("other", -1));
    }

    @Test
    void updateUser_changesUsernameAndRole() {
        int id = dao.create("alice", "h", Role.DOCTOR);
        dao.updateUser(id, "alice2", Role.RECEPTIONIST);
        User u = dao.findById(id).orElseThrow();
        assertEquals("alice2", u.getUsername());
        assertEquals(Role.RECEPTIONIST, u.getRole());
    }

    @Test
    void updateUser_unknownIdThrowsDatabaseException() {
        assertThrows(DatabaseException.class, () -> dao.updateUser(9999, "x", Role.ADMIN));
    }

    @Test
    void updatePassword_replacesHash() {
        int id = dao.create("alice", "oldhash", Role.DOCTOR);
        dao.updatePassword(id, "newhash");
        User u = dao.findById(id).orElseThrow();
        assertEquals("newhash", u.getPasswordHash());
    }

    @Test
    void updatePassword_unknownIdThrows() {
        assertThrows(DatabaseException.class, () -> dao.updatePassword(9999, "h"));
    }

    @Test
    void updateActiveStatus_flipsFlag() {
        int id = dao.create("alice", "h", Role.DOCTOR);
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
    void duplicateUsernameViolationThrowsDatabaseException() {
        dao.create("alice", "h", Role.DOCTOR);
        assertThrows(DatabaseException.class, () -> dao.create("alice", "h2", Role.RECEPTIONIST));
    }
}
