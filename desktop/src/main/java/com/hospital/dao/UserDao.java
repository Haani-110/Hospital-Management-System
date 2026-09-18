package com.hospital.dao;

import com.hospital.model.Role;
import com.hospital.model.User;

import java.util.List;
import java.util.Optional;

/**
 * Data access contract for the {@code users} table.
 * All methods use prepared statements; SQL lives here only.
 */
public interface UserDao {

    /** Persist a new user (active by default). Returns the generated id. */
    int create(String username, String passwordHash, Role role);

    /** Find a user by primary key. */
    Optional<User> findById(int id);

    /** Find a user by username (exact match). */
    Optional<User> findByUsername(String username);

    /** Return every user ordered by username (case-insensitive). */
    List<User> findAll();

    /** Update username + role for an existing user. */
    void updateUser(int id, String username, Role role);

    /** Update just the password hash. */
    void updatePassword(int id, String newPasswordHash);

    /** Update active flag. */
    void updateActiveStatus(int id, boolean active);

    /** @return number of rows in the users table. */
    int count();

    /**
     * Case-insensitive username existence check, optionally excluding a user id
     * (used during updates to avoid flagging the user against itself).
     */
    boolean existsByUsernameIgnoreCase(String username, int excludeId);
}
