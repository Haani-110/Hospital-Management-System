package com.hospital.service;

import com.hospital.dao.UserDao;
import com.hospital.exception.AuthenticationException;
import com.hospital.exception.DatabaseException;
import com.hospital.model.Role;
import com.hospital.model.User;
import com.hospital.util.PasswordUtil;

import java.util.Optional;

/**
 * Handles authentication and demo-user seeding.
 *
 * Seeding: on first launch (when the users table is empty) we create the
 * three demo accounts described in the specification. Passwords are stored
 * as PBKDF2 hashes, never in plain text.
 */
public class AuthService {

    private final UserDao userDao;

    public AuthService(UserDao userDao) {
        this.userDao = userDao;
    }

    /**
     * Attempt to log in with the given credentials.
     *
     * @return the authenticated User
     * @throws AuthenticationException when credentials are invalid
     */
    public User login(String username, String password) {
        if (username == null || username.isBlank()) {
            throw new AuthenticationException("Username is required");
        }
        if (password == null || password.isEmpty()) {
            throw new AuthenticationException("Password is required");
        }

        Optional<User> userOpt = userDao.findByUsername(username.trim());
        if (userOpt.isEmpty()) {
            throw new AuthenticationException("Invalid username or password");
        }
        User user = userOpt.get();
        if (!PasswordUtil.verify(password, user.getPasswordHash())) {
            throw new AuthenticationException("Invalid username or password");
        }
        if (!user.isActive()) {
            throw new AuthenticationException("This account is inactive. Please contact an administrator.");
        }

        Session.getInstance().setCurrentUser(user);
        return user;
    }

    public void logout() {
        Session.getInstance().clear();
    }

    /**
     * Seed the three demo users if and only if the users table is empty.
     * This makes the operation idempotent (it is safe to call on every startup).
     */
    public void seedDemoUsersIfEmpty() {
        if (userDao.count() > 0) {
            return;
        }
        createDemoUser("admin", "admin123", Role.ADMIN);
        createDemoUser("doctor", "doctor123", Role.DOCTOR);
        createDemoUser("receptionist", "receptionist123", Role.RECEPTIONIST);
    }

    private void createDemoUser(String username, String rawPassword, Role role) {
        String hash = PasswordUtil.hash(rawPassword);
        try {
            userDao.create(username, hash, role);
        } catch (DatabaseException e) {
            // Another thread/process concurrently created the same username.
            // That is fine; swallow since the seed user exists.
            if (e.getMessage() != null && e.getMessage().contains("Username already exists")) {
                return;
            }
            throw e;
        }
    }

    /**
     * Convenience method used by tests to create a user with a specific password.
     */
    public int registerUser(String username, String rawPassword, Role role) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username cannot be empty");
        }
        if (rawPassword == null || rawPassword.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be empty");
        }
        if (role == null) {
            throw new IllegalArgumentException("Role is required");
        }
        return userDao.create(username, PasswordUtil.hash(rawPassword), role);
    }
}
